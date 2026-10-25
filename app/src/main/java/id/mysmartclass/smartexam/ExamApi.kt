package id.mysmartclass.smartexam

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Klien HTTP ringan untuk API ujian LAN; online memakai kontrak API yang sama. */
class ExamApi(private val baseUrl: String) {
    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("User-Agent", "MySmartExamBrowser/0.1.0")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = try { JSONObject(text) } catch (_: Exception) {
                throw IOException("Respons server tidak sesuai format My SmartClass (HTTP $status).")
            }
            if (status !in 200..299) throw IOException(response.optString("error").ifBlank { "Server menolak permintaan (HTTP $status)." })
            return response.optJSONObject("data") ?: throw IOException("Server tidak mengirim data ujian.")
        } finally {
            connection.disconnect()
        }
    }

    fun examInfo(code: String): PublicExamInfo = PublicExamInfo.fromJson(
        request("GET", "/api/exam/public/info?token=${enc(code)}")
    )

    fun join(code: String, participant: ExamParticipant): AttemptData = AttemptData.fromJson(
        request("POST", "/api/exam/public/join", JSONObject()
            .put("token", code)
            .put("studentId", participant.id)
            .put("studentCode", participant.name))
    )

    fun resume(attemptId: String, key: String): AttemptData = AttemptData.fromJson(
        request("GET", "/api/exam/public/attempt/${enc(attemptId)}?key=${enc(key)}")
    )

    fun saveAnswer(attemptId: String, key: String, questionId: String, answer: String) {
        request("POST", "/api/exam/public/attempt/${enc(attemptId)}/answer", JSONObject()
            .put("key", key)
            .put("questionId", questionId)
            .put("answer", answer))
    }

    fun submit(attemptId: String, key: String): String = request(
        "POST", "/api/exam/public/attempt/${enc(attemptId)}/submit", JSONObject().put("key", key)
    ).getString("status")
}
