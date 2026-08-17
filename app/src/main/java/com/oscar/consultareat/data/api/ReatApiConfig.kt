package com.oscar.consultareat.data.api

import android.content.Context
import com.oscar.consultareat.BuildConfig
import com.oscar.consultareat.R

data class ReatApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val userAgent: String
) {
    val esConfigurada: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    companion object {
        fun desdeRecursos(context: Context): ReatApiConfig {
            val res = context.resources
            return ReatApiConfig(
                baseUrl = res.getString(R.string.api_nap_base_url),
                apiKey = BuildConfig.NAP_API_KEY,
                userAgent = res.getString(R.string.api_nap_user_agent)
            )
        }
    }
}
