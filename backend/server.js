const express = require('express');
const mongoose = require('mongoose');
const xlsx = require('xlsx'); // <--- DÙNG THƯ VIỆN EXCEL XỊN SÒ
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// 1. KẾT NỐI MONGODB
const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/globalcashflow?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI).then(() => {
    console.log("✅ Đã kết nối MongoDB!");
    seedDataFromExcel(); // Gọi hàm mới
}).catch(err => console.error("❌ Lỗi kết nối MongoDB:", err));

// 2. SCHEMA
const countryTimelineSchema = new mongoose.Schema({
    countryId: String,
    countryName: String,
    metrics: [
        {
            name: String,
            unit: { type: String, default: "Tỷ USD" },
            history: [ { year: Number, value: Number } ]
        }
    ]
});
const CountryTimeline = mongoose.model('CountryTimeline', countryTimelineSchema);

// 3. HÀM QUÉT THẲNG FILE EXCEL (.XLSX)
async function seedDataFromExcel() {
    try {
        console.log("🧹 Đang dọn dẹp kho dữ liệu cũ...");
        await CountryTimeline.deleteMany({}); // Xóa sạch data rác cũ

        console.log("⏳ Đang đọc trực tiếp file Excel...");
        const results = [];
        const toBillion = (val) => val ? parseFloat((Number(val) / 1000000000).toFixed(2)) : 0;

        // Đọc thẳng file Excel
        const workbook = xlsx.readFile('Global_Cashflow_2020_2024.xlsx');
        const sheetName = workbook.SheetNames[0]; // Lấy sheet đầu tiên
        const data = xlsx.utils.sheet_to_json(workbook.Sheets[sheetName]); // Tự động biến thành JSON mượt mà

        data.forEach((row) => {
            // Bao lô luôn cả trường hợp cột có dấu cách hoặc không có dấu cách
            results.push({
                countryId: row['CountryCode'] || row['Country Code'],
                countryName: row['CountryName'] || row['Country Name'],
                metrics: [
                    {
                        name: "GDP",
                        history: [
                            { year: 2020, value: toBillion(row['GDP20']) }, { year: 2021, value: toBillion(row['GDP21']) },
                            { year: 2022, value: toBillion(row['GDP22']) }, { year: 2023, value: toBillion(row['GDP23']) },
                            { year: 2024, value: toBillion(row['GDP24']) }
                        ]
                    },
                    {
                        name: "FDI Inflows",
                        history: [
                            { year: 2020, value: toBillion(row['FDI_In20']) }, { year: 2021, value: toBillion(row['FDI_In21']) },
                            { year: 2022, value: toBillion(row['FDI_In22']) }, { year: 2023, value: toBillion(row['FDI_In23']) },
                            { year: 2024, value: toBillion(row['FDI_In24']) }
                        ]
                    },
                    {
                        name: "FDI Outflows",
                        history: [
                            { year: 2020, value: toBillion(row['FDI_Out20']) }, { year: 2021, value: toBillion(row['FDI_Out21']) },
                            { year: 2022, value: toBillion(row['FDI_Out22']) }, { year: 2023, value: toBillion(row['FDI_Out23']) },
                            { year: 2024, value: toBillion(row['FDI_Out24']) }
                        ]
                    },
                    {
                        name: "Exports",
                        history: [
                            { year: 2020, value: toBillion(row['Export20']) }, { year: 2021, value: toBillion(row['Export21']) },
                            { year: 2022, value: toBillion(row['Export22']) }, { year: 2023, value: toBillion(row['Export23']) },
                            { year: 2024, value: toBillion(row['Export24']) }
                        ]
                    },
                    {
                        name: "Imports",
                        history: [
                            { year: 2020, value: toBillion(row['Import20']) }, { year: 2021, value: toBillion(row['Import21']) },
                            { year: 2022, value: toBillion(row['Import22']) }, { year: 2023, value: toBillion(row['Import23']) },
                            { year: 2024, value: toBillion(row['Import24']) }
                        ]
                    }
                ]
            });
        });

        await CountryTimeline.insertMany(results);
        console.log(`✅ Tuyệt vời! Đã nạp thành công ${results.length} quốc gia từ Excel lên MongoDB!`);
    } catch (err) {
        console.error("❌ Lỗi khi import data:", err);
    }
}

// 4. API TỔNG: HỐT TRỌN 36 NƯỚC 
app.get('/api/analytics', async (req, res) => {
    try {
        const allData = await CountryTimeline.find({});
        res.status(200).json({ success: true, data: allData });
    } catch (err) {
        res.status(500).json({ success: false, message: "Lỗi Server" });
    }
});

// 5. API ĐƠN: LẤY 1 NƯỚC 
app.get('/api/analytics/:countryCode', async (req, res) => {
    try {
        const countryId = req.params.countryCode.toUpperCase();
        const data = await CountryTimeline.findOne({ countryId: countryId });
        
        if (!data) return res.status(404).json({ success: false, message: "Không tìm thấy dữ liệu nước này" });
        
        res.status(200).json({ success: true, data: data });
    } catch (err) {
        res.status(500).json({ success: false, message: "Lỗi Server" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`🚀 Server đang chạy ngon lành ở Cổng ${PORT}!`));