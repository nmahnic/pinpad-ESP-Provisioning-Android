package com.espressif.model

import android.content.Intent

object Pinpad {
    var receiverIntent: Intent? = null
    var returnIntent: Intent? = null
    var transaction: PpInput? = null
    var response: PpOutput? = null

    fun sendResponse(): Intent? {
        response?.let{
            returnIntent = Intent()
            returnIntent!!.putExtra("deviceName", it.deviceName)
            returnIntent!!.putExtra("ssid", it.ssid)
            returnIntent!!.putExtra("errorCode", it.errorCode)
            returnIntent!!.action = Intent.ACTION_SEND
            returnIntent!!.type = "text/plain"

            return returnIntent!!
        }
        return null
    }

    fun chargeResponse(
        deviceName: String,
        ssid: String? = null,
        errorCode: String = "FAILURE"
    ){
        response = PpOutput(
            deviceName = deviceName,
            ssid = ssid,
            errorCode = errorCode
        )
    }
}