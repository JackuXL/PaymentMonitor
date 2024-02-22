package cn.jackuxl.paymentmonitor

import java.io.Serializable
import java.sql.Timestamp

data class Message(
    var source:Type,
    var amount:String,
    var date:String,
    var timestamp: Timestamp = Timestamp(System.currentTimeMillis())

):Serializable{
    val TAG = "NotificationMessage"
    enum class Type {
        Wechat,
        Alipay
    }
    companion object {
        private const val serialVersionUID = 1L // 建议添加，以便长期兼容
    }
}