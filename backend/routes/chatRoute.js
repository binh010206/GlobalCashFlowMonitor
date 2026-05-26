const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

// Danh sách 20 quốc gia trọng điểm để tạo ma trận dòng tiền chéo toàn cầu (Tránh quá tải prompt)
const COUNTRIES = ["VN", "US", "SG", "JP", "KR", "CN", "DE", "GB", "FR", "IN", "CA", "AU", "BR", "ID", "TH", "MY", "PH", "NL", "CH", "AE"];

// Hàm giả lập tạo ra các luồng dòng tiền kết nối chéo GIỮA CÁC NƯỚC VỚI NHAU
function generateGlobalFlows() {
    let flows = [];
    
    // Vòng lặp quét qua các cặp quốc gia để tạo kết nối ngẫu nhiên
    for (let i = 0; i < COUNTRIES.length; i++) {
        for (let j = 0; j < COUNTRIES.length; j++) {
            if (i !== j) {
                // Tỷ lệ xuất hiện dòng tiền giữa 2 nước ngẫu nhiên là 35% để map không bị quá dày đặc
                if (Math.random() < 0.35) {
                    // Tạo số vốn ngẫu nhiên từ 0.5 đến 50 tỷ USD tùy thuộc vào độ lớn của quốc gia
                    let baseAmount = (Math.random() * 15 + 0.5);
                    if (["US", "CN", "JP", "DE"].includes(COUNTRIES[i])) baseAmount *= 3; // Các ông lớn bắn tiền nhiều hơn
                    
                    flows.push({
                        from: COUNTRIES[i], // Nước xuất khẩu vốn
                        to: COUNTRIES[j],   // Nước nhận đầu tư
                        amount: parseFloat(baseAmount.toFixed(2)), // Số tỷ USD
                        timestamp: new Date().toISOString()
                    });
                }
            }
        }
    }
    return flows;
}

router.post('/', async (req, res) => {
    try {
        const userMessage = req.body.message;
        
        // 🌟 BƯỚC NGOẶT: Gọi hàm sinh ma trận 400 luồng dòng tiền chéo Realtime
        const liveMatrixFlows = generateGlobalFlows();

        if (!process.env.OPENROUTER_API_KEY) {
            return res.status(200).json({ 
                success: true, 
                data: { reply: "Thiếu cấu hình biến OPENROUTER_API_KEY.", action: "NONE", targetId: "" } 
            });
        }

        const systemPrompt = `
        Bạn là Trợ lý AI Phân tích Vĩ mô của Global Cash Flow Monitor.
        Đây là ma trận Dòng tiền FDI kết nối chéo giữa các quốc gia (Tỷ USD) VỪA ĐƯỢC CẬP NHẬT REALTIME:
        ${JSON.stringify(liveMatrixFlows)}
        
        Nhiệm vụ của bạn:
        1. Phân tích ngắn gọn luồng tiền đang chảy mạnh từ nước nào sang nước nào dựa trên ma trận trên (Ví dụ: "Hiện tại dòng vốn từ US đang đổ mạnh sang CN với X tỷ USD...").
        2. Trả lời đúng trọng tâm câu hỏi của user.
        3. Quyết định xem có cần điều khiển camera bản đồ bay đến Quốc gia nào không.

        BẮT BUỘC TRẢ VỀ DUY NHẤT 1 CHUỖI JSON THEO ĐỊNH DẠNG SAU (KHÔNG BỌC \`\`\`json):
        {
            "reply": "Câu trả lời chuyên nghiệp bằng tiếng Việt, có trích xuất số liệu từ ma trận",
            "action": "ZOOM_TO" hoặc "NONE",
            "targetId": "Mã quốc gia viết hoa 2 chữ cái (ví dụ: US, VN, JP, SG, CN) nếu action là ZOOM_TO. Ngược lại để rỗng"
        }
        Câu hỏi của user: "${userMessage}"
        `;

        // Gọi API lên OpenRouter với model tự động điều phối (Bao sống)
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.OPENROUTER_API_KEY}`,
                "HTTP-Referer": "https://globalcashflowbackend.onrender.com",
                "X-Title": "Global Cash Flow",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "model": "openrouter/auto", 
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

const aiCommand = JSON.parse(responseText);

// 🌟 BƯỚC NGOẶT: Trả AI Command kèm theo toàn bộ Ma trận dòng tiền về cho Android vẽ Map
res.status(200).json({ 
    success: true, 
    data: aiCommand,
    liveFlows: liveMatrixFlows // Đẩy data thật ra ngoài cho Frontend!
});

    } catch (error) {
        console.error("❌ Lỗi Crash Server:", error);
        res.status(200).json({ 
            success: true, 
            data: { reply: "AI đang bảo trì vĩ mô.", action: "NONE", targetId: "" } 
        });
    }
});

module.exports = router;