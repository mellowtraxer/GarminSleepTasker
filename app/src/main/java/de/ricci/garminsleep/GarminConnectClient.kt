package de.ricci.garminsleep

import android.content.Context
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class GarminNightMetrics(
    val avgSpo2: Double? = null,
    val minSpo2: Double? = null,
    val avgResp: Double? = null,
    val minResp: Double? = null,
    val avgHrv: Double? = null,
    val spo2Series: List<MetricPoint> = emptyList(),
    val respirationSeries: List<MetricPoint> = emptyList(),
    val hrvSeries: List<MetricPoint> = emptyList()
)

sealed class GarminLoginResult {
    data object Success : GarminLoginResult()
    data class MfaRequired(val method: String) : GarminLoginResult()
    data class Error(val message: String) : GarminLoginResult()
}

/**
 * Small, self-contained Garmin Connect client for the fields Health Connect
 * does not receive from Garmin. Passwords are never persisted. Only Garmin's
 * OAuth access/refresh tokens are kept in this app's private SharedPreferences.
 *
 * Garmin Connect's consumer API is unofficial and can change without notice.
 */
class GarminConnectClient(private val context: Context) {
    companion object {
        private const val SSO = "https://sso.garmin.com"
        private const val API = "https://connectapi.garmin.com"
        private const val DI_TOKEN = "https://diauth.garmin.com/di-oauth2-service/oauth/token"
        private const val IOS_CLIENT = "GCM_IOS_DARK"
        private const val IOS_SERVICE = "https://mobile.integration.garmin.com/gcm/ios"
        private const val IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
        private const val DI_GRANT = "https://connectapi.garmin.com/di-oauth2-service/oauth/grant/service_ticket"
        private val DI_CLIENTS = listOf(
            "GARMIN_CONNECT_MOBILE_ANDROID_DI_2025Q2",
            "GARMIN_CONNECT_MOBILE_ANDROID_DI_2024Q4",
            "GARMIN_CONNECT_MOBILE_ANDROID_DI",
            "GARMIN_CONNECT_MOBILE_IOS_DI"
        )
    }

    private val prefs = context.getSharedPreferences("garmin_auth", Context.MODE_PRIVATE)
    private val cookieJar = MemoryCookieJar()
    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var mfaMethod = "email"

    fun isLinked(): Boolean = prefs.contains("refresh_token") || prefs.contains("access_token")

    fun logout() {
        prefs.edit().clear().apply()
        cookieJar.clear()
    }

