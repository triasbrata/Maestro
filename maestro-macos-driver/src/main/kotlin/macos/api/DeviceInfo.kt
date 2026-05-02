package macos.api

data class DeviceInfo(
    val width: Int,
    val height: Int,
    val osVersion: String,
    val screenScale: Double?
)
