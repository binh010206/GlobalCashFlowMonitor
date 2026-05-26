const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

router.post('/', async (req, res) => {
    try {
        const userMessage = req.body.message;
        const Flow = mongoose.model('Flow');
        const allFlows = await Flow.find();
        
        const systemPrompt = `
        Bạn là Trợ lý AI của ứng dụng Global Cash Flow Monitor.
        Dữ liệu dòng tiền (Tỷ USD): ${JSON.stringify(allFlows)}
        Nhiệm vụ: Phân tích ngắn gọn và quyết định có cần điều khiển camera bản đồ đến Quốc gia nào không.
        BẮT BUỘC TRẢ VỀ DUY NHẤT 1 CHUỖI JSON THEO ĐỊNH DẠNG NÀY (KHÔNG BỌC ký tự \`\`\`json):
        {
            "reply": "Câu trả lời của bạn bằng tiếng Việt",
            "action": "ZOOM_TO" hoặc "NONE",
            "targetId": "Mã quốc gia viết hoa (ví dụ: US, VN) nếu action là ZOOM_TO. Ngược lại để rỗng"
        }
        Câu hỏi của user: "${userMessage}"
        `;

        // 🌟 GỌI THẲNG BẰNG KEY THẬT, DẸP BIẾN MÔI TRƯỜNG
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": "Bearer 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",
                "HTTP-Referer": "https://globalcashflowbackend.onrender.com", 
                "X-Title": "Global Cash Flow",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "model": "openrouter/free", 
                "messages": [{ "role": "user", "content": systemPrompt }]
            })
        });

        const jsonRes = await response.json();
        
        if (!jsonRes.choices || jsonRes.choices.length === 0) {
            console.error("Lỗi AI trả về:", jsonRes);
            return res.status(200).json({ 
                success: true, 
                data: { reply: "Lỗi OpenRouter: " + (jsonRes.error?.message || "Không rõ"), action: "NONE", targetId: "" } 
            });
        }

        let responseText = jsonRes.choices[0].message.content;
        responseText = responseText.replace(/```json/g, "").replace(/```/g, "").trim();
        
        const aiCommand = JSON.parse(responseText);
        res.status(200).json({ success: true, data: aiCommand });

    } catch (error) {
        console.error("Lỗi Server:", error);
        res.status(500).json({ 
            success: false, 
            data: { reply: "Sập nguồn Server Nodejs: " + error.message, action: "NONE", targetId: "" } 
        });
    }
});

module.exports = router;