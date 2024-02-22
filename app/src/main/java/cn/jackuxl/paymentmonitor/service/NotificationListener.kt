package cn.jackuxl.paymentmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import cn.jackuxl.paymentmonitor.Message
import cn.jackuxl.paymentmonitor.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationListener : NotificationListenerService() {
    override fun onCreate() {
        val id = getString(R.string.channel_id) // 渠道名称
        val name = getString(R.string.app_name) // 渠道描述
        val importance = NotificationManager.IMPORTANCE_HIGH // 重要性级别
        val channel = NotificationChannel(id, name, importance)
        channel.description = getString(R.string.channel_desc)
        // 注册渠道
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
        val notification = Notification.Builder(applicationContext, getString(R.string.channel_id))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(getString(R.string.payment_is_listening))
            .setContentTitle(getString(R.string.app_name))
            .build()
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        startForeground(notificationId, notification)
        super.onCreate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.e(TAG, "onNotificationPosted")
        val packageName = sbn.packageName
        Log.e(TAG, packageName)
        val type: Message.Type = if ("com.eg.android.AlipayGphone" == packageName) {
            Message.Type.Alipay
        } else if ("com.tencent.mm" == packageName) {
            Message.Type.Wechat
        } else {
            return
        }
        val notification = sbn.notification
        val bundle = notification.extras

        val whenTimeMillis = notification.`when`
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val whenTimeString = sdf.format(Date(whenTimeMillis))

        val msg = Message(
            source = type,
            amount = extractFirstMatchingPart(bundle.getString(Notification.EXTRA_TEXT)?:"", bundle.getString(Notification.EXTRA_TITLE)?:"")?:return,
            date = whenTimeString
        )
        val intent = Intent("com.example.ACTION")
        intent.putExtra("msg", msg)
        sendBroadcast(intent)
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "onNotificationRemoved")
        super.onNotificationRemoved(sbn)
    }

    private fun extractFirstMatchingPart(text1: String, text2: String): String? {
        val pattern = """\d+(\.\d+)?元""".toRegex()

        // 尝试在第一个文本中查找匹配项
        pattern.find(text1)?.let {
            return it.value
        }

        // 如果在第一个文本中没有找到，再在第二个文本中查找
        return pattern.find(text2)?.value
    }

    companion object {
        const val TAG = "NotificationListener"
        const val notificationId = 1000
    }
}
