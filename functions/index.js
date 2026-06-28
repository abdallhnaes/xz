const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendZajelPush = functions.database
  .ref("/zajel_secure_apps/{companyId}/pushQueue/{pushId}")
  .onCreate(async (snapshot, context) => {
    const data = snapshot.val() || {};
    const companyId = context.params.companyId || "zajel_main";
    const targetRole = data.targetRole === "manager" ? "manager" : "employee";
    const topic = `zajel_${companyId}_${targetRole}`;

    const title = String(data.title || "زاجل سحابي");
    const body = String(data.body || "");

    const message = {
      topic,
      notification: { title, body },
      data: {
        title,
        body,
        targetRole,
        companyId
      },
      android: {
        priority: "high",
        notification: {
          channelId: "zajel_channel",
          sound: "default"
        }
      }
    };

    try {
      const messageId = await admin.messaging().send(message);
      await snapshot.ref.update({
        sent: true,
        sentAt: admin.database.ServerValue.TIMESTAMP,
        messageId
      });
    } catch (error) {
      await snapshot.ref.update({
        sent: false,
        error: String(error && error.message ? error.message : error)
      });
    }
  });
