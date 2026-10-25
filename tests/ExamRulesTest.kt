package id.mysmartclass.smartexam

private inline fun shouldFail(test: () -> Unit) {
    var failed = false
    try { test() } catch (_: IllegalArgumentException) { failed = true }
    check(failed) { "Kasus yang seharusnya ditolak justru diterima." }
}

fun main() {
    check(ExamRules.normalizeServer("http://192.168.1.8:3210/ujian-siswa", ExamConnectionMode.LAN) == "http://192.168.1.8:3210")
    check(ExamRules.normalizeServer("http://10.10.2.1:3210/ujian-siswa?kode=MATEMATIKA", ExamConnectionMode.LAN) == "http://10.10.2.1:3210")
    check(ExamRules.normalizeServer("https://ujian.sekolah.id", ExamConnectionMode.ONLINE) == "https://ujian.sekolah.id")
    check(ExamRules.normalizeServer("http://172.16.0.1:3210", ExamConnectionMode.LAN) == "http://172.16.0.1:3210")
    shouldFail { ExamRules.normalizeServer("http://ujian.example.com", ExamConnectionMode.ONLINE) }
    shouldFail { ExamRules.normalizeServer("http://8.8.8.8:3210", ExamConnectionMode.LAN) }
    shouldFail { ExamRules.normalizeServer("http://127.0.0.1:3210", ExamConnectionMode.LAN) }
    shouldFail { ExamRules.normalizeServer("http://192.168.1.10:3210/other", ExamConnectionMode.LAN) }
    shouldFail { ExamRules.normalizeServer("http://192.168.1.10:3210/ujian-siswa?redirect=https://wrong.com", ExamConnectionMode.LAN) }
    shouldFail { ExamRules.normalizeServer("https://school.local", ExamConnectionMode.ONLINE) }
    check(ExamRules.codeFromExamLink("http://192.168.1.8:3210/ujian-siswa?kode=ABCD12") == "ABCD12")
    check(ExamRules.codeFromExamLink("http://192.168.1.8:3210/") == null)
    check(ExamRules.cleanCode(" ab 12 cd ") == "AB12CD")
    check(ExamRules.isPrivateIpv4("192.168.0.20"))
    check(!ExamRules.isPrivateIpv4("192.169.0.20"))
    check(ExamRules.unansweredNumbers(listOf("q1", "q2", "q3"), mapOf("q1" to "A", "q2" to "  ")) == listOf(2, 3))
    check(ExamRules.answerLetter(0) == "A")
    check(ExamRules.answerLetter(3) == "D")
    check(ExamRules.formatTime(3661) == "01:01:01")
    check(ExamRules.formatTime(-10) == "00:00:00")
    println("LULUS: 18+ pemeriksaan aturan dasar My SmartExam.")
}
