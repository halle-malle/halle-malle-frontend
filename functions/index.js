const {logger, setGlobalOptions} = require("firebase-functions");
const {onDocumentCreated, onDocumentUpdated} =
    require("firebase-functions/v2/firestore");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
setGlobalOptions({maxInstances: 10});

exports.notifyFriendRequest = onDocumentCreated(
    "users/{receiverUid}/friends/{senderUid}",
    async (event) => {
      if (!event.data || event.data.get("status") !== 0) {
        return;
      }

      const receiver = await getFirestore()
          .collection("users").doc(event.params.receiverUid).get();
      const token = receiver.get("fcmToken");
      if (typeof token !== "string" || token.length === 0) {
        return;
      }

      await getMessaging().send({
        token,
        data: {type: "friend_request", recipientUid: event.params.receiverUid},
        notification: {
          title: "친구 요청",
          body: "새 친구 요청이 도착했어요.",
        },
      });
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

        const member = await db.collection("users").doc(uid).get();
        const token = member.get("fcmToken");
        if (typeof token !== "string" || token.length === 0) {
          return;
        }

        await getMessaging().send({
          token,
          data: {
            type: "group_chat",
            groupId: event.params.groupId,
            recipientUid: uid,
          },
          notification: {title, body},
          android: {
            notification: {tag: `group_chat:${uid}:${event.params.groupId}`},
          },
        });
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
            "group_membership", groupId,
        );
      }
      if (left.length > 0) {
        await sendGroupMemberNotification(
            hostId, "모임 탈퇴", `참가자 ${left.length}명이 나갔어요.`,
            "group_membership", groupId,
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
          uid, "모임 취소", "모임이 취소되었어요.",
          "group_cancellation", groupId,
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
          "group_approval", event.params.groupId,
      );
    },
);

/**
 * Sends an FCM notification to a user with a stored token.
 * @param {string} uid Recipient Firebase Auth UID.
 * @param {string} title Notification title.
 * @param {string} body Notification text.
 * @param {string} type Notification destination type.
 * @param {string} groupId Group document ID.
 */
async function sendGroupMemberNotification(uid, title, body, type, groupId) {
  const user = await getFirestore().collection("users").doc(uid).get();
  const token = user.get("fcmToken");
  if (typeof token !== "string" || token.length === 0) {
    return;
  }

  await getMessaging().send({
    token,
    notification: {title, body},
    data: {type, groupId, recipientUid: uid},
  });
}

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
            "group_start", group.id,
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
