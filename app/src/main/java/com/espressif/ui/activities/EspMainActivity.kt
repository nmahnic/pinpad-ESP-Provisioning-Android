// Copyright 2020 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.espressif.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.espressif.AppConstants
import com.espressif.model.Pinpad
import com.espressif.model.PpInput
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.wifi_provisioning.R

class EspMainActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT:Long = 2000 // 2 sec

    private var provisionManager: ESPProvisionManager? = null
    private var btnAddDevice: CardView? = null
    private var sharedPreferences: SharedPreferences? = null
    private var deviceType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esp_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.title_activity_connect_device)
        setSupportActionBar(toolbar)

        initViews()
        sharedPreferences = getSharedPreferences(AppConstants.ESP_PREFERENCES, MODE_PRIVATE)
        provisionManager = ESPProvisionManager.getInstance(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        deviceType = sharedPreferences!!.getString(
            AppConstants.KEY_DEVICE_TYPES,
            AppConstants.DEVICE_TYPE_DEFAULT
        )
        if (deviceType == "wifi") {
            val editor = sharedPreferences!!.edit()
            editor.putString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT)
            editor.apply()
        }
        deviceType = sharedPreferences!!.getString(
            AppConstants.KEY_DEVICE_TYPES,
            AppConstants.DEVICE_TYPE_DEFAULT
        )

        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            Pinpad.receiverIntent = intent
            Pinpad.transaction = PpInput(
                inputData = intent.getStringExtra("inputData") ?: "",
            )
        }
        Pinpad.sendResponse()
        pinpadReturn()
    }

    private fun pinpadReturn(){
        Log.d("NM", "pinpadPayment.pinpadReturn is null?")
        Pinpad.returnIntent?.let { intent ->
            Log.d("NM", "pinpadPayment.Return launch")
            setResult(RESULT_OK, intent)
            Pinpad.returnIntent = null
            finish()
        }
    }

    private fun initViews() {
        btnAddDevice = findViewById(R.id.btn_provision_device)
        btnAddDevice?.findViewById<View>(R.id.iv_arrow)?.visibility = View.GONE
        btnAddDevice?.setOnClickListener(addDeviceBtnClickListener)
        val tvAppVersion = findViewById<TextView>(R.id.tv_app_version)
        var version = ""
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            version = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val appVersion = getString(R.string.app_version) + " - v" + version
        tvAppVersion.text = appVersion
    }

    var addDeviceBtnClickListener = View.OnClickListener { startProvisioningFlow() }
    private fun startProvisioningFlow() {
        deviceType = sharedPreferences!!.getString(
            AppConstants.KEY_DEVICE_TYPES,
            AppConstants.DEVICE_TYPE_DEFAULT
        )
        val isSec1 = sharedPreferences!!.getBoolean(AppConstants.KEY_SECURITY_TYPE, true)
        Log.d(TAG, "Device Types : $deviceType")
        Log.d(TAG, "isSec1 : $isSec1")
        var securityType = 0
        if (isSec1) {
            securityType = 1
        }
        if (isSec1) {
            provisionManager!!.createESPDevice(
                ESPConstants.TransportType.TRANSPORT_SOFTAP,
                ESPConstants.SecurityType.SECURITY_1
            )
        } else {
            provisionManager!!.createESPDevice(
                ESPConstants.TransportType.TRANSPORT_SOFTAP,
                ESPConstants.SecurityType.SECURITY_0
            )
        }
        goToWiFiProvisionLandingActivity(securityType)
    }

    private fun goToWiFiProvisionLandingActivity(securityType: Int) {
        val intent = Intent(this, ProvisionLanding::class.java)
        intent.putExtra(AppConstants.KEY_SECURITY_TYPE, securityType)
        startActivity(intent)
    }

    companion object {
        private val TAG = EspMainActivity::class.java.simpleName
    }

}