const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());


//KẾT NỐI MONGODB 

const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
    .then(() => console.log("✅ Đã kết nối MongoDB thành công!"))
    .catch(err => console.error("❌ Lỗi kết nối MongoDB:", err));

const countryTimelineSchema = new mongoose.Schema({
    countryId: String,
    countryName: String,
    metrics: [{
        name: String,
        unit: String,
        history: [{
            year: Number,
            value: Number
        }]
    }]
});
const CountryTimeline = mongoose.model('CountryTimeline', countryTimelineSchema);

// BẢNG 2: THÔNG BÁO PUSH NOTIFICATION
const notificationSchema = new mongoose.Schema({
    title: String,
    content: String,
    isSuccess: Boolean,
    createdAt: { type: Date, default: Date.now }
});
const Notification = mongoose.model('Notification', notificationSchema);

// BẢNG 3: LỊCH SỬ TRÒ CHUYỆN AI BOT
const chatSchema = new mongoose.Schema({
    role: String, // 'user' hoặc 'ai'
    content: String,
    createdAt: { type: Date, default: Date.now }
});
const ChatMessage = mongoose.model('ChatMessage', chatSchema);



// 3. CÁC API XỬ LÝ 

app.get('/api/countrytimelines', async (req, res) => {
    try {
        const allData = await CountryTimeline.find({});
        
        // LƯU 1 THÔNG BÁO VÀO MONGODB
        await Notification.create({
            title: "Hệ thống đồng bộ",
            content: `✅ Đã kết nối và đồng bộ thành công dữ liệu ${allData.length} quốc gia.`,
            isSuccess: true
        });

        res.status(200).json({ success: true, data: allData });
    } catch (err) {
        //  LỖI 
        await Notification.create({
            title: "Lỗi hệ thống",
            content: `❌ Mất kết nối CSDL, không thể tải dữ liệu.`,
            isSuccess: false
        });
        res.status(500).json({ success: false, message: "Lỗi DB" });
    }
});


// --- API QUẢN LÝ THÔNG BÁO ---

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




app.get('/api/chat/history', async (req, res) => {
    try {
        const history = await ChatMessage.find().sort({ createdAt: 1 });
        res.json({ success: true, data: history });
    } catch (err) {
        res.json({ success: false });
    }
});


app.post('/api/chat/clear', async (req, res) => {
    try {
        await ChatMessage.deleteMany({});
        res.json({ success: true, message: "Đã xóa lịch sử Chat" });
    } catch (err) {
        res.json({ success: false });
    }
});

app.post('/api/chat', async (req, res) => {
    try {
        const userMessage = req.body.message;
        
        // 1. Lưu câu hỏi của User vào MongoDB
        await ChatMessage.create({ role: "user", content: userMessage });

        const systemPrompt = `Bạn là Trợ lý AI Vĩ mô. Trả lời JSON: {"reply": "câu trả lời", "action": "ZOOM_TO|NONE", "targetId": "VNM"}`;

        console.log("Đang gọi OpenRouter..."); 
        
        const aiResponse = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                model: "openrouter/auto", 
                messages: [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: userMessage }
                ]
            })
        });

        const data = await aiResponse.json();
        
        // Bắt lỗi nếu OpenRouter từ chối (Sai Key, hết hạn mức...)
        if (data.error) {
            console.error("LỖI TỪ OPENROUTER:", data.error);
            return res.status(500).json({ success: false, message: data.error.message });
        }

        let botContent = data.choices[0].message.content.replace(/```json/gi, "").replace(/```/gi, "").trim();
        const resultJson = JSON.parse(botContent);

        // 2. Lưu câu trả lời của AI vào MongoDB
        await ChatMessage.create({ role: "ai", content: resultJson.reply });

        res.json({ success: true, data: resultJson });
    } catch (err) {
        console.error("LỖI SERVER Node.js:", err);
        res.status(500).json({ success: false, message: "Lỗi xử lý AI" });
    }
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => console.log(`🚀 Server Vĩ Mô đang chạy tại cổng ${PORT}`));