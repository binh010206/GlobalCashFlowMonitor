require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const mongoose = require('mongoose');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

// ==========================================
// 1. KẾT NỐI DATABASE MONGODB (Giữ nguyên cấu hình của mày)
// ==========================================
const uri = process.env.MONGO_URI || "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
mongoose.connect(uri)
  .then(() => console.log("✅ Kết nối MongoDB thành công!"))
  .catch(err => console.log("❌ Lỗi MongoDB:", err));

// ==========================================
// 2. KHO DỮ LIỆU TỌA ĐỘ CÁC NƯỚC LỚN
// ==========================================
const countries = [
    { id: "VN", name: "Việt Nam", lat: 16.0544, lng: 108.2022, gdp: 430 }, // Tâm điểm Đà Nẵng
    { id: "US", name: "Hoa Kỳ", lat: 39.8283, lng: -98.5795, gdp: 25460 },
    { id: "JP", name: "Nhật Bản", lat: 36.2048, lng: 138.2529, gdp: 4230 },
    { id: "SG", name: "Singapore", lat: 1.3521, lng: 103.8198, gdp: 466 },
    { id: "KR", name: "Hàn Quốc", lat: 35.9078, lng: 127.7669, gdp: 1665 },
    { id: "CN", name: "Trung Quốc", lat: 35.8617, lng: 104.1954, gdp: 17963 }
];

// ==========================================
// 3. THUẬT TOÁN GIẢ LẬP DÒNG TIỀN (MOCK DATA)
// ==========================================
function generateRandomFlows() {
    const flows = [];
    const types = ["FDI", "TRADE"]; // FDI: Xanh nhạt, TRADE: Cam
    
    // Mỗi nhịp sẽ có 2 đến 3 luồng tiền bay cùng lúc
    const numFlows = Math.floor(Math.random() * 2) + 2; 
    
    for (let i = 0; i < numFlows; i++) {
        // Lấy ngẫu nhiên 1 nước làm nguồn phát
        const sources = ["US", "JP", "SG", "KR", "CN"];
        const sourceId = sources[Math.floor(Math.random() * sources.length)];
        
        // Mặc định cho mọi đường tiền đáp xuống Việt Nam để demo cho đẹp
        const targetId = "VN"; 
        
        flows.push({
            id: `flow_${Date.now()}_${i}`,
            sourceId: sourceId,
            targetId: targetId,
            // Số tiền ngẫu nhiên từ 0.5 đến 10.5 Tỷ USD
            amount: parseFloat((Math.random() * 10 + 0.5).toFixed(1)), 
            type: types[Math.floor(Math.random() * types.length)]
        });
    }
    return flows;
}

// ==========================================
// 4. KHỞI ĐỘNG HỆ THỐNG TRUYỀN DỮ LIỆU SOCKET
// ==========================================
io.on('connection', (socket) => {
    console.log(`⚡ Có thiết bị đang kết nối App: ${socket.id}`);
    
    // Ngay khi App mở lên, lập tức bắn data luôn không cần chờ
    socket.emit("cashflow_update", JSON.stringify({
        countries: countries,
        flows: generateRandomFlows()
    }));

    socket.on('disconnect', () => {
        console.log(`❌ Thiết bị đã thoát: ${socket.id}`);
    });
});

// NHỊP ĐẬP TRÁI TIM: Cứ 4 giây lại tạo dòng tiền mới và bơm xuống tất cả App
setInterval(() => {
    const payload = {
        countries: countries,
        flows: generateRandomFlows()
    };
    // Dùng io.emit để tất cả máy đang mở App đều nhìn thấy dòng tiền giống hệt nhau
    io.emit("cashflow_update", JSON.stringify(payload));
}, 4000);

// Bật Server
const PORT = process.env.PORT || 10000;
server.listen(PORT, () => {
    console.log(`🚀 Server đang chạy ở cổng ${PORT}`);
});