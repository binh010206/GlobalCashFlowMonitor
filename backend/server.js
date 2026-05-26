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
// 2. KHO DỮ LIỆU 40 QUỐC GIA 
// ==========================================
const countries = [
    { id: "VN", name: "Việt Nam", lat: 16.0544, lng: 108.2022, flagUrl: "https://flagcdn.com/w80/vn.png" }, // Tâm điểm Đà Nẵng
    { id: "US", name: "Hoa Kỳ", lat: 39.82, lng: -98.57, flagUrl: "https://flagcdn.com/w80/us.png" },
    { id: "CN", name: "Trung Quốc", lat: 35.86, lng: 104.19, flagUrl: "https://flagcdn.com/w80/cn.png" },
    { id: "JP", name: "Nhật Bản", lat: 36.20, lng: 138.25, flagUrl: "https://flagcdn.com/w80/jp.png" },
    { id: "KR", name: "Hàn Quốc", lat: 35.90, lng: 127.76, flagUrl: "https://flagcdn.com/w80/kr.png" },
    { id: "SG", name: "Singapore", lat: 1.35, lng: 103.81, flagUrl: "https://flagcdn.com/w80/sg.png" },
    { id: "DE", name: "Đức", lat: 51.16, lng: 10.45, flagUrl: "https://flagcdn.com/w80/de.png" },
    { id: "GB", name: "Anh", lat: 55.37, lng: -3.43, flagUrl: "https://flagcdn.com/w80/gb.png" },
    { id: "FR", name: "Pháp", lat: 46.22, lng: 2.21, flagUrl: "https://flagcdn.com/w80/fr.png" },
    { id: "IN", name: "Ấn Độ", lat: 20.59, lng: 78.96, flagUrl: "https://flagcdn.com/w80/in.png" },
    { id: "RU", name: "Nga", lat: 61.52, lng: 105.31, flagUrl: "https://flagcdn.com/w80/ru.png" },
    { id: "CA", name: "Canada", lat: 56.13, lng: -106.34, flagUrl: "https://flagcdn.com/w80/ca.png" },
    { id: "AU", name: "Úc", lat: -25.27, lng: 133.77, flagUrl: "https://flagcdn.com/w80/au.png" },
    { id: "BR", name: "Brazil", lat: -14.23, lng: -51.92, flagUrl: "https://flagcdn.com/w80/br.png" },
    { id: "MX", name: "Mexico", lat: 23.63, lng: -102.55, flagUrl: "https://flagcdn.com/w80/mx.png" },
    { id: "ID", name: "Indonesia", lat: -0.78, lng: 113.92, flagUrl: "https://flagcdn.com/w80/id.png" },
    { id: "MY", name: "Malaysia", lat: 4.21, lng: 101.97, flagUrl: "https://flagcdn.com/w80/my.png" },
    { id: "TH", name: "Thái Lan", lat: 15.87, lng: 100.99, flagUrl: "https://flagcdn.com/w80/th.png" },
    { id: "PH", name: "Philippines", lat: 12.87, lng: 121.77, flagUrl: "https://flagcdn.com/w80/ph.png" },
    { id: "NL", name: "Hà Lan", lat: 52.13, lng: 5.29, flagUrl: "https://flagcdn.com/w80/nl.png" },
    { id: "CH", name: "Thụy Sĩ", lat: 46.81, lng: 8.22, flagUrl: "https://flagcdn.com/w80/ch.png" },
    { id: "SA", name: "Ả Rập Xê Út", lat: 23.88, lng: 45.07, flagUrl: "https://flagcdn.com/w80/sa.png" },
    { id: "AE", name: "UAE", lat: 23.42, lng: 53.84, flagUrl: "https://flagcdn.com/w80/ae.png" },
    { id: "ZA", name: "Nam Phi", lat: -30.55, lng: 22.93, flagUrl: "https://flagcdn.com/w80/za.png" },
    { id: "TR", name: "Thổ Nhĩ Kỳ", lat: 38.96, lng: 35.24, flagUrl: "https://flagcdn.com/w80/tr.png" },
    { id: "ES", name: "Tây Ban Nha", lat: 40.46, lng: -3.74, flagUrl: "https://flagcdn.com/w80/es.png" },
    { id: "TW", name: "Đài Loan", lat: 23.69, lng: 120.96, flagUrl: "https://flagcdn.com/w80/tw.png" },
    { id: "HK", name: "Hong Kong", lat: 22.31, lng: 114.16, flagUrl: "https://flagcdn.com/w80/hk.png" },
    { id: "SE", name: "Thụy Điển", lat: 60.12, lng: 18.64, flagUrl: "https://flagcdn.com/w80/se.png" },
    { id: "NO", name: "Na Uy", lat: 60.47, lng: 8.46, flagUrl: "https://flagcdn.com/w80/no.png" },
    { id: "DK", name: "Đan Mạch", lat: 56.26, lng: 9.50, flagUrl: "https://flagcdn.com/w80/dk.png" },
    { id: "FI", name: "Phần Lan", lat: 61.92, lng: 25.74, flagUrl: "https://flagcdn.com/w80/fi.png" },
    { id: "NZ", name: "New Zealand", lat: -40.90, lng: 174.88, flagUrl: "https://flagcdn.com/w80/nz.png" },
    { id: "IL", name: "Israel", lat: 31.04, lng: 34.85, flagUrl: "https://flagcdn.com/w80/il.png" },
    { id: "IE", name: "Ireland", lat: 53.14, lng: -7.69, flagUrl: "https://flagcdn.com/w80/ie.png" },
    { id: "PL", name: "Ba Lan", lat: 51.91, lng: 19.14, flagUrl: "https://flagcdn.com/w80/pl.png" },
    { id: "CL", name: "Chile", lat: -35.67, lng: -71.54, flagUrl: "https://flagcdn.com/w80/cl.png" },
    { id: "AR", name: "Argentina", lat: -38.41, lng: -63.61, flagUrl: "https://flagcdn.com/w80/ar.png" },
    { id: "IT", name: "Ý", lat: 41.87, lng: 12.56, flagUrl: "https://flagcdn.com/w80/it.png" },
    { id: "BE", name: "Bỉ", lat: 50.50, lng: 4.46, flagUrl: "https://flagcdn.com/w80/be.png" }
];

// ==========================================
// 3. THUẬT TOÁN GIẢ LẬP DÒNG TIỀN (Bắn từ 39 nước về VN)
// ==========================================
function generateRandomFlows() {
    const flows = [];
    const types = ["FDI", "TRADE", "REMITTANCE"]; // Thêm Kiều hối (Màu tím)
    
    if (Math.random() < 0.3) return flows; // 30% thời gian màn hình tĩnh lặng
    
    const numFlows = Math.floor(Math.random() * 3) + 1; // Nổ từ 1-3 thương vụ cùng lúc
    
    // Tự động lấy danh sách 39 nước (Bỏ Việt Nam ra khỏi danh sách nguồn)
    const sources = countries.map(c => c.id).filter(id => id !== "VN");
    
    for (let i = 0; i < numFlows; i++) {
        const sourceId = sources[Math.floor(Math.random() * sources.length)];
        const targetId = "VN"; 
        
        let amountBase = Math.pow(Math.random(), 3); 
        let finalAmount = (amountBase * 5.4) + 0.1; 

        flows.push({
            id: `flow_${Date.now()}_${i}`,
            sourceId: sourceId,
            targetId: targetId,
            amount: parseFloat(finalAmount.toFixed(2)),
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