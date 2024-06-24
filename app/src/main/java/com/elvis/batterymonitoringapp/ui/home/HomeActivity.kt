package com.elvis.batterymonitoringapp.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.elvis.batterymonitoringapp.R
import com.elvis.batterymonitoringapp.databinding.ActivityHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {
    private var _binding: ActivityHomeBinding? = null
    private val binding get() = _binding

    private lateinit var voltageRef: DatabaseReference
    private lateinit var currentRef: DatabaseReference
    private lateinit var temp1Ref: DatabaseReference
    private lateinit var temp2Ref: DatabaseReference
    private lateinit var temp3Ref: DatabaseReference
    private lateinit var overCurrent: DatabaseReference

    private var voltage: Double? = null
    private var isZero: Boolean = false
    private var isOverCurrent: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val database = Firebase.database
        voltageRef = database.getReference("Voltage")
        currentRef = database.getReference("Current")
        temp1Ref = database.getReference("Temperature 1")
        temp2Ref = database.getReference("Temperature 2")
        temp3Ref = database.getReference("Temperature 3")
        overCurrent = database.getReference("Over Current")

        getVoltage(voltageRef)
        getCurrent(currentRef)
        getTemperature(temp1Ref, binding?.pbTemp1, binding?.tvTemp1Value)
        getTemperature(temp2Ref, binding?.pbTemp2, binding?.tvTemp2Value)
        getTemperature(temp3Ref, binding?.pbTemp3, binding?.tvTemp3Value)
        getOverVurrent(overCurrent)

    }

    private fun setProgressBar(charge: Int) {
        binding?.textViewProgress?.text = "$charge %"
        binding?.batteryCharge?.progress = charge
        val drawable = ContextCompat.getDrawable(this, R.drawable.circle) as LayerDrawable
        val rotateDrawable = drawable.getDrawable(1) as RotateDrawable
        val shapeDrawable = rotateDrawable.drawable as GradientDrawable
        var startChargeColor: Int? = null
        var endChargeColor: Int? = null

        if (charge >= 80) {
            endChargeColor = ContextCompat.getColor(this, R.color.green1end)
            startChargeColor = ContextCompat.getColor(this, R.color.green1end)
        } else if (charge >= 50 && charge <= 70){
            endChargeColor = ContextCompat.getColor(this, R.color.green2end)
            startChargeColor = ContextCompat.getColor(this, R.color.green2end)
        } else if (charge >= 30 && charge <= 40){
            endChargeColor = ContextCompat.getColor(this, R.color.yellowEnd)
            startChargeColor = ContextCompat.getColor(this, R.color.yellowEnd)
        }  else if (charge >= 0 && charge <= 20){
            endChargeColor = ContextCompat.getColor(this, R.color.redEnd)
            startChargeColor = ContextCompat.getColor(this, R.color.redEnd)
        }
        shapeDrawable.colors = intArrayOf(startChargeColor!!, endChargeColor!!)
        binding?.textViewProgress?.setTextColor(endChargeColor)

    }

    private fun setCharge() {
        if (voltage != null) {
            Log.d("", "setCharge: ============================")
            if(isZero){
                if (voltage!! >= 38.67) {
                    setProgressBar(100)
                } else if (voltage!! >= 38.34 && voltage!! < 38.67) {
                    setProgressBar(90)
                } else if (voltage!! >= 37.85 && voltage!! < 38.34) {
                    setProgressBar(80)
                } else if (voltage!! >= 37.53 && voltage!! < 37.85) {
                    setProgressBar(70)
                } else if (voltage!! >= 37.23 && voltage!! < 37.53) {
                    setProgressBar(60)
                } else if (voltage!! >= 36.69 && voltage!! < 37.23) {
                    setProgressBar(50)
                } else if (voltage!! >= 36.33 && voltage!! < 36.69) {
                    setProgressBar(40)
                } else if (voltage!! >= 35.88 && voltage!! < 36.33) {
                    setProgressBar(30)
                } else if (voltage!! >= 35.43 && voltage!! < 35.88) {
                    setProgressBar(20)
                } else if (voltage!! >= 35.1 && voltage!! < 35.43) {
                    setProgressBar(10)
                } else if (voltage!! < 35.1) {
                    setProgressBar(0)
                }
            }
        }
    }

    private fun sendNotif(){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Monitoring App")
            .setSmallIcon(R.drawable.baseline_battery_alert_24)
            .setContentText("Terjadi arus berlebih ada baterai")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSubText("Peringatan")

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            builder.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getOverVurrent(reference: DatabaseReference){
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(Boolean::class.java)
                isOverCurrent = value!!
                if (isOverCurrent){
                    sendNotif()
                }
                Log.d("TAG", "Value is: $value ")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
    }

    private fun getVoltage(reference: DatabaseReference) {
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(Double::class.java)
                voltage = value
                val formattedValue = String.format("%.2f", value)
                binding?.tvVoltageValue?.text = formattedValue
                setCharge()
                Log.d("TAG", "Value is: $value ")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
    }

    private fun getCurrent(reference: DatabaseReference) {
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val formattedValue: String
                val value = dataSnapshot.getValue() as Double
                if (value < 1.0 && value > 0) {
                    formattedValue = String.format("%.1f", value * 1000)
                    isZero = false
                    binding?.tvCurrentUnit?.text = "mA"
                } else if (value <= 0) {
                    formattedValue = "0"
                    isZero = true
                    binding?.tvCurrentUnit?.text = "A"
                } else {
                    formattedValue = String.format("%.2f", value)
                    isZero = false
                    binding?.tvCurrentUnit?.text = "A"
                }
                binding?.tvCurrentValue?.text = formattedValue
                Log.d("TAG", "Value is: $value ")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
    }

    private fun getTemperature(
        reference: DatabaseReference,
        pbTemp: ProgressBar?,
        pbTv: TextView?
    ) {
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue()
                val formattedValue = String.format("%.0f", value)

                val drawable = ContextCompat.getDrawable(this@HomeActivity, R.drawable.custom_progress_bg) as LayerDrawable
                val scaleDrawable = drawable.getDrawable(1) as ScaleDrawable
                val gradientDrawable = scaleDrawable.drawable as GradientDrawable
                var startTempColor: Int? = null
                var centerTempColor: Int? = null
                var endTempColor: Int? = null
                pbTemp?.apply {
                    max = 50
                    progress = formattedValue.toInt()
                    if (formattedValue.toInt() >= 30 && formattedValue.toInt() < 40){
                        startTempColor = ContextCompat.getColor(this@HomeActivity, R.color.blue)
                        centerTempColor = ContextCompat.getColor(this@HomeActivity, R.color.yellowEnd)
                        endTempColor = ContextCompat.getColor(this@HomeActivity, R.color.yellowEnd)
                    } else if(formattedValue.toInt() >= 40){
                        startTempColor = ContextCompat.getColor(this@HomeActivity, R.color.blue)
                        centerTempColor = ContextCompat.getColor(this@HomeActivity, R.color.yellowEnd)
                        endTempColor = ContextCompat.getColor(this@HomeActivity, R.color.redEnd)
                    } else {
                        startTempColor = ContextCompat.getColor(this@HomeActivity, R.color.blue)
                        centerTempColor = ContextCompat.getColor(this@HomeActivity, R.color.blue)
                        endTempColor = ContextCompat.getColor(this@HomeActivity, R.color.blue)
                    }
                }
                gradientDrawable.colors = intArrayOf(startTempColor!!, centerTempColor!!, endTempColor!!)
                pbTv?.apply {
                    text = String.format("%.1f", value)
                    setTextColor(endTempColor!!)
                }
                Log.d("TAG", "Value is: $value ")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "channel_01"
        private const val CHANNEL_NAME = "bms channel"
    }
}