const express = require('express');
const mongoose = require('mongoose');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(cors());

const server = http.createServer(app);
// Cấu hình Socket.io cho phép kết nối từ mọi nguồn (App Android)
const io = new Server(server, { cors: { origin: "*" } });

// =========================================
// 1. KẾT NỐI MONGODB ATLAS
// =========================================
const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
  .then(() => {
      console.log("🔥 Kết nối MongoDB thành công!");
      seedRealData(); // Tự động nạp data thật nếu Database đang trống
  })
  .catch(err => console.log("❌ Lỗi MongoDB:", err));

// =========================================
// 2. KHAI BÁO SCHEMA (CẤU TRÚC BẢNG)
// =========================================
const countrySchema = new mongoose.Schema({
    id: String, name: String, lat: Number, lng: Number
});
const Country = mongoose.model('Country', countrySchema);

const flowSchema = new mongoose.Schema({
    sourceId: String, targetId: String, amount: Number, type: String, period: String
});
const Flow = mongoose.model('Flow', flowSchema);

// =========================================
// 3. RESTFUL API (PHỤC VỤ MÀN HÌNH BIỂU ĐỒ & AI)
// =========================================
app.get('/api/flows/fdi', async (req, res) => {
    try {
        const { year } = req.query;
        const filter = { type: 'FDI' };
        if (year) filter.period = year;
        
        const fdiData = await Flow.find(filter).sort({ amount: -1 });
        res.status(200).json({ success: true, data: fdiData });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Lỗi truy xuất FDI', error });
    }
});

app.get('/api/flows/trade', async (req, res) => {
    try {
        const tradeData = await Flow.find({ type: 'TRADE' }).sort({ amount: -1 });
        res.status(200).json({ success: true, data: tradeData });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Lỗi truy xuất TRADE', error });
    }
});

app.get('/api/flows/summary', async (req, res) => {
    try {
        const summary = await Flow.aggregate([
            { $group: { _id: "$type", totalAmount: { $sum: "$amount" }, count: { $sum: 1 } } }
        ]);
        res.status(200).json({ success: true, data: summary });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Lỗi tổng hợp dữ liệu', error });
    }
});

// =========================================
// 4. ĐỘNG CƠ REALTIME (PHỤC VỤ MAPBOX 3D)
// =========================================
io.on('connection', (socket) => {
    console.log("⚡ Radar Android vừa kết nối: " + socket.id);
    let currentIndex = 0;
    
    // Động cơ Time-Lapse: Tua lại lịch sử dòng tiền mỗi 3 giây
    const dataPump = setInterval(async () => {
        try {
            const countries = await Country.find();
            const allRealFlows = await Flow.find(); 
            
            if (allRealFlows.length > 0) {
                // Nhả 2 luồng tiền ra bản đồ cùng lúc
                const flowsToEmit = [
                    allRealFlows[currentIndex % allRealFlows.length],
                    allRealFlows[(currentIndex + 1) % allRealFlows.length]
                ];
                
                socket.emit("cashflow_update", {
                    countries: countries.length > 0 ? countries : getFallbackCountries(),
                    flows: flowsToEmit
                });
                
                currentIndex += 2; 
            }
        } catch (error) { console.error("Lỗi động cơ Realtime:", error); }
    }, 3000); 

    socket.on('disconnect', () => {
        console.log("❌ Radar ngắt kết nối");
        clearInterval(dataPump);
    });
});

// =========================================
// 5. KHỞI ĐỘNG SERVER
// =========================================
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => { 
    console.log(`🚀 Trạm phát sóng đang chạy tại port ${PORT}`); 
});

// =========================================
// 6. HÀM SEED DATA (MỒI DỮ LIỆU)
// =========================================
async function seedRealData() {
    const flowCount = await Flow.countDocuments();
    if (flowCount === 0) {
        console.log("Đang nạp dữ liệu Vĩ mô 2024-2025 vào DB...");
        const countries = getFallbackCountries();
        await Country.insertMany(countries);

        const realFlows = [
            { sourceId: "SG", targetId: "VN", amount: 6.77, type: "FDI", period: "2024" }, 
            { sourceId: "KR", targetId: "VN", amount: 4.50, type: "FDI", period: "2024" }, 
            { sourceId: "CN", targetId: "VN", amount: 4.10, type: "FDI", period: "2024" }, 
            { sourceId: "US", targetId: "VN", amount: 110.0, type: "TRADE", period: "2024" }, 
            { sourceId: "JP", targetId: "VN", amount: 2.8, type: "FDI", period: "2024" },
            { sourceId: "US", targetId: "VN", amount: 14.0, type: "REMITTANCE", period: "2024" }
        ];
        await Flow.insertMany(realFlows);
        console.log("✅ Đã nạp thành công 100% Data thực tế!");
    }
}

function getFallbackCountries() {
    return [
        { id: "VN", name: "Vietnam", lat: 14.0583, lng: 108.2772 },
        { id: "US", name: "United States", lat: 37.0902, lng: -95.7129 },
        { id: "JP", name: "Japan", lat: 36.2048, lng: 138.2529 },
        { id: "CN", name: "China", lat: 35.8617, lng: 104.1954 },
        { id: "KR", name: "South Korea", lat: 35.9078, lng: 127.7669 },
        { id: "SG", name: "Singapore", lat: 1.3521, lng: 103.8198 }
    ];
}