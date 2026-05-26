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

        // Gọi API lên OpenRouter trung chuyển sang Gemini 1.5 Flash (Bao sống, không bóp limit)
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.GEMINI_API_KEY}`, // Điền key OpenRouter trên Render
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "model": "google/gemini-1.5-flash", 
                "messages": [{ "role": "user", "content": systemPrompt }]
            })
        });

        const jsonRes = await response.json();
        
        // Trích xuất văn bản JSON mà AI sinh ra
        let responseText = jsonRes.choices[0].message.content;
        responseText = responseText.replace(/```json/g, "").replace(/```/g, "").trim();
        
        const aiCommand = JSON.parse(responseText);
        res.status(200).json({ success: true, data: aiCommand });

    } catch (error) {
        console.error("Lỗi OpenRouter AI Route:", error);
        res.status(500).json({ 
            success: false, 
            data: { reply: "Hệ thống AI đang bảo trì vĩ mô.", action: "NONE", targetId: "" } 
        });
    }
});

module.exports = router;