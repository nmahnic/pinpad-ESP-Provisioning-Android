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


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.ContentLoadingProgressBar
import com.espressif.AppConstants
import com.espressif.model.Pinpad
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPConstants.ProvisionFailureReason
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.wifi_provisioning.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception

class ProvisionActivity : AppCompatActivity() {

    private var tvTitle: TextView? = null
    private var tvBack: TextView? = null
    private var tvCancel: TextView? = null
    private var tick1: ImageView? = null
    private var tick2: ImageView? = null
    private var tick3: ImageView? = null
    private var progress1: ContentLoadingProgressBar? = null
    private var progress2: ContentLoadingProgressBar? = null
    private var progress3: ContentLoadingProgressBar? = null
    private var tvErrAtStep1: TextView? = null
    private var tvErrAtStep2: TextView? = null
    private var tvErrAtStep3: TextView? = null
    private var tvProvError: TextView? = null
    private var btnOk: CardView? = null
    private var txtOkBtn: TextView? = null
    private var ssidValue: String? = null
    private var passphraseValue: String? = ""
    private var provisionManager: ESPProvisionManager? = null
    private var isProvisioningCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provision)
        val intent = intent
        ssidValue = intent.getStringExtra(AppConstants.KEY_WIFI_SSID)
        passphraseValue = intent.getStringExtra(AppConstants.KEY_WIFI_PASSWORD)
        provisionManager = ESPProvisionManager.getInstance(applicationContext)
        initViews()
        EventBus.getDefault().register(this)
        Log.d(TAG, "Selected AP -$ssidValue")
        showLoading()
        doProvisioning()
    }

    override fun onBackPressed() {
        provisionManager!!.espDevice.disconnectDevice()
        super.onBackPressed()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: DeviceConnectionEvent) {
        Log.d(TAG, "On Device Connection Event RECEIVED : " + event.eventType)
        when (event.eventType) {
            ESPConstants.EVENT_DEVICE_DISCONNECTED -> if (!isFinishing && !isProvisioningCompleted) {
                showAlertForDeviceDisconnected()
            }
        }
    }

    private val okBtnClickListener = View.OnClickListener {
        val deviceName = provisionManager?.espDevice?.deviceName
        Log.d("NM", "deviceName: $deviceName")
        Log.d("NM", "ssidValue: $ssidValue")
        provisionManager!!.espDevice.disconnectDevice()
        Pinpad.chargeResponse(
            deviceName = deviceName.toString(),
            ssid = ssidValue.toString(),
            errorCode = "SUCCESS"
        )
        finish()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.main_toolbar_title)
        tvBack = findViewById(R.id.btn_back)
        tvCancel = findViewById(R.id.btn_cancel)
        tick1 = findViewById(R.id.iv_tick_1)
        tick2 = findViewById(R.id.iv_tick_2)
        tick3 = findViewById(R.id.iv_tick_3)
        progress1 = findViewById(R.id.prov_progress_1)
        progress2 = findViewById(R.id.prov_progress_2)
        progress3 = findViewById(R.id.prov_progress_3)
        tvErrAtStep1 = findViewById(R.id.tv_prov_error_1)
        tvErrAtStep2 = findViewById(R.id.tv_prov_error_2)
        tvErrAtStep3 = findViewById(R.id.tv_prov_error_3)
        tvProvError = findViewById(R.id.tv_prov_error)
        tvTitle?.setText(R.string.title_activity_provisioning)
        tvBack?.setVisibility(View.GONE)
        tvCancel?.setVisibility(View.GONE)
        btnOk = findViewById(R.id.btn_ok)
        txtOkBtn = findViewById(R.id.text_btn)
        btnOk?.findViewById<View>(R.id.iv_arrow)?.visibility = View.GONE
        txtOkBtn?.setText(R.string.btn_ok)
        btnOk?.setOnClickListener(okBtnClickListener)
    }

    private fun doProvisioning() {
        tick1!!.visibility = View.GONE
        progress1!!.visibility = View.VISIBLE
        provisionManager!!.espDevice.provision(
            ssidValue,
            passphraseValue,
            object : ProvisionListener {
                override fun createSessionFailed(e: Exception) {
                    runOnUiThread {
                        tick1!!.setImageResource(R.drawable.ic_error)
                        tick1!!.visibility = View.VISIBLE
                        progress1!!.visibility = View.GONE
                        tvErrAtStep1!!.visibility = View.VISIBLE
                        tvErrAtStep1!!.setText(R.string.error_session_creation)
                        tvProvError!!.visibility = View.VISIBLE
                        hideLoading()
                    }
                }

                override fun wifiConfigSent() {
                    runOnUiThread {
                        tick1!!.setImageResource(R.drawable.ic_checkbox_on)
                        tick1!!.visibility = View.VISIBLE
                        progress1!!.visibility = View.GONE
                        tick2!!.visibility = View.GONE
                        progress2!!.visibility = View.VISIBLE
                    }
                }

                override fun wifiConfigFailed(e: Exception) {
                    runOnUiThread {
                        tick1!!.setImageResource(R.drawable.ic_error)
                        tick1!!.visibility = View.VISIBLE
                        progress1!!.visibility = View.GONE
                        tvErrAtStep1!!.visibility = View.VISIBLE
                        tvErrAtStep1!!.setText(R.string.error_prov_step_1)
                        tvProvError!!.visibility = View.VISIBLE
                        hideLoading()
                    }
                }

                override fun wifiConfigApplied() {
                    runOnUiThread {
                        tick2!!.setImageResource(R.drawable.ic_checkbox_on)
                        tick2!!.visibility = View.VISIBLE
                        progress2!!.visibility = View.GONE
                        tick3!!.visibility = View.GONE
                        progress3!!.visibility = View.VISIBLE
                    }
                }

                override fun wifiConfigApplyFailed(e: Exception) {
                    runOnUiThread {
                        tick2!!.setImageResource(R.drawable.ic_error)
                        tick2!!.visibility = View.VISIBLE
                        progress2!!.visibility = View.GONE
                        tvErrAtStep2!!.visibility = View.VISIBLE
                        tvErrAtStep2!!.setText(R.string.error_prov_step_2)
                        tvProvError!!.visibility = View.VISIBLE
                        hideLoading()
                    }
                }

                override fun provisioningFailedFromDevice(failureReason: ProvisionFailureReason) {
                    runOnUiThread {
                        when (failureReason) {
                            ProvisionFailureReason.AUTH_FAILED -> tvErrAtStep3!!.setText(R.string.error_authentication_failed)
                            ProvisionFailureReason.NETWORK_NOT_FOUND -> tvErrAtStep3!!.setText(R.string.error_network_not_found)
                            ProvisionFailureReason.DEVICE_DISCONNECTED, ProvisionFailureReason.UNKNOWN -> tvErrAtStep3!!.setText(
                                R.string.error_prov_step_3
                            )
                        }
                        tick3!!.setImageResource(R.drawable.ic_error)
                        tick3!!.visibility = View.VISIBLE
                        progress3!!.visibility = View.GONE
                        tvErrAtStep3!!.visibility = View.VISIBLE
                        tvProvError!!.visibility = View.VISIBLE
                        hideLoading()
                    }
                }

                override fun deviceProvisioningSuccess() {
                    runOnUiThread {
                        isProvisioningCompleted = true
                        tick3!!.setImageResource(R.drawable.ic_checkbox_on)
                        tick3!!.visibility = View.VISIBLE
                        progress3!!.visibility = View.GONE
                        hideLoading()
                    }
                }

                override fun onProvisioningFailed(e: Exception) {
                    runOnUiThread {
                        tick3!!.setImageResource(R.drawable.ic_error)
                        tick3!!.visibility = View.VISIBLE
                        progress3!!.visibility = View.GONE
                        tvErrAtStep3!!.visibility = View.VISIBLE
                        tvErrAtStep3!!.setText(R.string.error_prov_step_3)
                        tvProvError!!.visibility = View.VISIBLE
                        hideLoading()
                    }
                }
            })
    }

    private fun showLoading() {
        btnOk!!.isEnabled = false
        btnOk!!.alpha = 0.5f
    }

    fun hideLoading() {
        btnOk!!.isEnabled = true
        btnOk!!.alpha = 1f
    }

    private fun showAlertForDeviceDisconnected() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle(R.string.error_title)
        builder.setMessage(R.string.dialog_msg_ble_device_disconnection)

        // Set up the buttons
        builder.setPositiveButton(
            R.string.btn_ok
        ) { dialog, which ->
            dialog.dismiss()
            finish()
        }
        builder.show()
    }

    companion object Run {
        private val TAG = ProvisionActivity::class.java.simpleName
    }
}