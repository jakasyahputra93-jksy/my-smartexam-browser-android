package id.mysmartclass.smartexam

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

/** Aturan yang dapat diuji tanpa emulator atau Android SDK. */
enum class ExamConnectionMode { LAN, ONLINE }

object ExamRules {
    fun cleanCode(value: String): String = value.uppercase(Locale.ROOT).replace(Regex("\\s+"), "").trim()

    fun normalizeServer(rawAddress: String, mode: ExamConnectionMode): String {
        val raw = rawAddress.trim().trimEnd('/')
        require(raw.isNotEmpty()) { "Alamat server belum diisi." }
        val uri = try { URI(raw) } catch (_: Exception) { null }
        require(uri != null) { "Format alamat server tidak benar." }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT)
        require(scheme == "http" || scheme == "https") { "Gunakan alamat http:// atau https://." }
        require(!host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) {
            "Alamat server tidak valid."
        }
        val path = uri.path.orEmpty().trimEnd('/')
        require(uri.query == null || (path == "/ujian-siswa" && uri.query.matches(Regex("(?i)(kode|code)=[A-Za-z0-9_-]{4,24}")))) {
            "Tautan hanya boleh memuat parameter kode ujian."
        }
        require(path.isEmpty() || path == "/ujian-siswa") {
            "Gunakan alamat utama server atau tautan /ujian-siswa dari guru."
        }
        require(uri.port in -1..65535 && uri.port != 0) { "Nomor port tidak valid." }
        if (mode == ExamConnectionMode.LAN) {
            require(isPrivateIpv4(host)) {
                "Mode LAN hanya menerima alamat IP privat, seperti 192.168.1.10:3210."
            }
        } else {
            require(scheme == "https") { "Mode Online wajib menggunakan HTTPS." }
            require(!isAnyIpv4(host) || !isPrivateIpv4(host)) {
                "Alamat IP lokal tidak boleh digunakan untuk mode Online."
            }
            require(host != "localhost" && !host.endsWith(".local")) {
                "Mode Online memerlukan domain server publik dengan HTTPS."
            }
        }
        return "$scheme://$host${if (uri.port == -1) "" else ":${uri.port}"}"
    }

    fun codeFromExamLink(raw: String): String? {
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
        if (uri.path?.trimEnd('/') != "/ujian-siswa") return null
        val query = uri.rawQuery.orEmpty()
        val pair = query.split('=', limit = 2)
        if (pair.size != 2 || pair[0].lowercase(Locale.ROOT) !in setOf("kode", "code")) return null
        return cleanCode(URLDecoder.decode(pair[1], "UTF-8")).takeIf { it.length in 4..24 }
    }

    private fun isAnyIpv4(host: String): Boolean {
        val segments = host.split('.')
        return segments.size == 4 && segments.all { p -> p.isNotEmpty() && p.all(Char::isDigit) && (p.toIntOrNull() ?: 256) in 0..255 }
    }

    fun isPrivateIpv4(host: String): Boolean {
        if (!isAnyIpv4(host)) return false
        val parts = host.split('.').map(String::toInt)
        return (parts[0] == 10) ||
            (parts[0] == 172 && parts[1] in 16..31) ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 100 && parts[1] in 64..127) // CGNAT jaringan privat operator, jika dipakai sebagai LAN
    }

    fun unansweredNumbers(questionIds: List<String>, answers: Map<String, String>): List<Int> =
        questionIds.mapIndexedNotNull { index, id -> if (answers[id].orEmpty().trim().isEmpty()) index + 1 else null }

    fun answerLetter(index: Int): String {
        require(index in 0..25) { "Pilihan jawaban tidak valid." }
        return ('A'.code + index).toChar().toString()
    }

    fun formatTime(seconds: Long): String {
        val value = seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(Locale.ROOT, value / 3600, (value % 3600) / 60, value % 60)
    }
}
