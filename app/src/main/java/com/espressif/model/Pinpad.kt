package com.espressif.model

import android.content.Intent

object Pinpad {
    var receiverIntent: Intent? = null
    var returnIntent: Intent? = null
    var payment: PpPayment? = null
    var doingPayment: Boolean = false
    var paymentResult: PpPaymentResult? = null

    fun sendResponse(): Intent? {
        paymentResult?.let{
            returnIntent = Intent()
            returnIntent!!.putExtra("transactionResult", it.transactionResult)
            returnIntent!!.putExtra("errorCode", it.errorCode)
            returnIntent!!.putExtra("issuer", it.issuer)
            returnIntent!!.putExtra("installments", it.installments)
            returnIntent!!.putExtra("approvedCode", it.approvedCode)
            returnIntent!!.putExtra("rrn", it.rrn)
            returnIntent!!.putExtra("maskedCardNo", it.maskedCardNo)
            returnIntent!!.action = Intent.ACTION_SEND
            returnIntent!!.type = "text/plain"

            return returnIntent!!
        }
        return null
    }

    fun chargeDefaultErrorResponse(){
        chargeResponse(
            transactionResult = "CANCELLED",
        )
    }

    fun chargeResponse(
        transactionResult: String,
        errorCode: String? = null,
        issuer: String? = null,
        installments: Int? = null,
        approvedCode: String? = null,
        rrn: String? = null,
        maskedCardNo: String? = null
    ){
        paymentResult = PpPaymentResult(
            transactionResult = transactionResult,
            errorCode = errorCode,
            issuer = issuer,
            installments = installments,
            approvedCode = approvedCode,
            rrn = rrn,
            maskedCardNo = maskedCardNo
        )
    }
}