package id.mysmartclass.smartexam

import android.content.Context
import org.json.JSONObject

/** Penyimpanan internal aplikasi. Pencadangan Android untuk data ini dimatikan. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("smart_exam_local_session", Context.MODE_PRIVATE)

    data class SavedAttempt(val baseUrl: String, val code: String, val id: String, val key: String)

    @Synchronized fun saveSettings(mode: ExamConnectionMode, baseUrl: String, code: String) {
        prefs.edit().putString("mode", mode.name).putString("server", baseUrl).putString("code", code).apply()
    }

    fun savedMode(): ExamConnectionMode =
        runCatching { ExamConnectionMode.valueOf(prefs.getString("mode", "LAN") ?: "LAN") }
            .getOrDefault(ExamConnectionMode.LAN)

    fun savedServer(): String = prefs.getString("server", "") ?: ""
    fun savedCode(): String = prefs.getString("code", "") ?: ""

    @Synchronized fun saveAttempt(baseUrl: String, code: String, attempt: AttemptData) {
        prefs.edit()
            .putString("active_url", baseUrl)
            .putString("active_code", code)
            .putString("active_id", attempt.id)
            .putString("active_key", attempt.accessKey)
            .putString("active_pending", "{}")
            .apply()
    }

    @Synchronized fun savedAttempt(): SavedAttempt? {
        val id = prefs.getString("active_id", "").orEmpty()
        val key = prefs.getString("active_key", "").orEmpty()
        val baseUrl = prefs.getString("active_url", "").orEmpty()
        val code = prefs.getString("active_code", "").orEmpty()
        return if (id.isBlank() || key.isBlank() || baseUrl.isBlank()) null else SavedAttempt(baseUrl, code, id, key)
    }

    @Synchronized fun clearAttempt() {
        prefs.edit().remove("active_url").remove("active_code").remove("active_id")
            .remove("active_key").remove("active_pending").apply()
    }

    @Synchronized fun pending(): Map<String, String> {
        val json = runCatching { JSONObject(prefs.getString("active_pending", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val data = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            data[key] = json.optString(key, "")
        }
        return data
    }

    @Synchronized fun stageAnswer(questionId: String, value: String) {
        val pending = JSONObject(prefs.getString("active_pending", "{}") ?: "{}")
        pending.put(questionId, value)
        prefs.edit().putString("active_pending", pending.toString()).commit()
    }

    /** Hapus hanya ketika nilai yang diterima server sama dengan nilai terakhir siswa. */
    @Synchronized fun acknowledge(questionId: String, acknowledged: String) {
        val pending = JSONObject(prefs.getString("active_pending", "{}") ?: "{}")
        if (pending.has(questionId) && pending.optString(questionId) == acknowledged) {
            pending.remove(questionId)
            prefs.edit().putString("active_pending", pending.toString()).commit()
        }
    }

    @Synchronized fun logIncident(type: String) {
        val current = prefs.getString("incidents", "").orEmpty().lineSequence().filter { it.isNotBlank() }.toList().takeLast(49)
        val next = (current + "${System.currentTimeMillis()}:$type").joinToString("\n")
        prefs.edit().putString("incidents", next).apply()
    }
}