    fun login(email: String, password: String): GarminLoginResult = try {
        val url = "$SSO/mobile/api/login".toHttpUrl().newBuilder()
            .addQueryParameter("clientId", IOS_CLIENT)
            .addQueryParameter("locale", "en-US")
            .addQueryParameter("service", IOS_SERVICE)
            .build()
        val body = JSONObject()
            .put("username", email)
            .put("password", password)
            .put("rememberMe", true)
            .put("captchaToken", "")
            .toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url)
            .header("User-Agent", IOS_UA)
            .header("Accept", "application/json, text/plain, */*")
            .header("Origin", SSO)
            .post(body).build()
        http.newCall(req).execute().use { response ->
            if (response.code == 429) return GarminLoginResult.Error("Garmin blockiert den Login momentan (HTTP 429). Bitte später erneut versuchen.")
            if (response.code == 403) return GarminLoginResult.Error("Garmin hat den Login mit einer Sicherheitsprüfung blockiert (HTTP 403).")
            val json = JSONObject(response.body?.string().orEmpty())
            when (json.optJSONObject("responseStatus")?.optString("type")) {
                "SUCCESSFUL" -> {
                    exchangeTicket(json.getString("serviceTicketId"))
                    GarminLoginResult.Success
                }
                "MFA_REQUIRED" -> {
                    mfaMethod = json.optJSONObject("customerMfaInfo")?.optString("mfaLastMethodUsed", "email") ?: "email"
                    GarminLoginResult.MfaRequired(mfaMethod)
                }
                "INVALID_USERNAME_PASSWORD" -> GarminLoginResult.Error("Garmin-Anmeldung fehlgeschlagen: E-Mail oder Passwort falsch.")
                "CAPTCHA_REQUIRED" -> GarminLoginResult.Error("Garmin verlangt aktuell eine CAPTCHA-Prüfung. Bitte später erneut versuchen.")
                else -> GarminLoginResult.Error("Garmin-Anmeldung fehlgeschlagen.")
            }
        }
    } catch (t: Throwable) {
        GarminLoginResult.Error(t.message ?: "Unbekannter Garmin-Anmeldefehler")
    }

    fun verifyMfa(code: String): GarminLoginResult = try {
        val url = "$SSO/mobile/api/mfa/verifyCode".toHttpUrl().newBuilder()
            .addQueryParameter("clientId", IOS_CLIENT)
            .addQueryParameter("locale", "en-US")
            .addQueryParameter("service", IOS_SERVICE)
            .build()
        val body = JSONObject()
            .put("mfaMethod", mfaMethod)
            .put("mfaVerificationCode", code.trim())
            .put("rememberMyBrowser", true)
            .put("reconsentList", org.json.JSONArray())
            .put("mfaSetup", false)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url)
            .header("User-Agent", IOS_UA)
            .header("Accept", "application/json, text/plain, */*")
            .header("Origin", SSO)
            .post(body).build()
        http.newCall(req).execute().use { response ->
            val json = JSONObject(response.body?.string().orEmpty())
            if (json.optJSONObject("responseStatus")?.optString("type") == "SUCCESSFUL") {
                exchangeTicket(json.getString("serviceTicketId"))
                GarminLoginResult.Success
            } else GarminLoginResult.Error("Garmin-MFA-Code wurde nicht akzeptiert.")
        }
    } catch (t: Throwable) {
        GarminLoginResult.Error(t.message ?: "MFA fehlgeschlagen")
    }

    private fun exchangeTicket(ticket: String) {
        var last = "kein DI-Client akzeptiert"
        for (clientId in DI_CLIENTS) {
            val form = FormBody.Builder()
                .add("client_id", clientId)
                .add("service_ticket", ticket)
                .add("grant_type", DI_GRANT)
                .add("service_url", IOS_SERVICE)
                .build()
            val basic = android.util.Base64.encodeToString("$clientId:".toByteArray(), android.util.Base64.NO_WRAP)
            val req = Request.Builder().url(DI_TOKEN)
                .headers(nativeHeaders())
                .header("Authorization", "Basic $basic")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(form).build()
            http.newCall(req).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    last = "HTTP ${response.code}"
                    return@use
                }
                val json = JSONObject(raw)
                val access = json.optString("access_token")
                if (access.isNotBlank()) {
                    val refresh = json.optString("refresh_token")
                    prefs.edit()
                        .putString("access_token", access)
                        .putString("refresh_token", refresh)
                        .putString("client_id", clientIdFromJwt(access) ?: clientId)
                        .apply()
                    return
                }
            }
            if (prefs.contains("access_token")) return
        }
        throw IOException("Garmin Token-Austausch fehlgeschlagen: $last")
    }

    private fun refresh(): Boolean {
        val refresh = prefs.getString("refresh_token", null) ?: return false
        val clientId = prefs.getString("client_id", null) ?: return false
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", clientId)
            .add("refresh_token", refresh)
            .build()
        val basic = android.util.Base64.encodeToString("$clientId:".toByteArray(), android.util.Base64.NO_WRAP)
        val req = Request.Builder().url(DI_TOKEN)
            .headers(nativeHeaders())
            .header("Authorization", "Basic $basic")
            .post(form).build()
        return http.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@use false
            val json = JSONObject(response.body?.string().orEmpty())
            val access = json.optString("access_token")
            if (access.isBlank()) return@use false
            prefs.edit()
                .putString("access_token", access)
                .putString("refresh_token", json.optString("refresh_token", refresh))
                .putString("client_id", clientIdFromJwt(access) ?: clientId)
                .apply()
            true
        }
    }

    fun nightMetrics(date: LocalDate): GarminNightMetrics? {
        if (!isLinked()) return null
        // Refresh first. If Garmin rejects refresh but the current access token
        // is still valid, the API call below still gets one chance.
        runCatching { refresh() }
        val token = prefs.getString("access_token", null) ?: return null
        val d = date.toString()

        // This sleep endpoint does not need displayName and returns the nightly
        // summary used by Garmin Connect, including sleep SpO2/respiration/HRV.
        val sleep = apiGet(
            "/sleep-service/sleep/dailySleepData",
            mapOf("date" to d, "nonSleepBufferMinutes" to "60"),
            token
        ) ?: return fallbackMetrics(d, token)

        val dto = sleep.optJSONObject("dailySleepDTO")
        val avgSpo2 = dto.num("averageSpO2Value") ?: dto.num("avgSpO2")
        val minSpo2 = dto.num("lowestSpO2Value")
        val avgResp = dto.num("averageRespirationValue")
        val minResp = dto.num("lowestRespirationValue")
        val hrv = sleep.num("avgOvernightHrv") ?: dto.num("avgSleepHRV")
        val spo2Raw = apiGet("/wellness-service/wellness/daily/spo2/$d", emptyMap(), token)
        val respRaw = apiGet("/wellness-service/wellness/daily/respiration/$d", emptyMap(), token)
        val hrvRaw = apiGet("/hrv-service/hrv/$d", emptyMap(), token)
        val spo2Series = extractSeries(spo2Raw, listOf("spo2Values","spO2Values","spo2ValueArray","sleepSpO2Values","values"), listOf("spo2","spO2","value","reading"), listOf("timestampGMT","timestamp","timeGMT","time","startGMT"))
        val respirationSeries = extractSeries(respRaw, listOf("respirationValues","respirationValueArray","respirationData","values"), listOf("respiration","respirationValue","value","breathsPerMinute"), listOf("timestampGMT","timestamp","timeGMT","time","startGMT"))
        val hrvSeries = extractSeries(hrvRaw, listOf("hrvReadings","hrvValues","readings","values"), listOf("hrv","hrvValue","value","rmssd"), listOf("readingTimeGMT","timestampGMT","timestamp","timeGMT","time"))

        if (avgSpo2 != null || minSpo2 != null || avgResp != null || minResp != null || hrv != null || spo2Series.isNotEmpty() || respirationSeries.isNotEmpty() || hrvSeries.isNotEmpty()) {
            return GarminNightMetrics(avgSpo2, minSpo2, avgResp, minResp, hrv, spo2Series, respirationSeries, hrvSeries)
        }
        return fallbackMetrics(d, token)
    }


    private fun extractSeries(root: JSONObject?, arrayKeys: List<String>, valueKeys: List<String>, timeKeys: List<String>): List<MetricPoint> {
        if (root == null) return emptyList()
        val arrays = arrayKeys.mapNotNull { root.optJSONArray(it) } + sequence {
            val it = root.keys()
            while (it.hasNext()) { val k=it.next(); root.optJSONObject(k)?.let { o -> arrayKeys.forEach { ak -> o.optJSONArray(ak)?.let { yield(it) } } } }
        }.toList()
        return arrays.flatMap { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val item=arr.opt(i)
                when(item) {
                    is JSONObject -> {
                        val v=valueKeys.firstNotNullOfOrNull { item.num(it) } ?: return@mapNotNull null
                        val raw=timeKeys.firstNotNullOfOrNull { k -> item.opt(k)?.takeUnless { it==JSONObject.NULL } }
                        parseMetricTime(raw)?.let { MetricPoint(it,v) }
                    }
                    is org.json.JSONArray -> {
                        if(item.length()<2) null else parseMetricTime(item.opt(0))?.let { t -> (item.opt(1) as? Number)?.toDouble()?.let { MetricPoint(t,it) } }
                    }
                    else -> null
                }
            }
        }.filter { it.value > 0.0 }.distinctBy { it.timeMs }.sortedBy { it.timeMs }
    }

    private fun parseMetricTime(v: Any?): Long? = when(v) {
        is Number -> v.toLong().let { if(it < 100000000000L) it*1000 else it }
        is String -> v.toLongOrNull()?.let { if(it < 100000000000L) it*1000 else it } ?: runCatching { java.time.Instant.parse(v).toEpochMilli() }.getOrNull()
        else -> null
    }

    private fun fallbackMetrics(date: String, token: String): GarminNightMetrics? {
        val spo2 = apiGet("/wellness-service/wellness/daily/spo2/$date", emptyMap(), token)
        val resp = apiGet("/wellness-service/wellness/daily/respiration/$date", emptyMap(), token)
        val avgSpo2 = spo2.num("avgSleepSpO2") ?: spo2.num("averageSpO2")
        val minSpo2 = spo2.num("lowestSpO2")
        val avgResp = resp.num("avgSleepRespirationValue") ?: resp.num("averageRespirationValue")
        val minResp = resp.num("lowestRespirationValue")
        return if (avgSpo2 != null || minSpo2 != null || avgResp != null || minResp != null)
            GarminNightMetrics(avgSpo2, minSpo2, avgResp, minResp, null) else null
    }

    private fun apiGet(path: String, params: Map<String, String>, token: String): JSONObject? {
        val ub = "$API$path".toHttpUrl().newBuilder()
        params.forEach { (k, v) -> ub.addQueryParameter(k, v) }
        val req = Request.Builder().url(ub.build())
            .headers(nativeHeaders())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get().build()
        return http.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@use null
            runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
        }
    }

    private fun nativeHeaders() = Headers.Builder()
        .add("User-Agent", "GCM-Android-5.23")
        .add("X-Garmin-User-Agent", "com.garmin.android.apps.connectmobile/5.23; ; Google/sdk_gphone64_arm64/google; Android/33; Dalvik/2.1.0")
        .add("X-Garmin-Paired-App-Version", "10861")
        .add("X-Garmin-Client-Platform", "Android")
        .add("X-App-Ver", "10861")
        .add("X-Lang", "de")
        .add("X-GCExperience", "GC5")
        .build()

    private fun clientIdFromJwt(jwt: String): String? = runCatching {
        val part = jwt.split(".")[1]
        val decoded = android.util.Base64.decode(part, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
        JSONObject(String(decoded)).optString("client_id").takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun JSONObject?.num(key: String): Double? {
        if (this == null || isNull(key) || !has(key)) return null
        val v = opt(key)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies.removeAll { old -> cookies.any { it.name == old.name && it.domain == old.domain } }
            this.cookies.addAll(cookies)
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.filter { it.matches(url) }
        fun clear() = cookies.clear()
    }
}
