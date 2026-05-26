const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

router.post('/', async (req, res) => {
    try {
        const userMessage = req.body.message;
        const Flow = mongoose.model('Flow');
        const allFlows = await Flow.find();

        // 1. Kiểm tra an toàn: Đọc Key từ biến môi trường
        if (!process.env.OPENROUTER_API_KEY) {
            console.error("Lỗi: Không tìm thấy OPENROUTER_API_KEY");
            return res.status(200).json({ 
                success: true, 
                data: { reply: "Lỗi hệ thống: Chưa cấu hình API Key.", action: "NONE", targetId: "" } 
            });
        }

        const systemPrompt = `
        Bạn là Trợ lý AI của ứng dụng Global Cash Flow Monitor.
        Dữ liệu dòng tiền (Tỷ USD): ${JSON.stringify(allFlows)}
        Nhiệm vụ: Phân tích ngắn gọn. Quyết định có di chuyển bản đồ đến quốc gia nào không.
        BẮT BUỘC TRẢ VỀ DUY NHẤT 1 CHUỖI JSON THEO ĐỊNH DẠNG SAU (KHÔNG BỌC \`\`\`json):
        {
            "reply": "Câu trả lời chuyên nghiệp của bạn",
            "action": "ZOOM_TO" hoặc "NONE",
            "targetId": "Mã quốc gia (ví dụ: US, VN) nếu action là ZOOM_TO. Ngược lại để rỗng"
        }
        Câu hỏi: "${userMessage}"
        `;

        // 2. Gọi AI Llama 3.1 (Bản 8 Tỷ tham số) của Facebook hoàn toàn miễn phí
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "model": "meta-llama/llama-3.1-8b-instruct:free", 
                "messages": [{ "role": "user", "content": systemPrompt }]
            })
        });

        const jsonRes = await response.json();

        // Xử lý nếu API OpenRouter lỗi
        if (!jsonRes.choices || jsonRes.choices.length === 0) {
            return res.status(200).json({ 
                success: true, 
                data: { reply: "Llama AI đang bận, vui lòng thử lại.", action: "NONE", targetId: "" } 
            });
        }
        
        let responseText = jsonRes.choices[0].message.content;
        responseText = responseText.replace(/```json/g, "").replace(/```/g, "").trim();

        res.status(200).json({ success: true, data: JSON.parse(responseText) });

    } catch (error) {
        console.error("❌ Lỗi AI Backend:", error);
        res.status(200).json({ 
            success: true, 
            data: { reply: "AI đang bảo trì dữ liệu vĩ mô. Vui lòng thử lại sau.", action: "NONE", targetId: "" } 
        });
    }
});

module.exports = router;