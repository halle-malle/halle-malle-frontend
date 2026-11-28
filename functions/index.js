const {createHash} = require("node:crypto");
const {logger, setGlobalOptions} = require("firebase-functions");
const {onDocumentCreated, onDocumentUpdated} =
    require("firebase-functions/v2/firestore");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {initializeApp} = require("firebase-admin/app");
const {FieldValue, Timestamp, getFirestore} =
    require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
setGlobalOptions({maxInstances: 10});

const chatNotificationTypes = ["group_chat", "personal_chat"];

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
  const isChat = chatNotificationTypes.includes(type);
  const data = {type, groupId, recipientUid: uid};
  if (!isChat) {
    const id = createHash("sha256").update(type + ":" + eventId).digest("hex");
    const inbox = db.collection("notificationInboxes").doc(uid);
    const item = inbox.collection("items").doc(id);
    const batch = db.batch();
    batch.create(item, {
      title, body, type, groupId, createdAt: FieldValue.serverTimestamp(),
    });
    batch.set(inbox, {changedAt: FieldValue.serverTimestamp()}, {merge: true});
    try {
      await batch.commit();
    } catch (error) {
      if (error.code === 6) return; // Duplicate delivery of the same event.
      throw error;
    }
    data.notificationId = id;
  }

  const user = await db.collection("users").doc(uid).get();
  if (user.get("pushEnabled") === false) return;

  const token = user.get("fcmToken");
  if (typeof token !== "string" || token.length === 0) return;

  const message = {
    token,
    notification: {title, body},
    data,
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
  const visible = [];
  let page = await query.limit(100).get();
  while (!page.empty && visible.length < 51) {
    for (const doc of page.docs) {
      if (!chatNotificationTypes.includes(doc.get("type"))) visible.push(doc);
      if (visible.length === 51) break;
    }
    if (page.size < 100 || visible.length === 51) break;
    page = await query.startAfter(page.docs[page.size - 1]).limit(100).get();
  }
  const docs = visible.slice(0, 50);
  return {
    items: docs.map((doc) => {
      const {title, body, type, groupId, createdAt} = doc.data();
      return {
        id: doc.id, title, body, type, groupId, createdAt: createdAt.toMillis(),
        read: doc.get("read") === true,
      };
    }),
    nextCursor: visible.length > 50 ? docs[49].id : null,
  };
});

exports.getNotificationUnreadCount = onCall(
    {region: "asia-northeast3"},
    async (req) => {
      if (!req.auth) {
        throw new HttpsError("unauthenticated", "로그인이 필요해요.");
      }
      const items = getFirestore().collection("notificationInboxes")
          .doc(req.auth.uid).collection("items");
      const [total, read, legacyChat] = await Promise.all([
        items.count().get(),
        items.where("read", "==", true).count().get(),
        items.where("type", "in", chatNotificationTypes).select("read").get(),
      ]);
      const unreadChat = legacyChat.docs.filter((doc) =>
        doc.get("read") !== true,
      ).length;
      return {unread: Math.max(0,
          total.data().count - read.data().count - unreadChat)};
    },
);

exports.markNotificationRead = onCall(
    {region: "asia-northeast3"},
    async (req) => {
      if (!req.auth) {
        throw new HttpsError("unauthenticated", "로그인이 필요해요.");
      }
      const id = req.data && req.data.id;
      if (typeof id !== "string" || !/^[a-f0-9]{64}$/.test(id)) {
        throw new HttpsError("invalid-argument", "잘못된 알림이에요.");
      }
      const db = getFirestore();
      const inbox = db.collection("notificationInboxes").doc(req.auth.uid);
      const item = inbox.collection("items").doc(id);
      await db.runTransaction(async (transaction) => {
        const current = await transaction.get(item);
        if (!current.exists || current.get("read") === true) return;
        transaction.update(item, {read: true});
        transaction.set(inbox, {changedAt: FieldValue.serverTimestamp()},
            {merge: true});
      });
      return {read: true};
    },
);

