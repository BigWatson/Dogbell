const express = require('express');
const nodemailer = require('nodemailer');
const app = express();

app.use(express.json());

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: false,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

// Simple in-memory log
const smsLog = [];

/**
 * POST /text
 * Expects: { phone, message, key }
 * Returns: { success: true/false, quotaRemaining: 1, id: <uuid> }
 */
app.post('/text', async (req, res) => {
  const { phone, message, key, gatewayEmail } = req.body;

  // Validate request
  if (!phone || !message || !key) {
    return res.status(400).json({
      success: false,
      error: 'Missing required fields: phone, message, key'
    });
  }

  // Validate key
  if (key !== 'textbelt') {
    return res.status(401).json({
      success: false,
      error: 'Invalid API key'
    });
  }

  const smsRecord = {
    id: generateId(),
    phone,
    message,
    timestamp: new Date().toISOString(),
    status: 'pending'
  };
  smsLog.push(smsRecord);

  // Send via email-to-SMS gateway
  const to = gatewayEmail || `${phone}@tmomail.net`;
  try {
    await transporter.sendMail({
      from: process.env.SMTP_USER,
      to,
      subject: 'Dogbell Alert',
      text: message,
    });
    smsRecord.status = 'sent';
    console.log(`[SMS] Sent to ${phone} via ${to}`);
    return res.status(200).json({ success: true, quotaRemaining: 999, id: smsRecord.id });
  } catch (err) {
    smsRecord.status = 'failed';
    console.error(`[SMS] Failed to send to ${phone}: ${err.message}`);
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /status/:id
 * Check SMS delivery status
 */
app.get('/status/:id', (req, res) => {
  const record = smsLog.find(r => r.id === req.params.id);
  if (!record) {
    return res.status(404).json({ success: false, error: 'Not found' });
  }
  res.json({ success: true, status: record.status });
});

/**
 * GET /logs
 * View all SMS sent (for debugging)
 */
app.get('/logs', (req, res) => {
  res.json({ sms: smsLog });
});

/**
 * Health check
 */
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Start server
const PORT = process.env.PORT || 80;
app.listen(PORT, () => {
  console.log(`Textbelt-compatible SMS server running on port ${PORT}`);
});

function generateId() {
  return Math.random().toString(36).substring(2, 15).padEnd(24, '0');
}
