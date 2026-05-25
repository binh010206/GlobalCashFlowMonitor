const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const axios = require('axios');
const mongoose = require('mongoose');
const Chat = require('./ChatSchema'); // Đảm bảo đã tạo file ChatSchema.js

const app = express();
app.use(cors());
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*", methods: ["GET", "POST"] } });

// 1. KẾT NỐI MONGODB (Chỗ này mày thay link của mày vào nhé)
const uri = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
mongoose.connect(uri)
    .then(() => console.log("✅ Kết nối MongoDB thành công!"))
    .catch(err => console.error("❌ Lỗi kết nối MongoDB:", err));

const countryCodes = ['VN', 'US', 'CN', 'RU', 'BR', 'AU', 'IN', 'DE', 'JP', 'CA'];
let globalMacroData = [];
const coords = { 
    'VN': { lat: 16.05, lng: 108.20 }, 'US': { lat: 37.09, lng: -95.71 }, 
    'CN': { lat: 35.86, lng: 104.19 }, 'RU': { lat: 61.52, lng: 105.31 }, 
    'BR': { lat: -14.23, lng: -51.92 }, 'AU': { lat: -25.27, lng: 133.77 }, 
    'IN': { lat: 20.59, lng: 78.96 }, 'DE': { lat: 51.16, lng: 10.45 }, 
    'JP': { lat: 36.20, lng: 138.25 }, 'CA': { lat: 56.13, lng: -106.34 } 
};

// Hàm lấy dữ liệu từ World Bank
async function fetchRealBaseData() {
    try {
        const codesString = countryCodes.join(';');
        const response = await axios.get(`http://api.worldbank.org/v2/country/${codesString}/indicator/NY.GDP.MKTP.CD?format=json&per_page=100`);
        const apiData = response.data[1]; 
        globalMacroData = countryCodes.map(code => {
            const countryInfo = apiData.find(item => item.countryiso3code.substring(0,2) === code || item.country.id === code);
            const realGdp = countryInfo && countryInfo.value ? parseFloat((countryInfo.value / 1e9).toFixed(1)) : Math.random() * 5000 + 500;
            return { id: code, name: countryInfo ? countryInfo.country.value : code, lat: coords[code].lat, lng: coords[code].lng, gdp: realGdp };
        });
    } catch (error) { console.error("❌ Lỗi API World Bank"); }
}

// 2. XỬ LÝ SOCKET VÀ LƯU CHAT VÀO MONGODB
io.on('connection', (socket) => {
    console.log(`📱 Thiết bị kết nối: ${socket.id}`);

    // Khi có tin nhắn từ Android
    socket.on('send_message', async (data) => {
        // Lưu vào MongoDB
        const newMessage = new Chat({
            sender: data.sender,
            message: data.message,
            room: data.room
        });
        await newMessage.save();

        // Phát real-time cho các user khác
        io.to(data.room).emit('receive_message', data);
    });

    socket.on('join_room', (room) => { socket.join(room); });
});

// Chạy server
fetchRealBaseData().then(() => {
    const PORT = process.env.PORT || 3000;
    server.listen(PORT, () => { console.log(`🚀 Server đang chạy ở cổng ${PORT}`); });
});