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
// 1. KẾT NỐI MONGODB ATLAS
// =========================================
// (Giữ nguyên chuỗi kết nối chuẩn của mày)
const MONGO_URI = process.env.MONGO_URI || "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
  .then(() => {
      console.log("🔥 Kết nối MongoDB thành công!");
      seedRealData(); // Tự động nạp tọa độ 40 nước nếu DB trống
  })
  .catch(err => console.log("❌ Lỗi MongoDB:", err));

// =========================================
// 2. ĐỊNH NGHĨA CÁC SCHEMA (DATABASE)
// =========================================
const countrySchema = new mongoose.Schema({ id: String, name: String, lat: Number, lng: Number });
const Country = mongoose.model('Country', countrySchema);

// Mảng Flow Schema này để con AI đọc (File chatRoute.js)
const flowSchema = new mongoose.Schema({ sourceId: String, targetId: String, amount: Number, type: String });
const Flow = mongoose.model('Flow', flowSchema);

// =========================================
// 3. ĐĂNG KÝ ROUTER API (CHO CON AI)
// =========================================
// Nhúng file chatRoute.js lúc nãy anh em mình code để xử lý AI
app.use('/api/chat', require('./routes/chatRoute'));

// =========================================
// 4. ĐỘNG CƠ REALTIME SOCKET.IO (CHO MAPBOX)
// =========================================
io.on('connection', (socket) => {
    console.log("⚡ Radar Android vừa kết nối: " + socket.id);
    
    // Động cơ mô phỏng luân chuyển dòng tiền toàn cầu (Weighted Simulation Engine)
    const dataPump = setInterval(async () => {
        try {
            const countries = await Country.find();
            if (countries.length < 2) return;

            // Trọng số kinh tế: Các cường quốc này sẽ tạo ra / nhận nhiều dòng tiền hơn
            const heavyWeights = ["US", "CN", "DE", "JP", "GB", "FR", "IN"];
            const flowTypes = ["FDI", "TRADE", "REMITTANCE"];
            
            const flowsToEmit = [];
            const numFlows = Math.floor(Math.random() * 3) + 3; // Tạo 3 đến 5 luồng cùng lúc

            for (let i = 0; i < numFlows; i++) {
                // Rút ngẫu nhiên 2 quốc gia để kết nối
                let source = countries[Math.floor(Math.random() * countries.length)];
                let target = countries[Math.floor(Math.random() * countries.length)];
                while (source.id === target.id) {
                    target = countries[Math.floor(Math.random() * countries.length)];
                }

                // Tính toán số tiền dựa trên quy mô kinh tế
                let baseAmount = Math.random() * 5 + 0.5; // Mặc định từ 0.5 - 5.5 tỷ USD
                
                if (heavyWeights.includes(source.id)) baseAmount *= 4.5; 
                if (heavyWeights.includes(target.id)) baseAmount *= 2.5;
                if (source.id === "VN" || target.id === "VN") baseAmount *= 2; // Ưu tiên VN hiển thị sáng hơn

                flowsToEmit.push({
                    id: `flow_${Date.now()}_${i}`,
                    sourceId: source.id,
                    targetId: target.id,
                    amount: parseFloat(baseAmount.toFixed(2)),
                    type: flowTypes[Math.floor(Math.random() * flowTypes.length)]
                });
            }

            // Gửi dữ liệu xuống cho điện thoại vẽ đồ họa
            socket.emit("cashflow_update", {
                countries: countries,
                flows: flowsToEmit
            });

        } catch (error) { 
            console.error("Lỗi động cơ Socket:", error); 
        }
    }, 2500); // 2.5 giây làm mới một lần

    socket.on('disconnect', () => {
        clearInterval(dataPump);
        console.log("❌ Radar ngắt kết nối");
    });
});

