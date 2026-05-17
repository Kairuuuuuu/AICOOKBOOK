package com.cookbook.backend

import android.util.Log
import com.cookbook.data.model.OTPResult
import com.cookbook.data.model.OTPStatus

object EmailAuthenticationService {

    private const val TAG = "EmailAuth"

    fun processEmailForOTP(email: String): OTPResult {
        val trimmedEmail = email.trim()
        if (!trimmedEmail.contains("@") || trimmedEmail.isBlank()) {
            return OTPResult(status = OTPStatus.INVALID_EMAIL)
        }
        return try {
            val sentCode = EmailSender.sendOTP(trimmedEmail)
            if (sentCode.isNotBlank()) {
                OTPResult(status = OTPStatus.SUCCESS, sentCode = sentCode)
            } else {
                OTPResult(status = OTPStatus.CONNECTION_ERROR)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in processEmailForOTP for email: $trimmedEmail", e)
            OTPResult(status = OTPStatus.CONNECTION_ERROR)
        }
    }
}

