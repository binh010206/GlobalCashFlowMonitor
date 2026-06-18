const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// ==========================================
// KẾT NỐI MONGODB 
// ==========================================
const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
    .then(() => console.log("✅ Đã kết nối MongoDB thành công!"))
    .catch(err => console.error("❌ Lỗi kết nối MongoDB:", err));

// ==========================================
// ĐỊNH NGHĨA BẢNG DỮ LIỆU (SCHEMAS)
// ==========================================
const countryTimelineSchema = new mongoose.Schema({
    countryId: String,
    countryName: String,
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

// ==========================================
// HÀM "MÁY HÚT BỤI" TỰ ĐỘNG DỌN RÁC
// (Ép Data về đúng 100 dòng, xóa sạch phần dư)
// ==========================================
async function cleanDatabase(Model, limit = 100) {
    try {
        const count = await Model.countDocuments();
        if (count > limit) {
            const overLimit = count - limit; // Tính số dòng dư thừa (Ví dụ: 1800 - 100 = 1700 dòng dư)
            // Lấy danh sách ID của những dòng cũ nhất
            const oldest = await Model.find().sort({ createdAt: 1 }).limit(overLimit);
            const idsToDelete = oldest.map(item => item._id);
            // Xóa sạch chúng nó
            await Model.deleteMany({ _id: { $in: idsToDelete } });
            console.log(`🧹 Đã tự động dọn dẹp ${overLimit} dữ liệu rác khỏi bảng ${Model.modelName}.`);
        }
    } catch (err) {
        console.error(`❌ Lỗi khi dọn dẹp bảng ${Model.modelName}:`, err);
    }
}


// ==========================================
// CÁC API XỬ LÝ
// ==========================================

// 1. API Tải dữ liệu Bản Đồ + Lưu Thông báo
app.get('/api/countrytimelines', async (req, res) => {
    try {
        const allData = await CountryTimeline.find({});
        
        // Tạo thông báo tải data thành công
        await Notification.create({
            title: "Hệ thống đồng bộ",
            content: `✅ Đã kết nối và đồng bộ thành công dữ liệu ${allData.length} quốc gia.`,
            isSuccess: true
        });

        // TỰ ĐỘNG DỌN RÁC BẢNG THÔNG BÁO (Nếu quá 100 dòng sẽ tự cắt)
        await cleanDatabase(Notification, 100);

        res.status(200).json({ success: true, data: allData });
    } catch (err) {
        await Notification.create({ title: "Lỗi hệ thống", content: `❌ Mất kết nối CSDL, không thể tải dữ liệu.`, isSuccess: false });
        res.status(500).json({ success: false, message: "Lỗi DB" });
    }
});


// 2. API Quản lý thông báo (Dành cho App)
app.get('/api/notifications', async (req, res) => {
    try {
        const notis = await Notification.find().sort({ createdAt: -1 }); 
        res.json({ success: true, data: notis });
    } catch (err) {
        res.json({ success: false, message: "Lỗi DB" });
    }
});

app.post('/api/notifications/clear', async (req, res) => {
    try {
        await Notification.deleteMany({});
        res.json({ success: true, message: "Đã xóa sạch thông báo" });
    } catch (err) {
        res.json({ success: false, message: "Lỗi xóa" });
    }
});


// 3. API Chat AI (Với OpenRouter)
app.get('/api/chat/history', async (req, res) => {
    try {
        const history = await ChatMessage.find().sort({ createdAt: 1 });
        res.json({ success: true, data: history });
    } catch (err) { res.json({ success: false }); }
});

app.post('/api/chat/clear', async (req, res) => {
    try {
        await ChatMessage.deleteMany({});
        res.json({ success: true, message: "Đã xóa lịch sử Chat" });
    } catch (err) { res.json({ success: false }); }
});

app.post('/api/chat', async (req, res) => {
    try {
        const userMessage = req.body.message;
        
        // Lưu câu hỏi người dùng
        await ChatMessage.create({ role: "user", content: userMessage });

        const systemPrompt = `Bạn là Trợ lý AI Vĩ mô. BẮT BUỘC CHỈ trả lời bằng một chuỗi JSON hợp lệ. Định dạng chuẩn: {"reply": "câu trả lời", "action": "ZOOM_TO|NONE", "targetId": "VNM"}`;

        const aiResponse = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: { "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`, "Content-Type": "application/json" },
            body: JSON.stringify({
                model: "openrouter/auto", 
                messages: [ { role: "system", content: systemPrompt }, { role: "user", content: userMessage } ]
            })
        });

        const data = await aiResponse.json();
        
        if (data.error) {
            return res.status(500).json({ success: false, message: data.error.message });
        }

        let botContent = data.choices[0].message.content.replace(/```json/gi, "").replace(/```/gi, "").trim();
        let resultJson;
        try {
            resultJson = JSON.parse(botContent);
        } catch (parseError) {
            resultJson = { reply: botContent, action: "NONE", targetId: "" };
        }

        // Lưu câu trả lời AI
        await ChatMessage.create({ role: "ai", content: resultJson.reply });

        // TỰ ĐỘNG DỌN RÁC BẢNG CHAT AI (Giữ 100 tin mới nhất)
        await cleanDatabase(ChatMessage, 100);

        res.json({ success: true, data: resultJson });
    } catch (err) {
        console.error("LỖI SERVER Node.js:", err);
        res.status(500).json({ success: false, message: "Lỗi xử lý AI" });
    }
});


// ==========================================
// 4. TÍNH NĂNG MỚI: API THỐNG KÊ TOÀN DIỆN (BONUS)
// ==========================================
app.get('/api/stats', async (req, res) => {
    try {
        const totalCountries = await CountryTimeline.countDocuments();
        const totalNotis = await Notification.countDocuments();
        const totalMessages = await ChatMessage.countDocuments();
        const userQuestions = await ChatMessage.countDocuments({ role: "user" });

        res.json({
            success: true,
            data: {
                countries_loaded: totalCountries,
                system_notifications: totalNotis,
                ai_usage: {
                    total_messages_exchanged: totalMessages,
                    questions_asked_by_user: userQuestions
                }
            }
        });
    } catch (err) {
        res.status(500).json({ success: false });
    }
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => console.log(`🚀 Server Vĩ Mô đang chạy tại cổng ${PORT}`));