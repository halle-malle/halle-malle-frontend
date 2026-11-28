const {createHash} = require("node:crypto");
const {logger, setGlobalOptions} = require("firebase-functions");
const {onDocumentCreated, onDocumentUpdated} =
    require("firebase-functions/v2/firestore");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {initializeApp} = require("firebase-admin/app");
const {FieldValue, getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
setGlobalOptions({maxInstances: 10});

exports.notifyFriendRequest = onDocumentCreated(
    "users/{receiverUid}/friends/{senderUid}",
    async (event) => {
      if (!event.data || event.data.get("status") !== 0) {
        return;
      }

      await sendGroupMemberNotification(
          event.params.receiverUid, "친구 요청", "새 친구 요청이 도착했어요.",
          "friend_request", "", event.id,
      );
    },
);


exports.notifyGroupChat = onDocumentCreated(
    "groups/{groupId}/chats/{chatId}",
    async (event) => {
      if (!event.data) {
        return;
      }

      const senderId = event.data.get("senderId");
      const senderName = event.data.get("name");
      const content = event.data.get("content");
      const title = typeof senderName === "string" && senderName.trim() ?
        "모임 채팅 · " + [...senderName.trim()].slice(0, 40).join("") : "모임 채팅";
      let body = "새 채팅 메시지가 도착했어요.";
      if (typeof content === "string" && content.trim()) {
        const characters = [...content.trim()];
        body = characters.slice(0, 80).join("");
        if (characters.length > 80) body += "…";
      }
      const db = getFirestore();
      const group = await db.collection("groups")
          .doc(event.params.groupId).get();
      const memberIds = group.get("memberIds");
      if (!Array.isArray(memberIds) || !memberIds.includes(senderId)) {
        return;
      }

      await Promise.all([...new Set(memberIds)].map(async (uid) => {
        if (uid === senderId) {
          return;
        }

        await sendGroupMemberNotification(
            uid, title, body, "group_chat", event.params.groupId, event.id,
        );
      }));
    },
);

const groupMemberPath = "groups/{groupId}/members/{uid}";
const memberRole = 1; // ROLE_MEMBER in FirestoreData.kt
const waitingStatus = 1; // STATUS_WAITING
const joinedStatus = 0; // STATUS_JOINED

exports.notifyGroupMembership = onDocumentUpdated(
    "groups/{groupId}",
    async (event) => {
      if (!event.data) return;

      const {before, after} = event.data;
      const hostId = after.get("hostId");
      const previousMembers = before.get("memberIds");
      const currentMembers = after.get("memberIds");
      if (!hostId || !Array.isArray(previousMembers) ||
          !Array.isArray(currentMembers)) {
        return;
      }

      const previous = new Set(previousMembers);
      const current = new Set(currentMembers);
      const joined = [...current].filter((uid) =>
        uid !== hostId && !previous.has(uid),
      );
      const left = [...previous].filter((uid) =>
        uid !== hostId && !current.has(uid),
      );

      const groupId = event.params.groupId;
      if (joined.length > 0) {
        await sendGroupMemberNotification(
            hostId, "모임 참가", `새 참가자 ${joined.length}명이 들어왔어요.`,
            "group_membership", groupId, event.id + ":joined",
        );
      }
      if (left.length > 0) {
        await sendGroupMemberNotification(
            hostId, "모임 탈퇴", `참가자 ${left.length}명이 나갔어요.`,
            "group_membership", groupId, event.id + ":left",
        );
      }
    },
);

exports.notifyGroupVoteCancellation = onDocumentUpdated(
    "groups/{groupId}",
    async (event) => {
      if (!event.data) return;

      const {before, after} = event.data;
      const canceledStatus = 2; // STATUS_CANCELED in FirestoreData.kt
      if (before.get("status") === canceledStatus ||
          after.get("status") !== canceledStatus ||
          after.get("canceledByVote") !== true) {
        return;
      }

      const hostId = after.get("hostId");
      const memberIds = after.get("memberIds");
      if (!Array.isArray(memberIds)) return;

      const groupId = event.params.groupId;
      const recipients = [...new Set([hostId, ...memberIds])]
          .filter((uid) => uid);
      await Promise.all(recipients.map((uid) => sendGroupMemberNotification(
          uid, "모임 취소", "모임이 취소되었습니다.",
          "group_cancellation", groupId, event.id,
      )));
    },
);

exports.notifyGroupApproval = onDocumentUpdated(
    groupMemberPath,
    async (event) => {
      if (!event.data) {
        return;
      }

      const {before, after} = event.data;
      if (before.get("status") !== waitingStatus ||
          after.get("status") !== joinedStatus ||
          after.get("role") !== memberRole) {
        return;
      }

      const group = await after.ref.parent.parent.get();
      const memberIds = group.get("memberIds");
      if (!Array.isArray(memberIds) || !memberIds.includes(event.params.uid)) {
        return;
      }

      await sendGroupMemberNotification(
          event.params.uid, "모임 승인", "모임 신청이 승인됐어요.",
          "group_approval", event.params.groupId, event.id,
      );
    },
);

/**
 * Records a notification and sends it to the user's registered device.
 * @param {string} uid Recipient Firebase Auth UID.
 * @param {string} title Notification title.
 * @param {string} body Notification text.
 * @param {string} type Notification destination type.
 * @param {string} groupId Group document ID.
 * @param {string} eventId Stable source event key.
 */
async function sendGroupMemberNotification(
    uid, title, body, type, groupId, eventId,
) {
  const db = getFirestore();
  const id = createHash("sha256").update(type + ":" + eventId).digest("hex");
  const item = db.collection("notificationInboxes").doc(uid)
      .collection("items").doc(id);
  try {
    await item.create({
      title, body, type, groupId, createdAt: FieldValue.serverTimestamp(),
    });
  } catch (error) {
    if (error.code === 6) return; // Duplicate delivery of the same event.
    throw error;
  }

  const user = await db.collection("users").doc(uid).get();
  if (user.get("pushEnabled") === false) return;

  const token = user.get("fcmToken");
  if (typeof token !== "string" || token.length === 0) return;

  const message = {
    token,
    notification: {title, body},
    data: {type, groupId, recipientUid: uid},
  };
  if (type === "group_chat") {
    message.android = {
      notification: {tag: `group_chat:${uid}:${groupId}`},
    };
  }
  await getMessaging().send(message);
}

exports.listNotifications = onCall({region: "asia-northeast3"}, async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "로그인이 필요해요.");
  const cursor = req.data && req.data.cursor;
  const items = getFirestore().collection("notificationInboxes")
      .doc(req.auth.uid).collection("items");
  let query = items.orderBy("createdAt", "desc");
  if (cursor) {
    if (typeof cursor !== "string" || !/^[a-f0-9]{64}$/.test(cursor)) {
      throw new HttpsError("invalid-argument", "잘못된 페이지 요청이에요.");
    }
    const last = await items.doc(cursor).get();
    if (!last.exists) return {items: [], nextCursor: null};
    query = query.startAfter(last);
  }
  const page = await query.limit(51).get();
  const docs = page.docs.slice(0, 50);
  return {
    items: docs.map((doc) => {
      const {title, body, type, groupId, createdAt} = doc.data();
      return {title, body, type, groupId, createdAt: createdAt.toMillis()};
    }),
    nextCursor: page.size > 50 ? docs[49].id : null,
  };
});

