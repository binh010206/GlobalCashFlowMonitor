require('dotenv').config(); // Đọc file môi trường .env
const express = require('express');
const mongoose = require('mongoose');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json()); // Cho phép Express đọc dữ liệu JSON gửi lên

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

// =========================================
// KẾT NỐI MONGODB ATLAS
// =========================================
const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
  .then(() => {
      console.log("🔥 Kết nối MongoDB thành công!");
      seedRealData(); 
  })
  .catch(err => console.log("❌ Lỗi MongoDB:", err));

// =========================================
// ĐỊNH NGHĨA CÁC SCHEMA (CHỈ KHAI BÁO)
// =========================================
const countrySchema = new mongoose.Schema({ id: String, name: String, lat: Number, lng: Number });
const Country = mongoose.model('Country', countrySchema);

const flowSchema = new mongoose.Schema({ sourceId: String, targetId: String, amount: Number, type: String, period: String });
const Flow = mongoose.model('Flow', flowSchema);

// =========================================
// NHÚNG CÁC ROUTE PHỤ CHUYÊN BIỆT (MODULAR)
// =========================================
const chatRoute = require('./routes/chatRoute');
const flowRoute = require('./routes/flowRoute');

app.use('/api/chat', chatRoute);   // Trỏ chức năng AI về routes/chatRoute.js
app.use('/api/flows', flowRoute); // Trỏ chức năng Thống kê về routes/flowRoute.js

// =========================================
// ĐỘNG CƠ REALTIME SOCKET.IO (HƯỚNG ĐẾN MAPBOX)
// =========================================
io.on('connection', (socket) => {
    console.log("⚡ Radar Android vừa kết nối: " + socket.id);
    let currentIndex = 0;
    
    const dataPump = setInterval(async () => {
        try {
            const countries = await Country.find();
            const allRealFlows = await Flow.find(); 
            
            if (allRealFlows.length > 0) {
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
        } catch (error) { console.error("Lỗi động cơ:", error); }
    }, 3000); 

    socket.on('disconnect', () => {
        clearInterval(dataPump);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => { console.log(`🚀 Hệ thống mượt mà chạy tại port ${PORT}`); });

async function seedRealData() {
    const flowCount = await Flow.countDocuments();
    if (flowCount === 0) {
        console.log("Đang mồi dữ liệu...");
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
        console.log("✅ Đã nạp thành công!");
    }
}

function getFallbackCountries() {
    return [
        // Đông Nam Á (ASEAN)
        { id: "VN", name: "Vietnam", lat: 14.0583, lng: 108.2772 },
        { id: "SG", name: "Singapore", lat: 1.3521, lng: 103.8198 },
        { id: "TH", name: "Thailand", lat: 15.8700, lng: 100.9925 },
        { id: "MY", name: "Malaysia", lat: 4.2105, lng: 101.9758 },
        { id: "ID", name: "Indonesia", lat: -0.7893, lng: 113.9213 },
        { id: "PH", name: "Philippines", lat: 12.8797, lng: 121.7740 },
        { id: "KH", name: "Cambodia", lat: 12.5657, lng: 104.9910 },
        { id: "LA", name: "Laos", lat: 19.8563, lng: 102.4955 },
        // Đông Á
        { id: "CN", name: "China", lat: 35.8617, lng: 104.1954 },
        { id: "JP", name: "Japan", lat: 36.2048, lng: 138.2529 },
        { id: "KR", name: "South Korea", lat: 35.9078, lng: 127.7669 },
        { id: "TW", name: "Taiwan", lat: 23.6978, lng: 120.9605 },
        { id: "HK", name: "Hong Kong", lat: 22.3193, lng: 114.1694 },
        // Bắc Mỹ & Lục địa khác
        { id: "US", name: "United States", lat: 37.0902, lng: -95.7129 },
        { id: "CA", name: "Canada", lat: 56.1304, lng: -106.3468 },
        { id: "MX", name: "Mexico", lat: 23.6345, lng: -102.5528 },
        { id: "BR", name: "Brazil", lat: -14.2350, lng: -51.9253 },
        { id: "AU", name: "Australia", lat: -25.2744, lng: 133.7751 },
        { id: "NZ", name: "New Zealand", lat: -40.9006, lng: 174.8860 },
        { id: "IN", name: "India", lat: 20.5937, lng: 78.9629 },
        { id: "RU", name: "Russia", lat: 61.5240, lng: 105.3188 },
        { id: "ZA", name: "South Africa", lat: -30.5595, lng: 22.9375 },
        // Châu Âu (EU & lân cận)
        { id: "GB", name: "United Kingdom", lat: 55.3781, lng: -3.4360 },
        { id: "DE", name: "Germany", lat: 51.1657, lng: 10.4515 },
        { id: "FR", name: "France", lat: 46.2276, lng: 2.2137 },
        { id: "IT", name: "Italy", lat: 41.8719, lng: 12.5674 },
        { id: "NL", name: "Netherlands", lat: 52.1326, lng: 5.2913 },
        { id: "CH", name: "Switzerland", lat: 46.8182, lng: 8.2275 },
        { id: "ES", name: "Spain", lat: 40.4637, lng: -3.7492 },
        { id: "SE", name: "Sweden", lat: 60.1282, lng: 18.6435 },
        { id: "NO", name: "Norway", lat: 60.4720, lng: 8.4689 },
        { id: "DK", name: "Denmark", lat: 56.2639, lng: 9.5018 },
        { id: "FI", name: "Finland", lat: 61.9241, lng: 25.7482 },
        { id: "IE", name: "Ireland", lat: 53.4129, lng: -8.2439 },
        { id: "BE", name: "Belgium", lat: 50.5039, lng: 4.4699 },
        { id: "PL", name: "Poland", lat: 51.9194, lng: 19.1451 },
        // Trung Đông
        { id: "AE", name: "UAE", lat: 23.4241, lng: 53.8478 },
        { id: "SA", name: "Saudi Arabia", lat: 23.8859, lng: 45.0792 },
        { id: "TR", name: "Turkey", lat: 38.9637, lng: 35.2433 },
        { id: "IL", name: "Israel", lat: 31.0461, lng: 34.8516 }
    ];
}