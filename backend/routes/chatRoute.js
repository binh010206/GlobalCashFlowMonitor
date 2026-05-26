const express = require('express');
const router = express.Router();
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Gọi Model Flow (Dòng tiền) từ DB để AI đọc
const Flow = require('mongoose').model('Flow'); 

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

router.post('/', async (req, res) => {
    try {
        const userMessage = req.body.message;
        const allFlows = await Flow.find();
        
        const systemPrompt = `
        Bạn là Trợ lý AI của ứng dụng Global Cash Flow Monitor.
        Dữ liệu dòng tiền hiện tại: ${JSON.stringify(allFlows)}
        Nhiệm vụ: Phân tích ngắn gọn.
        BẮT BUỘC TRẢ VỀ JSON CÓ ĐỊNH DẠNG SAU:
        { "reply": "...", "action": "ZOOM_TO" hoặc "NONE", "targetId": "Mã quốc gia hoặc rỗng" }
        Câu hỏi: "${userMessage}"
        `;

        const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
        const result = await model.generateContent(systemPrompt);
        let responseText = result.response.text().replace(/```json/g, "").replace(/```/g, "").trim();
        
        res.status(200).json({ success: true, data: JSON.parse(responseText) });
    } catch (error) {
        console.error("Lỗi AI:", error);
        res.status(500).json({ success: false, data: { reply: "Lỗi AI", action: "NONE", targetId: "" } });
    }
});

module.exports = router;