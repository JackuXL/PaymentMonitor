package cn.jackuxl.paymentmonitor

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.jackuxl.paymentmonitor.service.NotificationListener
import cn.jackuxl.paymentmonitor.ui.theme.PaymentMonitorTheme
import cn.jackuxl.paymentmonitor.util.ServiceUtils

class MainActivity : ComponentActivity() {
    val TAG = "Notification"
    private lateinit var receiver: BroadcastReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNotificationEnabled()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.grant_notice)
                .setMessage(R.string.need_permission)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    startActivity(intent)
                    finish()
                }
                .show()
        }
        startListener()

        setContent {
            PaymentMonitorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val data = remember { mutableStateOf(listOf<Message>()) }
                    val income = remember { mutableStateOf("￥0") }
                    receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if(data.value.contains(intent?.getSerializableExtra("msg") as Message)){
                                return
                            }
                            print(intent.getSerializableExtra("msg") as Message)
                            // 更新MutableState的值，这将触发Compose UI的重绘
                            val newList = ArrayList(data.value)
                            data.value = newList.apply { add(intent.getSerializableExtra("msg") as Message) }
                            income.value = calculateTotalAmount(newList.map { it.amount })
                        }
                    }
                    // 注册BroadcastReceiver
                    IntentFilter("com.example.ACTION").also { filter ->
                        registerReceiver(receiver, filter)
                    }
                    Column{
                        Text(text = "Total:"+income.value, fontSize = 40.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if(data.value.isEmpty()){
                            Text(
                                text = "暂无数据",
                                modifier = Modifier
                                    .padding(20.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .weight(8f),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        else{
                            // Each cell of a column must have the same weight.
                            val column1Weight = .2f // 20%
                            val column2Weight = .2f // 20%
                            val column3Weight = .4f // 40%
                            val column4Weight = .2f // 20%
                            // The LazyColumn will be our table. Notice the use of the weights below
                            val listState = rememberLazyListState()
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(16.dp)){
                                // Here is the header
                                item {
                                    Row(Modifier.background(Color.LightGray)) {
                                        TableCell(text = "No.", weight = column1Weight)
                                        TableCell(text = "Source", weight = column2Weight)
                                        TableCell(text = "Amount", weight = column3Weight)
                                        TableCell(text = "Time", weight = column4Weight)
                                    }
                                }
                                // Here are all the lines of your table.
                                items(data.value.size) {
                                    val (source, amount, date, _) = data.value[it]
                                    Row(Modifier.fillMaxWidth()) {
                                        TableCell(text = (it+1).toString(), weight = column1Weight)
                                        TableCell(text = source.name, weight = column2Weight)
                                        TableCell(text = "￥"+calculateTotalAmount(listOf(amount)), weight = column3Weight)
                                        TableCell(text = date, weight = column4Weight)
                                    }
                                }
                            }
                            LaunchedEffect(key1 = data.value) {
                                listState.animateScrollToItem(data.value.size - 1)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isNotificationEnabled(): Boolean {
        val names = Settings.Secure.getString(
            applicationContext.contentResolver,
            "enabled_notification_listeners"
        )
        return names.contains(NotificationListener::class.java.canonicalName)
    }
    override fun onDestroy() {
        super.onDestroy()
        // 不要忘记取消注册BroadcastReceiver
        unregisterReceiver(receiver)
    }

    private fun calculateTotalAmount(amounts: List<String>): String {
        val pattern = """\d+(\.\d+)?""".toRegex()
        var total = 0.0

        for (amount in amounts) {
            val matchResult = pattern.find(amount)
            val value = matchResult?.value?.toDoubleOrNull()
            if (value != null) {
                total += value
            }
        }

        return String.format("%.2f", total)
    }

    private fun startListener() {
        if (ServiceUtils.isServiceRunning(this, NotificationListener::class.java)) {
            Log.d(TAG, "service is running")
        } else {
            val start = Intent(this, NotificationListener::class.java)
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d(TAG, "start service")
            startService(start)
        }
    }

    @Composable
    fun RowScope.TableCell(
        text: String,
        weight: Float
    ) {
        Text(
            text = text,
            Modifier
                .border(1.dp, Color.Black)
                .weight(weight)
                .padding(8.dp)
        )
    }
}
