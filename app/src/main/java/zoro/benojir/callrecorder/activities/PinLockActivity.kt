package zoro.benojir.callrecorder.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import zoro.benojir.callrecorder.R
import zoro.benojir.callrecorder.helpers.PinPreferencesHelper

class PinLockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_lock)

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        confirmButton.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            val storedPin = PinPreferencesHelper.getPin(this)

            if (storedPin == enteredPin) {
                PinPreferencesHelper.setSessionUnlocked(this, true)
                PinPreferencesHelper.updateLastUnlockTime(this)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
