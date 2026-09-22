package id.ac.usm.sipandutambak.data.model

data class DeviceData(
    val id: String,
    val name: String,
    val pondName: String = "Kolam 1",
    val commodity: String = "Nila Merah",
    val ipAddress: String = "-",
    val batteryPercent: Int = 98,
    val signalQuality: String = "Kuat",
    val status: String = "NORMAL",
    val isConnected: Boolean = true,
    val ponds: List
<
String
>
 = listOf(pondName),
    val pondCommodities: Map
<
String, String
>
 = mapOf(pondName to commodity)
)
