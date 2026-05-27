package com.example.globalcashflowmonitor.data

// Khuôn mẫu dữ liệu cho 1 quốc gia
data class CountryData(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val fdi: Double = 0.0,
    val tradeBalance: Double = 0.0,
    val gdp: Double = 0.0,
    val remittances: Double = 0.0,
    val fpi: Double = 0.0,
    val reserves: Double = 0.0,
    val debt: Double = 0.0,
    val tourism: Double = 0.0
)

object MockData {
    // Danh sách mở rộng phủ khắp các châu lục trên thế giới để map nhìn "Full"
    val topCountries = listOf(
        CountryData("VN", "Việt Nam", 16.0544, 108.2022, 36.6, 28.0, 430.0, 16.0, 5.2, 88.0, 130.0, 3.4),
        CountryData("US", "Hoa Kỳ", 37.0902, -95.7129, 388.0, -948.0, 27360.0, 7.0, 1200.0, 242.0, 34000.0, 190.0),
        CountryData("CN", "Trung Quốc", 35.8617, 104.1954, 163.0, 823.0, 17700.0, 50.0, 400.0, 3200.0, 2700.0, 40.0),
        CountryData("RU", "Liên Bang Nga", 61.5240, 105.3188, 12.0, 140.0, 2000.0, 3.0, 15.0, 580.0, 480.0, 5.0),
        CountryData("BR", "Brazil", -14.2350, -51.9253, 60.0, 98.0, 2130.0, 4.5, 25.0, 350.0, 680.0, 6.0),
        CountryData("AU", "Australia", -25.2744, 133.7751, 42.0, 65.0, 1700.0, 2.0, 80.0, 60.0, 2300.0, 12.0),
        CountryData("IN", "Ấn Độ", 20.5937, 78.9629, 70.0, -85.0, 3730.0, 100.0, 45.0, 600.0, 620.0, 9.0),
        CountryData("DE", "Cộng Hòa Liên Bang Đức", 51.1657, 10.4515, 55.0, 290.0, 4430.0, 5.0, 110.0, 300.0, 5700.0, 38.0),
        CountryData("JP", "Nhật Bản", 36.2048, 138.2529, 32.0, -40.0, 4200.0, 2.5, 150.0, 1200.0, 4400.0, 25.0),
        CountryData("CA", "Canada", 56.1304, -106.3468, 50.0, 15.0, 2140.0, 1.8, 75.0, 90.0, 2900.0, 18.0),
        CountryData("ZA", "Nam Phi", -30.5595, 22.9375, 9.5, 4.0, 380.0, 0.9, 11.0, 60.0, 170.0, 4.5),
        CountryData("GB", "Vương Quốc Anh", 55.3781, -3.4360, 65.0, -250.0, 3330.0, 4.0, 190.0, 180.0, 8700.0, 31.0),
        CountryData("KR", "Hàn Quốc", 35.9078, 127.7669, 18.0, 10.0, 1710.0, 7.0, 50.0, 420.0, 660.0, 10.0),
        CountryData("ID", "Indonesia", -0.7893, 113.9213, 22.0, 36.0, 1370.0, 10.0, 8.0, 140.0, 400.0, 7.0)
    )
}