exports.clearNotificationHistory = onCall(
    {region: "asia-northeast3"},
    async (req) => {
      if (!req.auth) {
        throw new HttpsError("unauthenticated", "로그인이 필요해요.");
      }

      const db = getFirestore();
      const cutoff = Timestamp.now();
      const inbox = db.collection("notificationInboxes").doc(req.auth.uid);
      const items = inbox.collection("items");
      const query = items.where("createdAt", "<=", cutoff).limit(400);
      let page = await query.get();
      while (!page.empty) {
        const batch = db.batch();
        page.docs.forEach((doc) => batch.delete(doc.ref));
        batch.set(inbox, {changedAt: FieldValue.serverTimestamp()},
            {merge: true});
        await batch.commit();
        page = await query.get();
      }
      return {cleared: true};
    },
);

exports.castGroupCancellationVote = onCall(
    {region: "asia-northeast3"},
    async (req) => {
      if (!req.auth) {
        throw new HttpsError("unauthenticated", "로그인이 필요해요.");
      }
      const {groupId, withdraw = false} = req.data || {};
      if (typeof groupId !== "string" || !groupId || groupId.includes("/") ||
          groupId === "." || groupId === ".." ||
          typeof withdraw !== "boolean" ||
          Object.keys(req.data).some((key) =>
            key !== "groupId" && key !== "withdraw",
          )) {
        throw new HttpsError("invalid-argument", "최신 앱에서 다시 시도해주세요.");
      }
      const uid = req.auth.uid;
      const db = getFirestore();
      const groupRef = db.collection("groups").doc(groupId);
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

        const capacity = group.get("maxMember");
        if (!Number.isSafeInteger(capacity) || capacity < 1) {
          throw new HttpsError("failed-precondition", "모임 정원을 확인해주세요.");
        }
        const required = capacity === 2 ? 1 : Math.floor(capacity / 2) + 1;
        const round = group.get("cancelVoteRound") || 0;
        const rounds = groupRef.collection("cancelVotes");
        const votes = round === 0 ? rounds :
          rounds.doc(String(round)).collection("ballots");
        const snapshot = await transaction.get(votes);
        const approved = new Set(snapshot.docs.filter((doc) =>
          participants.has(doc.id) && doc.get("approve") === true,
        ).map((doc) => doc.id));
        const alreadyApproved = approved.has(uid);
        if (withdraw) {
          if (alreadyApproved) transaction.delete(votes.doc(uid));
          approved.delete(uid);
        } else {
          if (!alreadyApproved) {
            transaction.set(votes.doc(uid), {approve: true});
          }
          approved.add(uid);
        }

        const yes = approved.size;
        const canceled = yes >= required;
        const update = {
          cancelApprovalCount: yes,
          cancelVoteUpdatedAt: FieldValue.serverTimestamp(),
        };
        if (canceled) {
          update.status = 2;
          update.canceledByVote = true;
        }
        transaction.update(groupRef, update);
        return {
          yes, required, canceled, approved: !withdraw, alreadyApproved,
        };
      });
    },
);

exports.notifyGroupStart = onSchedule("every 1 minutes", async () => {
  const db = getFirestore();
  const oneHourAhead = Date.now() + 60 * 60 * 1000;
  const groups = await db.collection("groups")
      .where("dateTime", ">", oneHourAhead - 2 * 60 * 1000)
      .where("dateTime", "<=", oneHourAhead)
      .get();

  for (const group of groups.docs) {
    const dateTime = group.get("dateTime");
    const recipients = await db.runTransaction(async (transaction) => {
      const current = await transaction.get(group.ref);
      const status = current.get("status");
      if (current.get("dateTime") !== dateTime ||
          (status !== 0 && status !== 1) ||
          current.get("oneHourReminderFor") === dateTime) {
        return [];
      }

      const memberIds = current.get("memberIds") || [];
      const hostId = current.get("hostId");
      // Reserve the reminder before sending so overlapping runs skip it.
      transaction.update(group.ref, {oneHourReminderFor: dateTime});
      return [...new Set([hostId, ...memberIds])].filter((uid) => uid);
    });

    await Promise.all(recipients.map(async (uid) => {
      try {
        await sendGroupMemberNotification(
            uid, "모임 시작 1시간 전", "모임이 약 1시간 뒤에 시작돼요.",
            "group_start", group.id, `one-hour:${group.id}:${dateTime}`,
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
