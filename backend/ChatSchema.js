const mongoose = require('mongoose');

const ChatSchema = new mongoose.Schema({
    sender: String,    // Ai nhắn?
    message: String,   // Nội dung gì?
    room: String,      // Nhắn ở phòng nào (ví dụ: 'US', 'VN')
    timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Chat', ChatSchema);