const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

router.post('/', async (req, res) => {
    try {
        const userMessage = req.body.message;
        const Flow = mongoose.model('Flow');
        const allFlows = await Flow.find();

        if (!process.env.OPENROUTER_API_KEY) {
            return res.status(200).json({ 
                success: true, 
                data: { reply: "Thiếu cấu hình biến OPENROUTER_API_KEY.", action: "NONE", targetId: "" } 
            });
        }

        const systemPrompt = `
        Bạn là Trợ lý AI của ứng dụng Global Cash Flow Monitor.
        Dữ liệu dòng tiền (Tỷ USD): ${JSON.stringify(allFlows)}
        Nhiệm vụ: Phân tích ngắn gọn. Quyết định có di chuyển bản đồ đến quốc gia nào không.
        BẮT BUỘC TRẢ VỀ DUY NHẤT 1 CHUỖI JSON THEO ĐỊNH DẠNG SAU (KHÔNG BỌC \`\`\`json):
        {
            "reply": "Câu trả lời chuyên nghiệp bằng tiếng Việt",
            "action": "ZOOM_TO" hoặc "NONE",
            "targetId": "Mã quốc gia viết hoa (ví dụ: US, VN) nếu action là ZOOM_TO. Ngược lại để rỗng"
        }
        Câu hỏi của user: "${userMessage}"
        `;

        // Gọi API lên OpenRouter với model chắc chắn tồn tại 100%
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
                "HTTP-Referer": "https://globalcashflowbackend.onrender.com",
                "X-Title": "Global Cash Flow",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "model": "google/gemini-2.0-flash-thinking-exp:free", // 🌟 CON NÀY ĐANG LIVE 100% FREE
                "messages": [{ "role": "user", "content": systemPrompt }]
            })
        });

        const jsonRes = await response.json();

        if (!jsonRes.choices || jsonRes.choices.length === 0) {
            console.error("OpenRouter báo lỗi payload:", jsonRes);
            return res.status(200).json({ 
                success: true, 
                data: { reply: "Hệ thống AI đang nghẽn mạch, thử lại sau mậy!", action: "NONE", targetId: "" } 
            });
        }
        
        let responseText = jsonRes.choices[0].message.content;
        responseText = responseText.replace(/```json/g, "").replace(/```/g, "").trim();

        res.status(200).json({ success: true, data: JSON.parse(responseText) });

    } catch (error) {
        console.error("❌ Lỗi Crash Server:", error);
        res.status(200).json({ 
            success: true, 
            data: { reply: "AI đang bảo trì vĩ mô.", action: "NONE", targetId: "" } 
        });
    }
});

module.exports = router;