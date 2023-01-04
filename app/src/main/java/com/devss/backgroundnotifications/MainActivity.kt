package com.devss.backgroundnotifications

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.devss.backgroundnotifications.databinding.ActivityMainBinding
import com.devss.backgroundnotifications.utils.RetrofitInstance
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
private const val TOPIC = "/topic/myTopic"

class MainActivity : AppCompatActivity() {

    private lateinit var mView: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mView = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mView.root)

        FirebaseService.sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
//            val msg = getString(R.string.msg_token_fmt, token)

            FirebaseService.token = token
            mView.tokenEt.setText(token)
        })
//        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
//            FirebaseService.token = it.token
//            etToken.setText(it.token)
//        }

        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

        mView.sendBtn.setOnClickListener {
            val title = mView.titleEt.text.toString().trim()
            val msg = mView.msgEt.text.toString().trim()
            val recipientToken = mView.tokenEt.text.toString().trim()

            if (title.isNotEmpty() && msg.isNotEmpty() && recipientToken.isNotEmpty()) {
                PushNotification(
                    NotificationData(title, msg),
                    recipientToken
                ).also {
                    sendNotification(it)
                }
            } else Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            response.body()?.close()

            Log.i(TAG, "sendNotification: ${response.raw()}")
            if(response.isSuccessful) {
                Log.d(TAG, "Response: ${response}")
//                Log.d(TAG, "Response: ${Gson().toJson(response.body()?.string())}")
            } else {
                Log.e(TAG, "Response Error: ${response.code()}")
            }
            response.body()?.close()
        } catch(e: Exception) {
            Log.e(TAG, "Error: ${e.message.toString()}")
        }
    }
}