const dns = require("node:dns");
dns.setDefaultResultOrder("ipv4first");
const {onRequest} = require("firebase-functions/v2/https");
const {onInit} = require("firebase-functions/v2/core");
const admin = require("firebase-admin");
const axios = require("axios");
const http = require("http");
const https = require("https");

// const VPC_CONNECTOR_NAME = "firebase-connector";

// We will reuse these settings for both functions
const VPC_OPTIONS = {
  secrets: ["MSG91_AUTHKEY"],
  timeoutSeconds: 60,
  cors: true,
};

onInit(async () => {
  admin.initializeApp();
  console.log("Firebase Admin Initialized");
});

exports.sendOtp = onRequest(VPC_OPTIONS, async (req, res) => {
  const MSG91_AUTH_KEY = process.env.MSG91_AUTHKEY;
  const phoneNumber = req.body.data.phoneNumber;
  if (!phoneNumber) {
    res.status(400).send({error: "Phone number is required."});
    return;
  }

  try {
    const response = await axios.post(
        `https://control.msg91.com/api/v5/otp?template_id=65f438cbd6fc056f6a2b8dc2&mobile=${phoneNumber}&authkey=${MSG91_AUTH_KEY}`,
        {},
        {
          httpAgent: new http.Agent({family: 4}),
          httpsAgent: new https.Agent({family: 4}),
          timeout: 10000,
        },
    );

    const data = response.data;
    if (data.type === "success") {
      res.status(200).json({data: {success: true, message: "OTP sent!"}});
    } else {
      res.status(500).json({data: {success: false, message: "OTP failed"}});
    }
  } catch (error) {
    console.error("Error sending OTP:", error);
    res.status(500).send({error: "Internal server error."});
  }
});

exports.verifyOtpAndSignIn = onRequest(VPC_OPTIONS, async (req, res) => {
  const MSG91_AUTH_KEY = process.env.MSG91_AUTHKEY;
  const {phoneNumber, otp} = req.body.data;

  if (!phoneNumber || !otp) {
    res.status(400).send({error: "Phone number and OTP are required."});
    return;
  }

  console.log("Verifying OTP for phone number:", phoneNumber);
  console.log("Received OTP:", otp);

  try {
    const response = await axios.get(
        `https://control.msg91.com/api/v5/otp/verify?mobile=${phoneNumber}&otp=${otp}&authkey=${MSG91_AUTH_KEY}`,
        {
          // 👇 ADD THIS BLOCK
          httpAgent: new http.Agent({family: 4}),
          httpsAgent: new https.Agent({family: 4}),
          timeout: 10000,
        },
    );

    const data = response.data;
    if (data.type !== "success") {
      res.status(400).send({error: "Invalid OTP."});
      return;
    }
    const fullPhoneNumber = `+${phoneNumber}`;
    let user;
    try {
      user = await admin.auth().getUserByPhoneNumber(fullPhoneNumber);
    } catch (error) {
      if (error.code === "auth/user-not-found") {
        user = await admin
            .auth()
            .createUser({phoneNumber: fullPhoneNumber});
      } else {
        throw error;
      }
    }

    const customToken = await admin.auth().createCustomToken(user.uid);
    res.status(200).json({data: {token: customToken}});
  } catch (error) {
    console.error("Error verifying OTP:", error);
    res.status(500).send({error: "Failed to verify OTP."});
  }
});
