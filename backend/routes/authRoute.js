const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

// 1. Định nghĩa cấu trúc bảng lưu trữ người dùng
const userSchema = new mongoose.Schema({
    fullName: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true } // Sau này nên dùng thư viện bcrypt để mã hóa mật khẩu bảo mật hơn
});

const User = mongoose.models.User || mongoose.model('User', userSchema);

// 2. API xử lý Đăng ký dữ liệux
router.post('/register', async (req, res) => {
    try {
        const { fullName, email, password } = req.body;

        // Kiểm tra tài khoản trùng lặp
        const existingUser = await User.findOne({ email: email.trim() });
        if (existingUser) {
            return res.status(200).json({ success: false, message: "Email này đã được đăng ký trên hệ thống!" });
        }

        // Tạo mới và lưu vào MongoDB
        const newUser = new User({ fullName: fullName.trim(), email: email.trim(), password });
        await newUser.save();

        // Trả phản hồi thành công chuẩn định dạng JSON giống Frontend yêu cầu
        res.status(200).json({ success: true, message: "Đăng ký thành công!" });
    } catch (error) {
        res.status(500).json({ success: false, message: "Lỗi hệ thống phía server", error });
    }
});

// 3. API xử lý Đăng nhập (Viết sẵn để khớp luồng với LoginTC)
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        const user = await User.findOne({ email: email.trim(), password });

        if (!user) {
            return res.status(200).json({ success: false, message: "Email hoặc mật khẩu không chính xác!" });
        }

        res.status(200).json({
            success: true,
            data: {
                email: user.email,
                name: user.fullName
            }
        });
    } catch (error) {
        res.status(500).json({ success: false, message: "Lỗi xử lý đăng nhập", error });
    }
});

module.exports = router;