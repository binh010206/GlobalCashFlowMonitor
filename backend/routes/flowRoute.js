const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

// 1. API: Lấy danh sách dòng vốn FDI (Có xếp hạng giảm dần)
router.get('/fdi', async (req, res) => {
    try {
        const Flow = mongoose.model('Flow');
        const { year } = req.query;
        const filter = { type: 'FDI' };
        if (year) filter.period = year;

        const fdiData = await Flow.find(filter).sort({ amount: -1 });
        res.status(200).json({ success: true, data: fdiData });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Lỗi truy xuất FDI', error });
    }
});

// 2. API: Lấy dữ liệu Thương mại (TRADE)
router.get('/trade', async (req, res) => {
    try {
        const Flow = mongoose.model('Flow');
        const tradeData = await Flow.find({ type: 'TRADE' }).sort({ amount: -1 });
        res.status(200).json({ success: true, data: tradeData });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Lỗi truy xuất TRADE', error });
    }
});

// 3. API: Tổng hợp dữ liệu bằng hàm Aggregate (Vẽ biểu đồ tròn)
router.get('/summary', async (req, res) => {
    try {
        const Flow = mongoose.model('Flow');
        const summary = await Flow.aggregate([
            { $group: { _id: "$type", totalAmount: { $sum: "$amount" }, count: { $sum: 1 } } }
        ]);
        res.status(200).json({ success: true, data: summary });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Lỗi tổng hợp dữ liệu', error });
    }
});

module.exports = router;