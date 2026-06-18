const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// ==========================================
// 1. KẾT NỐI MONGODB 
// ==========================================
const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
    .then(() => console.log("✅ Đã kết nối MongoDB thành công!"))
    .catch(err => console.error("❌ Lỗi kết nối MongoDB:", err));

// ==========================================
// 2. ĐỊNH NGHĨA BẢNG DỮ LIỆU (SCHEMAS)
// ==========================================
const countryTimelineSchema = new mongoose.Schema({
    countryId: String, countryName: String,
    metrics: [{ name: String, unit: String, history: [{ year: Number, value: Number }] }]
});
const CountryTimeline = mongoose.model('CountryTimeline', countryTimelineSchema);

const notificationSchema = new mongoose.Schema({
    title: String, content: String, isSuccess: Boolean, createdAt: { type: Date, default: Date.now }
});
const Notification = mongoose.model('Notification', notificationSchema);

const chatSchema = new mongoose.Schema({
    role: String, content: String, createdAt: { type: Date, default: Date.now }
});
const ChatMessage = mongoose.model('ChatMessage', chatSchema);

const roomSchema = new mongoose.Schema({
    name: String, createdAt: { type: Date, default: Date.now }
});
const Room = mongoose.model('Room', roomSchema);

// ÉP CỨNG TÊN BẢNG CHO DỄ TÌM TRÊN ATLAS
const groupMessageSchema = new mongoose.Schema({
    roomId: String, senderName: String, senderEmail: String, content: String,
    type: { type: String, default: "TEXT" }, countryId: { type: String, default: "" }, 
    createdAt: { type: Date, default: Date.now }
});
const GroupMessage = mongoose.model('GroupMessage', groupMessageSchema, 'groupmessages');

// ==========================================
// HÀM "MÁY HÚT BỤI" TỰ ĐỘNG DỌN RÁC (ÉP VỀ 100 DÒNG)
// ==========================================
async function cleanDatabase(Model, limit = 100) {
    try {
        const count = await Model.countDocuments();
        if (count > limit) {
            const overLimit = count - limit;
            const oldest = await Model.find().sort({ createdAt: 1 }).limit(overLimit);
            const idsToDelete = oldest.map(item => item._id);
            await Model.deleteMany({ _id: { $in: idsToDelete } });
            console.log(`🧹 Đã dọn dẹp ${overLimit} dữ liệu rác khỏi bảng ${Model.modelName}.`);
        }
    } catch (err) { console.error(`❌ Lỗi khi dọn dẹp:`, err); }
}

// ==========================================
// 3. API BẢN ĐỒ & THÔNG BÁO
// ==========================================
app.get('/api/countrytimelines', async (req, res) => {
    try {
        const allData = await CountryTimeline.find({});
        await Notification.create({ title: "Hệ thống đồng bộ", content: `✅ Đã kết nối và đồng bộ thành công dữ liệu ${allData.length} quốc gia.`, isSuccess: true });
        
        // Hút bụi bảng Notification
        await cleanDatabase(Notification, 100);
        res.status(200).json({ success: true, data: allData });
    } catch (err) {
        await Notification.create({ title: "Lỗi hệ thống", content: `❌ Mất kết nối CSDL, không thể tải dữ liệu.`, isSuccess: false });
        res.status(500).json({ success: false, message: "Lỗi DB" });
    }
});

app.get('/api/notifications', async (req, res) => {
    try { res.json({ success: true, data: await Notification.find().sort({ createdAt: -1 }) }); } catch (err) { res.json({ success: false }); }
});

// API Xóa thủ công Thông báo (Đã hoàn trả)
app.post('/api/notifications/clear', async (req, res) => {
    try {
        await Notification.deleteMany({});
        res.json({ success: true, message: "Đã xóa sạch thông báo" });
    } catch (err) { res.json({ success: false, message: "Lỗi xóa" }); }
});

// ==========================================
// 4. API CHAT TRỢ LÝ AI
// ==========================================
app.get('/api/chat/history', async (req, res) => {
    try { res.json({ success: true, data: await ChatMessage.find().sort({ createdAt: 1 }) }); } catch (err) { res.json({ success: false }); }
});

// API Xóa thủ công Chat AI (Đã hoàn trả)
app.post('/api/chat/clear', async (req, res) => {
    try {
        await ChatMessage.deleteMany({});
        res.json({ success: true, message: "Đã xóa lịch sử Chat" });
    } catch (err) { res.json({ success: false }); }
});

