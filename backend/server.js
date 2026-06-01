const express = require('express');
const mongoose = require('mongoose');
const fs = require('fs');
const csv = require('csv-parser');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// 1. KẾT NỐI MONGODB (Thay cái Link MongoDB của mày vào đây)
const MONGO_URI = "mongodb+srv://globalcashflowmonitor:global123%40@cluster0.xjhpeid.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

mongoose.connect(MONGO_URI).then(() => {
    console.log("✅ Đã kết nối MongoDB!");
    seedDataFromCSV(); // Gọi hàm nạp data khi bật server
}).catch(err => console.error("❌ Lỗi kết nối MongoDB:", err));

// 2. SCHEMA: Cấu trúc Dữ liệu trên Database
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

// 3. HÀM QUÉT CSV VÀ NẠP LÊN MONGODB
async function seedDataFromCSV() {
    try {
        const count = await CountryTimeline.countDocuments();
        if (count > 0) {
            console.log("⚡ Dữ liệu đã có sẵn trên MongoDB Cloud, không cần import lại.");
            return;
        }

        console.log("⏳ Đang phân tích file CSV và chuyển đổi số liệu...");
        const results = [];

        // Hàm quy đổi số thô ra Tỷ USD (VD: 433000000000 -> 433.00)
        const toBillion = (val) => val ? parseFloat((Number(val) / 1000000000).toFixed(2)) : 0;

        fs.createReadStream('Global_Cashflow_2020_2024.xlsx')
          .pipe(csv())
          .on('data', (row) => {
              results.push({
                  countryId: row.CountryCode,
                  countryName: row.CountryName,
                  metrics: [
                      {
                          name: "GDP",
                          history: [
                              // Đã sửa thành row.GDP theo file mới của mày
                              { year: 2020, value: toBillion(row.GDP20) }, { year: 2021, value: toBillion(row.GDP21) },
                              { year: 2022, value: toBillion(row.GDP22) }, { year: 2023, value: toBillion(row.GDP23) },
                              { year: 2024, value: toBillion(row.GDP24) }
                          ]
                      },
                      {
                          name: "FDI Inflows",
                          history: [
                              { year: 2020, value: toBillion(row.FDI_In20) }, { year: 2021, value: toBillion(row.FDI_In21) },
                              { year: 2022, value: toBillion(row.FDI_In22) }, { year: 2023, value: toBillion(row.FDI_In23) },
                              { year: 2024, value: toBillion(row.FDI_In24) }
                          ]
                      },
                      {
                          name: "FDI Outflows",
                          history: [
                              { year: 2020, value: toBillion(row.FDI_Out20) }, { year: 2021, value: toBillion(row.FDI_Out21) },
                              { year: 2022, value: toBillion(row.FDI_Out22) }, { year: 2023, value: toBillion(row.FDI_Out23) },
                              { year: 2024, value: toBillion(row.FDI_Out24) }
                          ]
                      },
                      {
                          name: "Exports",
                          history: [
                              { year: 2020, value: toBillion(row.Export20) }, { year: 2021, value: toBillion(row.Export21) },
                              { year: 2022, value: toBillion(row.Export22) }, { year: 2023, value: toBillion(row.Export23) },
                              { year: 2024, value: toBillion(row.Export24) }
                          ]
                      },
                      {
                          name: "Imports",
                          history: [
                              { year: 2020, value: toBillion(row.Import20) }, { year: 2021, value: toBillion(row.Import21) },
                              { year: 2022, value: toBillion(row.Import22) }, { year: 2023, value: toBillion(row.Import23) },
                              { year: 2024, value: toBillion(row.Import24) }
                          ]
                      }
                  ]
              });
          })
          .on('end', async () => {
              await CountryTimeline.insertMany(results);
              console.log(`✅ Thành công! Đã nạp xong ${results.length} quốc gia lên Data Warehouse!`);
          });
    } catch (err) {
        console.error("❌ Lỗi khi import data:", err);
    }
}

// 4. API ĐỂ FRONT-END APP KÉO DATA VỀ
app.get('/api/analytics/:countryCode', async (req, res) => {
    try {
        const countryId = req.params.countryCode.toUpperCase();
        const data = await CountryTimeline.findOne({ countryId: countryId });
        
        if (!data) {
            return res.status(404).json({ success: false, message: "Không tìm thấy dữ liệu nước này" });
        }
        
        res.status(200).json({ success: true, data: data });
    } catch (err) {
        res.status(500).json({ success: false, message: "Lỗi Server" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`🚀 Server đang chạy ngon lành ở Cổng ${PORT}!`));