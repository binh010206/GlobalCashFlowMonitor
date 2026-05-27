require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

// =========================================
// 1. KẾT NỐI MONGODB (FREE TIER BẤT TỬ)
// =========================================
const MONGO_URI = process.env.MONGO_URI || "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI)
  .then(() => {
      console.log("🔥 Kết nối MongoDB thành công!");
      seedRealData(); 
  })
  .catch(err => console.log("❌ Lỗi MongoDB:", err));

// =========================================
// 2. CẤU TRÚC DATABASE (SCHEMAS)
// =========================================
const countrySchema = new mongoose.Schema({ 
    id: String, name: String, lat: Number, lng: Number,
    gdp: Number, fdi: Number, tradeBalance: Number, reserves: Number, debt: Number 
});
const Country = mongoose.model('Country', countrySchema);

const flowSchema = new mongoose.Schema({ sourceId: String, targetId: String, amount: Number, type: String });
const Flow = mongoose.model('Flow', flowSchema);

const alertSchema = new mongoose.Schema({ 
    message: String, 
    amount: Number, 
    type: String, 
    timestamp: { type: Date, default: Date.now } 
});
const Alert = mongoose.model('Alert', alertSchema);

const chatHistorySchema = new mongoose.Schema({
    sessionId: String,     
    userMessage: String,   
    aiResponse: String,    
    timestamp: { type: Date, default: Date.now }
});
const ChatHistory = mongoose.model('ChatHistory', chatHistorySchema);

// 🌟 ĐÃ THÊM: BẢNG LƯU TÀI KHOẢN NGƯỜI DÙNG (USER)
const userSchema = new mongoose.Schema({
    fullName: String,
    email: { type: String, unique: true },
    password: String, // Đồ án nên lưu plain-text để dễ test demo
    pinnedCountry: { type: String, default: "VN" } // Nước ghim mặc định
});
const User = mongoose.model('User', userSchema);


// =========================================
// 3. ĐĂNG KÝ API RESTFUL
// =========================================
app.use('/api/chat', require('./routes/chatRoute'));

// API: Kéo 10 Cảnh báo mới nhất
app.get('/api/alerts', async (req, res) => {
    try {
        const alerts = await Alert.find().sort({ timestamp: -1 }).limit(10);
        res.status(200).json({ success: true, data: alerts });
    } catch (error) {
        res.status(500).json({ success: false, message: "Lỗi kéo Data" });
    }
});

// 🌟 ĐÃ THÊM: API ĐĂNG KÝ TÀI KHOẢN
app.post('/api/auth/register', async (req, res) => {
    try {
        const { fullName, email, password } = req.body;
        const existing = await User.findOne({ email });
        if (existing) return res.status(400).json({ success: false, message: "Email đã tồn tại!" });
        
        await User.create({ fullName, email, password });
        res.status(200).json({ success: true, message: "Đăng ký thành công!" });
    } catch (error) { 
        res.status(500).json({ success: false, message: "Lỗi Server" }); 
    }
});

// 🌟 ĐÃ THÊM: API ĐĂNG NHẬP TÀI KHOẢN
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        const user = await User.findOne({ email, password });
        if (!user) return res.status(401).json({ success: false, message: "Sai email hoặc mật khẩu!" });
        
        res.status(200).json({ success: true, data: { email: user.email, name: user.fullName } });
    } catch (error) { 
        res.status(500).json({ success: false, message: "Lỗi Server" }); 
    }
});


// =========================================
// 4. ĐỘNG CƠ SOCKET.IO REALTIME (CHILL MODE - 6 GIÂY)
// =========================================
io.on('connection', (socket) => {
    console.log("⚡ Radar kết nối: " + socket.id);
    
    const dataPump = setInterval(async () => {
        try {
            const countries = await Country.find();
            if (countries.length < 2) return;

            const heavyWeights = ["US", "CN", "DE", "JP", "GB", "FR", "IN"];
            const flowTypes = ["FDI", "TRADE", "REMITTANCE"];
            
            const flowsToEmit = [];
            const numFlows = Math.floor(Math.random() * 2) + 2; 

            for (let i = 0; i < numFlows; i++) {
                let source = countries[Math.floor(Math.random() * countries.length)];
                let target = countries[Math.floor(Math.random() * countries.length)];
                while (source.id === target.id) {
                    target = countries[Math.floor(Math.random() * countries.length)];
                }

                let baseAmount = Math.random() * 5 + 0.5; 
                if (heavyWeights.includes(source.id)) baseAmount *= 4.5; 
                if (heavyWeights.includes(target.id)) baseAmount *= 2.5;
                if (source.id === "VN" || target.id === "VN") baseAmount *= 2; 

                const finalAmount = parseFloat(baseAmount.toFixed(2));
                const flowType = flowTypes[Math.floor(Math.random() * flowTypes.length)];

                if (finalAmount >= 15) {
                    const msg = `CÁ MẬP: Dòng vốn ${finalAmount} tỷ USD (${flowType}) vừa di chuyển từ ${source.id} sang ${target.id}.`;
                    await Alert.create({ message: msg, amount: finalAmount, type: "SHARK" });

                    const totalAlerts = await Alert.countDocuments();
                    if (totalAlerts > 50) {
                        await Alert.findOneAndDelete({}, { sort: { timestamp: 1 } });
                    }
                }

                flowsToEmit.push({
                    id: `flow_${Date.now()}_${i}`,
                    sourceId: source.id,
                    targetId: target.id,
                    amount: finalAmount,
                    type: flowType
                });
            }

            const countriesWithStats = countries.map(c => {
                let obj = c.toObject();
                const fluctuation = () => 1 + (Math.random() * 0.001 - 0.0005); 
                obj.gdp = parseFloat(((obj.gdp || 100) * fluctuation()).toFixed(2));
                obj.fdi = parseFloat(((obj.fdi || 10) * fluctuation()).toFixed(2));
                obj.tradeBalance = parseFloat(((obj.tradeBalance || 5) * fluctuation()).toFixed(2));
                obj.reserves = parseFloat(((obj.reserves || 20) * fluctuation()).toFixed(2));
                obj.debt = parseFloat(((obj.debt || 50) * fluctuation()).toFixed(2));
                return obj;
            });

            socket.emit("cashflow_update", {
                countries: countriesWithStats,
                flows: flowsToEmit
            });

        } catch (error) { 
            console.error("Lỗi Socket:", error); 
        }
    }, 6000); 

    socket.on('disconnect', () => {
        clearInterval(dataPump);
        console.log("❌ Radar ngắt kết nối");
    });
});

