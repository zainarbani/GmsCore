/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.util.Base64
import android.util.Log

object FallbackCreator {
    private val FAST_FAIL = setOf("ad_attest", "recaptcha-frame", "federatedMachineLearningReduced", "msa-f", "ad-event-attest-token")

    @JvmStatic
    fun create(flow: String?, context: Context, map: Map<Any?, Any?>, e: Throwable): ByteArray {
        Log.w("DGFallback", "create($flow)")
        return if (flow in FAST_FAIL) {
            "ERROR : no fallback for $flow".encodeToByteArray()
        } else {
            try {
                create(map, null, flow, context, e)
            } catch (e: Throwable) {
                Log.w("DGFallback", e)
                "ERROR : $e".encodeToByteArray()
            }
        }
    }

    @JvmStatic
    fun create(map: Map<Any?, Any?>, bytes: ByteArray?, flow: String?, context: Context, e: Throwable): ByteArray {
        val fallback = buildString {
            append("ERROR : ")
            append("flow=")
            append(flow ?: "")
            append(" pkg=")
            append(context.packageName)
            if (map.isNotEmpty()) {
                append(" map=")
                append(
                    map.entries
                        .take(8)
                        .joinToString(",") { "${safeString(it.key)}=${safeString(it.value)}" }
                )
            }
            if (bytes != null && bytes.isNotEmpty()) {
                val prefix = bytes.copyOf(minOf(bytes.size, 24))
                append(" data=")
                append(Base64.encodeToString(prefix, Base64.NO_WRAP))
                if (bytes.size > prefix.size) append("...")
            }
            append(" ex=")
            append(e.javaClass.name)
            e.message?.let {
                append("(")
                append(safeString(it))
                append(")")
            }
            e.cause?.let {
                append(" cause=")
                append(it.javaClass.name)
            }
        }
        Log.w("DGFallback", fallback, e)
        return fallback.encodeToByteArray()
    }

    private fun safeString(any: Any?): String {
        if (any == null) return "null"
        return any.toString().replace('\n', ' ').trim().take(200)
    }
}