// =========================================
// 5. HÀM NẠP TỌA ĐỘ VĨ MÔ 40 QUỐC GIA (KHÔNG CHẾ DATA)
// =========================================
async function seedRealData() {
    const count = await Country.countDocuments();
    if (count === 0) {
        console.log("Đang nạp dữ liệu tọa độ địa lý 40 quốc gia trọng điểm...");
        const topCountries = [
            // Đông Nam Á & Châu Á
            { id: "VN", name: "Việt Nam", lat: 14.0583, lng: 108.2772 },
            { id: "SG", name: "Singapore", lat: 1.3521, lng: 103.8198 },
            { id: "TH", name: "Thái Lan", lat: 15.8700, lng: 100.9925 },
            { id: "ID", name: "Indonesia", lat: -0.7893, lng: 113.9213 },
            { id: "MY", name: "Malaysia", lat: 4.2105, lng: 101.9758 },
            { id: "PH", name: "Philippines", lat: 12.8797, lng: 121.7740 },
            { id: "CN", name: "Trung Quốc", lat: 35.8617, lng: 104.1954 },
            { id: "JP", name: "Nhật Bản", lat: 36.2048, lng: 138.2529 },
            { id: "KR", name: "Hàn Quốc", lat: 35.9078, lng: 127.7669 },
            { id: "IN", name: "Ấn Độ", lat: 20.5937, lng: 78.9629 },
            { id: "AE", name: "UAE", lat: 23.4241, lng: 53.8478 },
            { id: "SA", name: "Ả Rập Xê Út", lat: 23.8859, lng: 45.0792 },
            { id: "IL", name: "Israel", lat: 31.0461, lng: 34.8516 },
            { id: "TR", name: "Thổ Nhĩ Kỳ", lat: 38.9637, lng: 35.2433 },
            // Châu Âu
            { id: "GB", name: "Vương quốc Anh", lat: 55.3781, lng: -3.4360 },
            { id: "DE", name: "Đức", lat: 51.1657, lng: 10.4515 },
            { id: "FR", name: "Pháp", lat: 46.2276, lng: 2.2137 },
            { id: "IT", name: "Ý", lat: 41.8719, lng: 12.5674 },
            { id: "NL", name: "Hà Lan", lat: 52.1326, lng: 5.2913 },
            { id: "CH", name: "Thụy Sĩ", lat: 46.8182, lng: 8.2275 },
            { id: "ES", name: "Tây Ban Nha", lat: 40.4637, lng: -3.7492 },
            { id: "SE", name: "Thụy Điển", lat: 60.1282, lng: 18.6435 },
            { id: "NO", name: "Na Uy", lat: 60.4720, lng: 8.4689 },
            { id: "FI", name: "Phần Lan", lat: 61.9241, lng: 25.7482 },
            { id: "DK", name: "Đan Mạch", lat: 56.2639, lng: 9.5018 },
            { id: "IE", name: "Ireland", lat: 53.1424, lng: -7.6921 },
            // Châu Mỹ
            { id: "US", name: "Hoa Kỳ", lat: 37.0902, lng: -95.7129 },
            { id: "CA", name: "Canada", lat: 56.1304, lng: -106.3468 },
            { id: "MX", name: "Mexico", lat: 23.6345, lng: -102.5528 },
            { id: "BR", name: "Brazil", lat: -14.2350, lng: -51.9253 },
            { id: "AR", name: "Argentina", lat: -38.4161, lng: -63.6167 },
            { id: "CL", name: "Chile", lat: -35.6751, lng: -71.5430 },
            { id: "CO", name: "Colombia", lat: 4.5709, lng: -74.2973 },
            { id: "PE", name: "Peru", lat: -9.1900, lng: -75.0152 },
            // Châu Phi & Châu Đại Dương & Nga
            { id: "AU", name: "Úc", lat: -25.2744, lng: 133.7751 },
            { id: "NZ", name: "New Zealand", lat: -40.9006, lng: 174.8860 },
            { id: "ZA", name: "Nam Phi", lat: -30.5595, lng: 22.9375 },
            { id: "EG", name: "Ai Cập", lat: 26.8206, lng: 30.8025 },
            { id: "NG", name: "Nigeria", lat: 9.0820, lng: 8.6753 },
            { id: "RU", name: "Nga", lat: 61.5240, lng: 105.3188 }
        ];
        await Country.insertMany(topCountries);
        console.log("✅ Đã nạp xong bản đồ 40 quốc gia!");
    }
}

// =========================================
// 6. KHỞI ĐỘNG SERVER
// =========================================
const PORT = process.env.PORT || 5000;
server.listen(PORT, () => {
    console.log(`🚀 Hệ thống Backend Global Cash Flow đang chạy tại port ${PORT}`);
});