exports.castGroupCancellationVote = onCall(
    {region: "asia-northeast3"},
    async (req) => {
      if (!req.auth) {
        throw new HttpsError("unauthenticated", "로그인이 필요해요.");
      }
      const {groupId, approve} = req.data || {};
      if (typeof groupId !== "string" || !groupId || groupId.includes("/") ||
          groupId === "." || groupId === ".." || typeof approve !== "boolean") {
        throw new HttpsError("invalid-argument", "잘못된 투표 요청이에요.");
      }
      const uid = req.auth.uid;
      const db = getFirestore();
      const groupRef = db.collection("groups").doc(groupId);
      const votes = groupRef.collection("cancelVotes");
      return db.runTransaction(async (transaction) => {
        const group = await transaction.get(groupRef);
        if (!group.exists) {
          throw new HttpsError("not-found", "모임이 사라졌어요.");
        }
        const ids = group.get("memberIds");
        const participants = new Set([group.get("hostId"),
          ...(Array.isArray(ids) ? ids : [])].filter((id) => id));
        if (!participants.has(uid)) {
          throw new HttpsError("permission-denied", "참가자만 투표할 수 있어요.");
        }
        const status = group.get("status");
        if ((status !== 0 && status !== 1) ||
            group.get("dateTime") <= Date.now()) {
          throw new HttpsError("failed-precondition", "투표가 종료된 모임이에요.");
        }

        const ballots = await transaction.get(votes);
        if (ballots.docs.some((doc) => doc.id === uid)) {
          throw new HttpsError("already-exists", "이미 투표했어요.");
        }
        const yes = ballots.docs.filter((doc) =>
          participants.has(doc.id) && doc.get("approve") === true,
        ).length + (approve ? 1 : 0);
        const required = Math.floor(participants.size / 2) + 1;
        const canceled = yes >= required;
        transaction.create(votes.doc(uid), {approve});
        // Serialize votes even when the group is not yet canceled.
        transaction.update(groupRef, canceled ? {
          status: 2, canceledByVote: true,
        } : {cancelVoteUpdatedAt: FieldValue.serverTimestamp()});
        return {yes, required, canceled};
      });
    },
);

exports.notifyGroupStart = onSchedule("every 1 minutes", async () => {
  const db = getFirestore();
  const now = Date.now();
  const groups = await db.collection("groups")
      .where("dateTime", ">", now - 2 * 60 * 1000)
      .where("dateTime", "<=", now)
      .get();

  for (const group of groups.docs) {
    const dateTime = group.get("dateTime");
    const recipients = await db.runTransaction(async (transaction) => {
      const current = await transaction.get(group.ref);
      const status = current.get("status");
      if (current.get("dateTime") !== dateTime ||
          (status !== 0 && status !== 1) ||
          current.get("startNotificationFor") === dateTime) {
        return [];
      }

      const memberIds = current.get("memberIds") || [];
      const hostId = current.get("hostId");
      // Reserve this start time before sending so overlapping runs skip it.
      transaction.update(group.ref, {startNotificationFor: dateTime});
      return [...new Set([hostId, ...memberIds])].filter((uid) => uid);
    });

    await Promise.all(recipients.map(async (uid) => {
      try {
        await sendGroupMemberNotification(
            uid, "모임 시작", "모임 시작 시간이 되었어요.",
            "group_start", group.id, `start:${group.id}:${dateTime}`,
        );
      } catch (error) {
        logger.error("Group start notification failed", {
          groupId: group.id,
          code: error.code,
        });
      }
    }));
  }
});