app.post('/api/chat', async (req, res) => {
    try {
        const userMessage = req.body.message;
        await ChatMessage.create({ role: "user", content: userMessage });

        // SIÊU PROMPT TỐI THƯỢNG
        const systemPrompt = `Bạn là một Chuyên gia Kinh tế Vĩ mô cấp cao đang tư vấn trên hệ thống Global Cash Flow. 
Nhiệm vụ của bạn là phân tích dữ liệu, giải thích các hiện tượng kinh tế một cách chuyên sâu nhưng dễ hiểu.
QUAN TRỌNG: BẮT BUỘC toàn bộ câu trả lời của bạn phải là MỘT CHUỖI JSON HỢP LỆ. KHÔNG CÓ BẤT KỲ VĂN BẢN NÀO BÊN NGOÀI KHỐI JSON.
Cấu trúc JSON yêu cầu:
{
    "reply": "Câu trả lời chi tiết của bạn (dùng \\n để xuống dòng, KHÔNG dùng markdown)",
    "action": "ZOOM_TO" hoặc "NONE",
    "targetId": "Mã quốc gia ISO Alpha-3 tương ứng"
}`;

        const aiResponse = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST", headers: { "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`, "Content-Type": "application/json" },
            body: JSON.stringify({ model: "openrouter/auto", messages: [ { role: "system", content: systemPrompt }, { role: "user", content: userMessage } ] })
        });

        const data = await aiResponse.json();
        let botContent = data.choices[0].message.content.replace(/```json/gi, "").replace(/```/gi, "").trim();
        
        let resultJson;
        try { resultJson = JSON.parse(botContent); } catch (parseError) { resultJson = { reply: botContent, action: "NONE", targetId: "" }; }
        
        await ChatMessage.create({ role: "ai", content: resultJson.reply });
        
        // Hút bụi bảng Chat AI
        await cleanDatabase(ChatMessage, 100);
        res.json({ success: true, data: resultJson });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// 5. API CHAT CỘNG ĐỒNG (NHÓM)
// ==========================================
app.get('/api/rooms', async (req, res) => {
    try { res.json({ success: true, data: await Room.find().sort({ createdAt: -1 }) }); } catch (err) { res.status(500).json({ success: false }); }
});

app.post('/api/rooms', async (req, res) => {
    try {
        // Nếu để trống tên, tự sinh mã ngẫu nhiên để chống trùng E11000
        const roomName = req.body.name || `Phòng thảo luận #${Math.floor(Math.random() * 10000)}`;
        
        const newRoom = await Room.create({ name: roomName });
        
        // Tạo tin nhắn hệ thống đầu tiên
        await GroupMessage.create({ 
            roomId: newRoom._id.toString(), senderName: "Hệ thống", senderEmail: "admin@system.com", 
            content: `Chào mừng đến với ${newRoom.name}!`, type: "SYSTEM" 
        });
        
        res.json({ success: true, data: newRoom });
    } catch (err) { 
        console.error("Lỗi khi tạo phòng:", err);
        // BẮT BỆNH E11000 (TRÙNG TÊN)
        if (err.code === 11000) {
            return res.status(400).json({ success: false, message: "Tên kênh này đã tồn tại! Vui lòng chọn tên khác." });
        }
        res.status(500).json({ success: false, message: "Lỗi Server không xác định" }); 
    }
});

app.get('/api/rooms/:roomId/messages', async (req, res) => {
    try { res.json({ success: true, data: await GroupMessage.find({ roomId: req.params.roomId }).sort({ createdAt: 1 }) }); } catch (err) { res.status(500).json({ success: false }); }
});

app.post('/api/rooms/:roomId/messages', async (req, res) => {
    try {
        const { senderName, senderEmail, content, type, countryId } = req.body;
        const newMsg = await GroupMessage.create({ roomId: req.params.roomId, senderName, senderEmail, content, type: type || "TEXT", countryId: countryId || "" });
        
        // Hút bụi bảng GroupMessage của phòng hiện tại
        const msgCount = await GroupMessage.countDocuments({ roomId: req.params.roomId });
        if (msgCount > 100) {
            const oldest = await GroupMessage.find({ roomId: req.params.roomId }).sort({ createdAt: 1 }).limit(msgCount - 100);
            const idsToDelete = oldest.map(msg => msg._id);
            await GroupMessage.deleteMany({ _id: { $in: idsToDelete } });
        }
        res.json({ success: true, data: newMsg });
    } catch (err) { res.status(500).json({ success: false }); }
});

// ==========================================
// 6. API THỐNG KÊ TOÀN DIỆN
// ==========================================
app.get('/api/stats', async (req, res) => {
    try {
        res.json({
            success: true,
            data: {
                countries_loaded: await CountryTimeline.countDocuments(),
                system_notifications: await Notification.countDocuments(),
                ai_usage: { total_messages: await ChatMessage.countDocuments(), questions_asked: await ChatMessage.countDocuments({ role: "user" }) },
                community_rooms: await Room.countDocuments()
            }
        });
    } catch (err) { res.status(500).json({ success: false }); }
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => console.log(`🚀 Server Vĩ Mô đang chạy tại cổng ${PORT}`));