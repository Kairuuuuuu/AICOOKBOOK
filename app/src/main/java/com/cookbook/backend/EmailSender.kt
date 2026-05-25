package com.cookbook.backend

import android.util.Log
import com.cookbook.BuildConfig
import java.util.Properties
import java.util.Random
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    private const val TAG = "EmailSender"
    private const val SENDER_EMAIL = "aicookbooknoreply@gmail.com"

    fun sendOTP(recipientEmail: String): String {
        val emailPassword = BuildConfig.EMAIL_PASSWORD
        
        if (emailPassword.isBlank() || emailPassword == "EMAIL_PASSWORD") {
            return ""
        }
        
        val otp = String.format("%06d", Random().nextInt(1000000))

        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, emailPassword)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL))
                setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "AI COOKBOOK - Verification Code"
                setContent(
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                        <h2 style="color: #2D6A4F;">AI COOKBOOK</h2>
                        <p>Your verification code is:</p>
                        <h1 style="color: #2D6A4F; font-size: 32px; letter-spacing: 8px;">$otp</h1>
                        <p>Enter this code in the app to verify your email.</p>
                        <p style="color: #666; font-size: 12px;">
                            If you didn't request this code, please ignore this email.
                        </p>
                    </div>
                    """.trimIndent(),
                    "text/html; charset=utf-8"
                )
            }

            Log.d(TAG, "Attempting to send OTP email to $recipientEmail...")
            Transport.send(message)
            Log.d(TAG, "OTP email successfully sent!")
            return otp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send OTP email to $recipientEmail", e)
            return ""
        }
    }
}

