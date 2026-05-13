package com.example.globalcashflowmonitor.data

data class ProvinceFlow(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var fdiInflow: Double,
    var budgetOutflow: Double
)