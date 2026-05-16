package com.pairshot.core.promotion.local

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.pairshot.core.promotion.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHashProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        init {
            require(BuildConfig.PROMOTION_DEVICE_HASH_SALT.isNotBlank()) {
                "PROMOTION_DEVICE_HASH_SALT must be configured in local.properties"
            }
        }

        @SuppressLint("HardwareIds")
        fun deviceHash(): String {
            val androidId =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            val input = (androidId + BuildConfig.PROMOTION_DEVICE_HASH_SALT).toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256").digest(input)
            return digest.toHex()
        }

        private fun ByteArray.toHex(): String {
            val builder = StringBuilder(size * 2)
            for (byte in this) {
                val value = byte.toInt() and BYTE_MASK
                builder.append(HEX_CHARS[value ushr HIGH_NIBBLE_SHIFT])
                builder.append(HEX_CHARS[value and LOW_NIBBLE_MASK])
            }
            return builder.toString()
        }

        private companion object {
            const val BYTE_MASK = 0xFF
            const val HIGH_NIBBLE_SHIFT = 4
            const val LOW_NIBBLE_MASK = 0x0F
            val HEX_CHARS = "0123456789abcdef".toCharArray()
        }
    }
