const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

router.post('/', async (req, res) => {
    try {
        const userMessage = req.body.message;
        
        // 1. Lấy ID người dùng (Nếu là Khách thì lấy IP mạng làm ID tạm)
        const sessionId = req.headers['x-forwarded-for'] || req.socket.remoteAddress || "guest_device";
        const ChatHistory = mongoose.model('ChatHistory');

        // 🌟 BƯỚC ĂN ĐIỂM 1: KIỂM TRA GIỚI HẠN 5 CÂU CHO KHÁCH
        const chatCount = await ChatHistory.countDocuments({ sessionId: sessionId });
        if (chatCount >= 5) {
            return res.status(200).json({ 
                success: true, 
                data: { 
                    reply: "⚠️ Tài khoản Khách của bạn đã sử dụng hết 5 lượt phân tích AI. Vui lòng Đăng nhập để tiếp tục sử dụng hệ thống Vĩ mô nhé!", 
                    action: "NONE", 
                    targetId: "" 
                } 
            });
        }

        if (!process.env.OPENROUTER_API_KEY) {
            return res.status(200).json({ success: true, data: { reply: "Thiếu Key AI.", action: "NONE", targetId: "" } });
        }

        // Gọi con AI
        const systemPrompt = `
        Bạn là Trợ lý AI Phân tích Vĩ mô. Nhiệm vụ: Phân tích ngắn gọn.
        BẮT BUỘC TRẢ VỀ JSON:
        {
            "reply": "Câu trả lời phân tích tiếng Việt",
            "action": "ZOOM_TO" hoặc "NONE",
            "targetId": "Mã quốc gia 2 chữ (VD: US, VN) nếu ZOOM_TO"
        }`;

        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "model": "openrouter/auto", 
                "messages": [
                    { "role": "system", "content": systemPrompt },
                    { "role": "user", "content": userMessage }
                ]
            })
        });

        const jsonRes = await response.json();
        let responseText = jsonRes.choices[0].message.content;
        responseText = responseText.replace(/```json/g, "").replace(/```/g, "").trim();
        const aiData = JSON.parse(responseText);

        // 🌟 BƯỚC ĂN ĐIỂM 2: LƯU LỊCH SỬ CHAT VÀO MONGODB
        await ChatHistory.create({
            sessionId: sessionId,
            userMessage: userMessage,
            aiResponse: aiData.reply
        });

        // Trả kết quả về cho Android
        res.status(200).json({ success: true, data: aiData });

    } catch (error) {
        console.error("❌ Lỗi Chat AI:", error);
        res.status(200).json({ 
            success: true, 
            data: { reply: "Hệ thống AI đang bảo trì mạng lưới.", action: "NONE", targetId: "" } 
        });
    }
});

module.exports = router;