// =========================================
// 5. NẠP TỌA ĐỘ VÀ SỐ LIỆU VĨ MÔ
// =========================================
async function seedRealData() {
    try {
        const count = await Country.countDocuments();
        let needsUpdate = false;

        if (count > 0) {
            const vn = await Country.findOne({ id: "VN" });
            if (!vn || vn.gdp === undefined || vn.gdp === null) {
                needsUpdate = true;
            }
        } else {
            needsUpdate = true;
        }

        if (needsUpdate) {
            console.log("⚠️ Phát hiện Database cũ hoặc trống! Đang xóa sạch để nạp Data mới...");
            await Country.deleteMany({});
            
            const topCountries = [
                { id: "US", name: "Hoa Kỳ", lat: 37.0902, lng: -95.7129, gdp: 27360, fdi: 388, tradeBalance: -1060, reserves: 242, debt: 34000 },
                { id: "CN", name: "Trung Quốc", lat: 35.8617, lng: 104.1954, gdp: 17700, fdi: 163, tradeBalance: 823, reserves: 3225, debt: 14000 },
                { id: "VN", name: "Việt Nam", lat: 14.0583, lng: 108.2772, gdp: 430, fdi: 36.6, tradeBalance: 28, reserves: 88, debt: 135 },
                { id: "JP", name: "Nhật Bản", lat: 36.2048, lng: 138.2529, gdp: 4212, fdi: 32, tradeBalance: -60, reserves: 1290, debt: 10400 },
                { id: "DE", name: "Đức", lat: 51.1657, lng: 10.4515, gdp: 4456, fdi: 40, tradeBalance: 250, reserves: 310, debt: 3200 },
                { id: "IN", name: "Ấn Độ", lat: 20.5937, lng: 78.9629, gdp: 3730, fdi: 49, tradeBalance: -250, reserves: 600, debt: 2100 },
                { id: "SG", name: "Singapore", lat: 1.3521, lng: 103.8198, gdp: 501, fdi: 141, tradeBalance: 154, reserves: 345, debt: 650 },
                { id: "GB", name: "Vương quốc Anh", lat: 55.3781, lng: -3.4360, gdp: 3332, fdi: 14, tradeBalance: -210, reserves: 180, debt: 3100 },
                { id: "FR", name: "Pháp", lat: 46.2276, lng: 2.2137, gdp: 3050, fdi: 34, tradeBalance: -100, reserves: 240, debt: 3300 },
                { id: "KR", name: "Hàn Quốc", lat: 35.9078, lng: 127.7669, gdp: 1712, fdi: 18, tradeBalance: 15, reserves: 415, debt: 950 },
                { id: "BR", name: "Brazil", lat: -14.2350, lng: -51.9253, gdp: 2120, fdi: 65, tradeBalance: 98, reserves: 350, debt: 1600 },
                { id: "TH", name: "Thái Lan", lat: 15.8700, lng: 100.9925, gdp: 514, fdi: 10, tradeBalance: 12, reserves: 220, debt: 310 },
                { id: "ID", name: "Indonesia", lat: -0.7893, lng: 113.9213, gdp: 1370, fdi: 22, tradeBalance: 36, reserves: 145, debt: 520 },
                { id: "MY", name: "Malaysia", lat: 4.2105, lng: 101.9758, gdp: 430, fdi: 17, tradeBalance: 45, reserves: 115, debt: 250 },
                { id: "AU", name: "Úc", lat: -25.2744, lng: 133.7751, gdp: 1680, fdi: 61, tradeBalance: 70, reserves: 55, debt: 900 },
                { id: "PH", name: "Philippines", lat: 12.8797, lng: 121.7740, gdp: 436, fdi: 9, tradeBalance: -15, reserves: 98, debt: 115 },
                { id: "AE", name: "UAE", lat: 23.4241, lng: 53.8478, gdp: 509, fdi: 22, tradeBalance: 80, reserves: 115, debt: 150 },
                { id: "SA", name: "Ả Rập Xê Út", lat: 23.8859, lng: 45.0792, gdp: 1060, fdi: 12, tradeBalance: 120, reserves: 450, debt: 250 },
                { id: "IL", name: "Israel", lat: 31.0461, lng: 34.8516, gdp: 522, fdi: 21, tradeBalance: -5, reserves: 200, debt: 145 },
                { id: "TR", name: "Thổ Nhĩ Kỳ", lat: 38.9637, lng: 35.2433, gdp: 1150, fdi: 13, tradeBalance: -45, reserves: 85, debt: 450 },
                { id: "IT", name: "Ý", lat: 41.8719, lng: 12.5674, gdp: 2250, fdi: 19, tradeBalance: 30, reserves: 170, debt: 2800 },
                { id: "NL", name: "Hà Lan", lat: 52.1326, lng: 5.2913, gdp: 1110, fdi: 45, tradeBalance: 75, reserves: 40, debt: 520 },
                { id: "CH", name: "Thụy Sĩ", lat: 46.8182, lng: 8.2275, gdp: 880, fdi: -15, tradeBalance: 40, reserves: 920, debt: 210 },
                { id: "ES", name: "Tây Ban Nha", lat: 40.4637, lng: -3.7492, gdp: 1580, fdi: 35, tradeBalance: 10, reserves: 85, debt: 1600 },
                { id: "SE", name: "Thụy Điển", lat: 60.1282, lng: 18.6435, gdp: 590, fdi: 24, tradeBalance: 15, reserves: 55, debt: 210 },
                { id: "NO", name: "Na Uy", lat: 60.4720, lng: 8.4689, gdp: 485, fdi: 11, tradeBalance: 105, reserves: 85, debt: 150 },
                { id: "FI", name: "Phần Lan", lat: 61.9241, lng: 25.7482, gdp: 300, fdi: 8, tradeBalance: 2, reserves: 45, debt: 180 },
                { id: "DK", name: "Đan Mạch", lat: 56.2639, lng: 9.5018, gdp: 410, fdi: 10, tradeBalance: 35, reserves: 70, debt: 120 },
                { id: "IE", name: "Ireland", lat: 53.1424, lng: -7.6921, gdp: 545, fdi: 85, tradeBalance: 80, reserves: 12, debt: 250 },
                { id: "CA", name: "Canada", lat: 56.1304, lng: -106.3468, gdp: 2140, fdi: 54, tradeBalance: 15, reserves: 110, debt: 2200 },
                { id: "MX", name: "Mexico", lat: 23.6345, lng: -102.5528, gdp: 1780, fdi: 35, tradeBalance: -25, reserves: 205, debt: 850 },
                { id: "AR", name: "Argentina", lat: -38.4161, lng: -63.6167, gdp: 630, fdi: 6, tradeBalance: 15, reserves: 25, debt: 380 },
                { id: "CL", name: "Chile", lat: -35.6751, lng: -71.5430, gdp: 335, fdi: 12, tradeBalance: 8, reserves: 40, debt: 125 },
                { id: "CO", name: "Colombia", lat: 4.5709, lng: -74.2973, gdp: 363, fdi: 17, tradeBalance: -10, reserves: 55, debt: 180 },
                { id: "PE", name: "Peru", lat: -9.1900, lng: -75.0152, gdp: 264, fdi: 8, tradeBalance: 12, reserves: 75, debt: 95 },
                { id: "NZ", name: "New Zealand", lat: -40.9006, lng: 174.8860, gdp: 250, fdi: 4, tradeBalance: -5, reserves: 15, debt: 120 },
                { id: "ZA", name: "Nam Phi", lat: -30.5595, lng: 22.9375, gdp: 377, fdi: 5, tradeBalance: 5, reserves: 60, debt: 260 },
                { id: "EG", name: "Ai Cập", lat: 26.8206, lng: 30.8025, gdp: 398, fdi: 9, tradeBalance: -30, reserves: 35, debt: 160 },
                { id: "NG", name: "Nigeria", lat: 9.0820, lng: 8.6753, gdp: 390, fdi: 2, tradeBalance: 5, reserves: 35, debt: 120 },
                { id: "RU", name: "Nga", lat: 61.5240, lng: 105.3188, gdp: 1997, fdi: -15, tradeBalance: 120, reserves: 590, debt: 350 }
            ];
            await Country.insertMany(topCountries);
            console.log("✅ Đã nạp xong bản đồ 40 quốc gia + Dữ liệu thống kê!");
        } else {
            console.log("✅ Dữ liệu DB đã chuẩn, không cần nạp lại.");
        }
    } catch (error) {
        console.error("❌ Lỗi khi nạp data:", error);
    }
}

const PORT = process.env.PORT || 5000;
server.listen(PORT, () => {
    console.log(`🚀 Backend Global Cash Flow đang chạy tại port ${PORT}`);
});