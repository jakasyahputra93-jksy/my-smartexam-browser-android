package id.mysmartclass.smartexam

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * My SmartExam Browser v0.1.0
 * Aplikasi native yang menggunakan API ujian LAN RC6.19.14 tanpa mengganti server.
 * Online memerlukan backend cloud kompatibel, bukan sekadar mengubah alamat.
 */
class MainActivity : Activity() {
    private enum class Screen { SETUP, PICK_STUDENT, PIN_GATE, EXAM, BUSY, FINISHED }

    private lateinit var ui: UiKit
    private lateinit var store: SessionStore
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var screen = Screen.SETUP
    private var mode = ExamConnectionMode.LAN
    private var server = ""
    private var code = ""
    private var api: ExamApi? = null
    private var publicInfo: PublicExamInfo? = null
    private var exam: AttemptData? = null
    private var currentIndex = 0
    private val flaggedIds = mutableSetOf<String>()

    private var expiresAtElapsed = 0L
    private var timeoutHandled = false
    private var isSyncing = false
    private var submitting = false
    private var errorMessage = ""
    private var saveMessage = ""
    private var timerView: TextView? = null
    private var progressView: TextView? = null
    private var saveView: TextView? = null
    private var essayDebounce: Runnable? = null
    private var lastResumeCheck = 0L

    private val ticker = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                val a = exam
                if (a?.status == "ACTIVE") {
                    updateCounters()
                    if (remainingSeconds() == 0L && !timeoutHandled && !submitting) {
                        timeoutHandled = true
                        submitAttempt(automatic = true)
                    }
                    if (SystemClock.elapsedRealtime() - lastResumeCheck > 12_000L && !submitting) {
                        lastResumeCheck = SystemClock.elapsedRealtime()
                        syncPending()
                    }
                }
                mainHandler.postDelayed(this, 1_000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 35) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val edges = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(0, edges.top, 0, edges.bottom)
                insets
            }
        }
        ui = UiKit(this)
        store = SessionStore(this)
        mode = store.savedMode()
        server = store.savedServer()
        code = store.savedCode()
        renderSetup()
        mainHandler.post(ticker)
        if (store.savedAttempt() != null) restoreSession()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized && exam?.status == "ACTIVE") {
            mainHandler.postDelayed({
                if (!isFinishing && exam?.status == "ACTIVE") {
                    if (!isPinned() && screen != Screen.BUSY) renderPinGate()
                    else if (isPinned() && screen == Screen.PIN_GATE) renderExam()
                    syncPending()
                }
            }, 200)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Ini hanya indikasi gangguan fokus, bukan bukti kecurangan.
        if (::store.isInitialized && !hasFocus && exam?.status == "ACTIVE" && screen == Screen.EXAM) {
            store.logIncident("FOCUS_LOST")
        }
    }

    @Deprecated("Android masih mendukung callback Back untuk Activity ini")
    override fun onBackPressed() {
        if (exam?.status == "ACTIVE") {
            Toast.makeText(this, "Selesaikan ujian melalui tombol yang tersedia.", Toast.LENGTH_SHORT).show()
        } else if (screen == Screen.PICK_STUDENT || screen == Screen.FINISHED) {
            renderSetup()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(ticker)
        essayDebounce?.let { mainHandler.removeCallbacks(it) }
        worker.shutdown()
        super.onDestroy()
    }

    private fun basePage(title: String, subtitle: String): LinearLayout {
        timerView = null
        progressView = null
        saveView = null
        val scroller = ScrollView(this)
        scroller.isFillViewport = true
        scroller.setBackgroundColor(Palette.BG)
        val body = ui.vertical(16)
        scroller.addView(body)
        setContentView(scroller)
        val header = ui.vertical(18)
        header.background = ui.shape(Palette.NAVY, 18)
        header.addView(ui.label("MY SMARTEXAM BROWSER", 12f, 0xFFBDE2EA.toInt(), true))
        header.addView(ui.spaced(ui.label(title, 23f, 0xFFFFFFFF.toInt(), true), 5))
        header.addView(ui.spaced(ui.label(subtitle, 13f, 0xFFDBE8F3.toInt()), 5))
        body.addView(header)
        return body
    }

    private fun message(body: LinearLayout, text: String, danger: Boolean = false) {
        if (text.isBlank()) return
        val notice = ui.label(text, 13f, if (danger) Palette.RED else Palette.MUTED)
        body.addView(ui.spaced(notice, 10))
    }

    private fun renderSetup() {
        screen = Screen.SETUP
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val body = basePage("Siap mengikuti ujian?", "Hubungkan HP ke server ujian My SmartClass.")
        val card = ui.card()
        body.addView(ui.spaced(card, 14))
        card.addView(ui.label("1. Pilih jalur ujian", 16f, Palette.NAVY, true))

        val choice = RadioGroup(this)
        choice.orientation = RadioGroup.HORIZONTAL
        val local = RadioButton(this).apply { id = 101; text = "LAN Sekolah"; textSize = 14f }
        val online = RadioButton(this).apply { id = 102; text = "Online"; textSize = 14f }
        choice.addView(local)
        choice.addView(online)
        choice.check(if (mode == ExamConnectionMode.LAN) 101 else 102)
        card.addView(ui.spaced(choice, 7))

        card.addView(ui.spaced(ui.label("2. Alamat server", 16f, Palette.NAVY, true), 15))
        val serverField = ui.textField("http://192.168.1.10:3210", server)
        card.addView(ui.spaced(serverField, 7))
        message(card, "Untuk LAN, salin alamat /ujian-siswa dari menu Ujian pada komputer guru. Contoh: http://192.168.1.10:3210/ujian-siswa")

        card.addView(ui.spaced(ui.label("3. Kode ujian", 16f, Palette.NAVY, true), 15))
        val codeField = ui.textField("Kode dari guru", code)
        card.addView(ui.spaced(codeField, 7))
        message(card, "Mode Online memerlukan server HTTPS yang mendukung API Ujian Hybrid. Layanan cloud belum tersedia dalam baseline lama.")

        val check = ui.button("Periksa Kode Ujian") {
            val selectedMode = if (choice.checkedRadioButtonId == 102) ExamConnectionMode.ONLINE else ExamConnectionMode.LAN
            val normalized = try { ExamRules.normalizeServer(serverField.text.toString(), selectedMode) }
            catch (e: IllegalArgumentException) { showAlert("Alamat belum benar", e.message ?: "Periksa alamat server."); return@button }
            if (selectedMode == ExamConnectionMode.ONLINE) {
                showAlert("Mode Online belum tersedia", "APK sudah menyiapkan pilihan Online, tetapi backend Ujian Hybrid My SmartClass belum dibangun. Untuk saat ini gunakan LAN Sekolah.")
                return@button
            }
            val clean = ExamRules.cleanCode(codeField.text.toString().ifBlank { ExamRules.codeFromExamLink(serverField.text.toString()).orEmpty() })
            if (clean.length < 4) { showAlert("Kode belum lengkap", "Kode ujian minimal 4 karakter."); return@button }
            if (store.savedAttempt() != null && store.pending().isNotEmpty()) {
                showAlert("Jawaban belum terkirim", "Ada jawaban lokal dari sesi sebelumnya. Pulihkan sesi tersebut sebelum membuat sesi baru.")
                return@button
            }
            mode = selectedMode
            server = normalized
            code = clean
            store.saveSettings(mode, server, code)
            api = ExamApi(server)
            checkCode()
        }
        card.addView(ui.spaced(check, 16))

        if (store.savedAttempt() != null) {
            val resume = ui.button("Pulihkan Ujian Sebelumnya", false) { restoreSession() }
            body.addView(ui.spaced(resume, 12))
        }
        if (errorMessage.isNotBlank()) message(body, errorMessage, true)
        message(body, "Penguncian Screen Pinning memerlukan persetujuan Android. Pemilik HP tetap dapat melepaskan sematan sesuai aturan perangkat.")
    }

    private fun checkCode() {
        renderBusy("Memeriksa kode ujian", "Menghubungi server My SmartClass...")
        val client = api ?: return
        val requestedCode = code
        worker.execute {
            val result = runCatching { client.examInfo(requestedCode) }
            runOnUiThread {
                result.onSuccess {
                    publicInfo = it
                    errorMessage = ""
                    renderStudentSelection()
                }.onFailure {
                    errorMessage = it.message ?: "Server belum bisa dihubungi."
                    renderSetup()
                }
            }
        }
    }

    private fun renderStudentSelection() {
        val info = publicInfo ?: return renderSetup()
        screen = Screen.PICK_STUDENT
        val body = basePage("Pilih peserta ujian", "Pastikan nama dan kelas sesuai dengan data guru.")
        val card = ui.card()
        body.addView(ui.spaced(card, 14))
        card.addView(ui.label(info.title, 19f, Palette.NAVY, true))
        card.addView(ui.spaced(ui.label("${info.subjectName}  •  ${info.className}", 14f, Palette.MUTED), 6))
        card.addView(ui.spaced(ui.label("${info.questionCount} soal  •  ${info.durationMinutes} menit", 14f), 6))
        card.addView(ui.spaced(ui.label("Nama peserta", 15f, Palette.NAVY, true), 16))
        val spinner = Spinner(this)
        val names = listOf("Pilih nama siswa") + info.participants.map { it.label }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        card.addView(ui.spaced(spinner, 7))
        val action = ui.button("Masuk dan Aktifkan Penguncian") {
            val selected = spinner.selectedItemPosition - 1
            if (selected !in info.participants.indices) {
                showAlert("Pilih peserta", "Pilih nama siswa yang sesuai terlebih dahulu.")
                return@button
            }
            joinExam(info.participants[selected])
        }
        card.addView(ui.spaced(action, 16))
        body.addView(ui.spaced(ui.button("Kembali ke Pengaturan", false) { renderSetup() }, 8))
        message(body, "Setelah masuk, Android akan meminta izin untuk menyematkan aplikasi. Urutan soal berasal dari server dan tetap konsisten ketika melanjutkan sesi.")
    }

    private fun joinExam(participant: ExamParticipant) {
        renderBusy("Membuka soal", "Mempersiapkan percobaan ujian dan jawaban siswa...")
        val client = api ?: return renderSetup()
        val requestedCode = code
        worker.execute {
            val result = runCatching { client.join(requestedCode, participant) }
            runOnUiThread {
                result.onSuccess { payload ->
                    if (payload.questions.isEmpty()) {
                        showAlert("Soal kosong", "Paket ujian belum berisi soal. Hubungi guru.")
                        renderStudentSelection()
                        return@onSuccess
                    }
                    store.saveAttempt(server, code, payload)
                    activateAttempt(payload, fromRestore = false)
                }.onFailure {
                    showAlert("Gagal masuk ujian", it.message ?: "Periksa koneksi dan hubungi guru.")
                    renderStudentSelection()
                }
            }
        }
    }

    private fun restoreSession() {
        val saved = store.savedAttempt() ?: return renderSetup()
        renderBusy("Memulihkan sesi", "Mengambil jawaban dan sisa waktu dari server...")
        worker.execute {
            val result = runCatching { ExamApi(saved.baseUrl).resume(saved.id, saved.key) }
            runOnUiThread {
                result.onSuccess { payload ->
                    server = saved.baseUrl
                    code = saved.code
                    api = ExamApi(server)
                    activateAttempt(payload, fromRestore = true)
                }.onFailure {
                    errorMessage = "Sesi lama belum bisa dipulihkan: ${it.message}. Jangan hapus data aplikasi jika ada jawaban yang belum terkirim."
                    renderSetup()
                }
            }
        }
    }

    private fun activateAttempt(payload: AttemptData, fromRestore: Boolean) {
        exam = payload
        currentIndex = 0
        flaggedIds.clear()
        timeoutHandled = false
        expiresAtElapsed = SystemClock.elapsedRealtime() + payload.remainingSeconds * 1_000L
        if (fromRestore) {
            val validIds = payload.questions.map { it.id }.toSet()
            for ((questionId, answer) in store.pending()) {
                if (questionId in validIds) payload.answers[questionId] = answer
            }
        }
        if (payload.status == "ACTIVE") {
            renderPinGate()
            if (!fromRestore) requestPinning()
        } else {
            renderFinished()
        }
    }

    private fun renderPinGate() {
        val attempt = exam ?: return renderSetup()
        if (attempt.status != "ACTIVE") return renderFinished()
        if (isPinned()) return renderExam()
        screen = Screen.PIN_GATE
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val body = basePage("Aktifkan mode ujian", "Aplikasi perlu disematkan sebelum soal ditampilkan.")
        val card = ui.card()
        body.addView(ui.spaced(card, 14))
        card.addView(ui.label("Perlindungan layar ujian", 19f, Palette.NAVY, true))
        card.addView(ui.spaced(ui.label("Android akan meminta Anda menyetujui Screen Pinning. Setelah aktif, soal ujian terbuka. Anda tetap bisa melepaskan pin sesuai aturan Android.", 14f), 9))
        card.addView(ui.spaced(ui.button("Sematkan Aplikasi Sekarang") { requestPinning() }, 14))
        card.addView(ui.spaced(ui.button("Buka Pengaturan Keamanan", false) {
            try { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            catch (_: Exception) { showAlert("Pengaturan tidak tersedia", "Cari fitur Sematkan Layar atau Screen Pinning di Pengaturan Android.") }
        }, 8))
        message(body, "Bila layar tidak dapat disematkan, aktifkan fitur Sematkan Layar melalui Pengaturan > Keamanan pada HP. Nama menu dapat berbeda menurut merek HP.")
    }

    private fun requestPinning() {
        if (exam?.status != "ACTIVE") return
        if (isPinned()) { renderExam(); return }
        try { startLockTask() }
        catch (_: Exception) { showAlert("Screen Pinning belum aktif", "Aktifkan fitur Sematkan Layar di Pengaturan Android, lalu coba kembali.") }
        mainHandler.postDelayed({
            if (!isFinishing && exam?.status == "ACTIVE" && isPinned()) renderExam()
        }, 850)
    }

    private fun isPinned(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_PINNED ||
            am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED
    }

    private fun renderExam() {
        val a = exam ?: return renderSetup()
        if (a.status != "ACTIVE") return renderFinished()
        if (!isPinned()) return renderPinGate()
        screen = Screen.EXAM
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val question = a.questions.getOrNull(currentIndex) ?: return
        val body = basePage(a.title, "${a.studentName}  •  Percobaan ${a.attemptNumber}")
        val statusCard = ui.card(14)
        body.addView(ui.spaced(statusCard, 12))
        timerView = ui.label("00:00:00", 25f, Palette.NAVY, true)
        statusCard.addView(ui.label("Sisa waktu", 12f, Palette.MUTED))
        statusCard.addView(timerView)
        progressView = ui.label("", 13f, Palette.MUTED)
        statusCard.addView(ui.spaced(progressView!!, 6))
        saveView = ui.label("", 12f, Palette.TEAL)
        statusCard.addView(ui.spaced(saveView!!, 4))
        updateCounters()

        val card = ui.card()
        body.addView(ui.spaced(card, 13))
        val kind = if (question.type == "MULTIPLE_CHOICE") "Pilihan Ganda" else "Uraian"
        card.addView(ui.label("SOAL ${currentIndex + 1} / ${a.questions.size}  •  $kind", 12f, Palette.TEAL, true))
        card.addView(ui.spaced(ui.label(question.prompt, 18f, Palette.TEXT, true), 12))
        if (question.type == "MULTIPLE_CHOICE") {
            question.options.forEachIndexed { idx, option ->
                val letter = ExamRules.answerLetter(idx)
                val selected = a.answers[question.id] == letter
                val choice = ui.button("$letter.  $option", false) {
                    recordAnswer(question.id, letter, debounce = false)
                    renderExam()
                }
                choice.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                choice.backgroundTintList = ColorStateList.valueOf(if (selected) Palette.LIGHT_TEAL else 0xFFF0F5F8.toInt())
                choice.setTextColor(if (selected) Palette.TEAL else Palette.TEXT)
                card.addView(ui.spaced(choice, 8))
            }
        } else {
            val essay = ui.textField("Tulis jawaban di sini", a.answers[question.id].orEmpty(), multiline = true)
            essay.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    recordAnswer(question.id, s?.toString().orEmpty(), debounce = true)
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            card.addView(ui.spaced(essay, 12))
        }
        val navRow = ui.horizontal()
        val previous = ui.button("Sebelumnya", false) {
            currentIndex = (currentIndex - 1).coerceAtLeast(0)
            renderExam()
        }
        previous.isEnabled = currentIndex > 0
        navRow.addView(previous, LinearLayout.LayoutParams(0, ui.dp(50), 1f))
        val next = ui.button("Berikutnya", true) {
            currentIndex = (currentIndex + 1).coerceAtMost(a.questions.lastIndex)
            renderExam()
        }
        next.isEnabled = currentIndex < a.questions.lastIndex
        val nextParams = LinearLayout.LayoutParams(0, ui.dp(50), 1f)
        nextParams.leftMargin = ui.dp(8)
        navRow.addView(next, nextParams)
        body.addView(ui.spaced(navRow, 13))

        val questionCard = ui.card(12)
        questionCard.addView(ui.label("Navigasi Soal", 15f, Palette.NAVY, true))
        val horizontal = HorizontalScrollView(this)
        val numberRow = ui.horizontal()
        a.questions.forEachIndexed { idx, q ->
            val filled = a.answers[q.id].orEmpty().trim().isNotEmpty()
            val button = ui.button("${idx + 1}", false) {
                currentIndex = idx
                renderExam()
            }
            button.minWidth = ui.dp(50)
            button.backgroundTintList = ColorStateList.valueOf(
                if (idx == currentIndex) Palette.TEAL else if (filled) Palette.LIGHT_TEAL else 0xFFE9F0F6.toInt()
            )
            button.setTextColor(if (idx == currentIndex) 0xFFFFFFFF.toInt() else Palette.TEXT)
            val p = LinearLayout.LayoutParams(ui.dp(53), ui.dp(49))
            p.rightMargin = ui.dp(5)
            numberRow.addView(button, p)
        }
        horizontal.addView(numberRow)
        questionCard.addView(ui.spaced(horizontal, 10))
        body.addView(ui.spaced(questionCard, 12))
        body.addView(ui.spaced(ui.button("Selesai dan Kumpulkan Ujian") { askSubmitConfirmation() }, 14))
        message(body, "Jawaban tersimpan ke server secara berkala. Jika sinyal terganggu, aplikasi menyimpan antrean jawaban lokal dan mencoba mengirim ulang.")
    }

    private fun updateCounters() {
        val a = exam ?: return
        val waiting = store.pending().size
        val filled = a.questions.size - ExamRules.unansweredNumbers(a.questions.map { it.id }, a.answers).size
        timerView?.text = ExamRules.formatTime(remainingSeconds())
        progressView?.text = "Terjawab $filled dari ${a.questions.size} soal"
        saveView?.text = when {
            submitting -> "Sedang mengumpulkan ujian..."
            isSyncing -> "Menyimpan jawaban ke server..."
            waiting > 0 -> "$waiting jawaban menunggu sinkronisasi${if (saveMessage.isNotBlank()) ". Periksa koneksi." else ""}"
            else -> "Semua perubahan sudah tersimpan"
        }
        saveView?.setTextColor(if (waiting > 0) Palette.AMBER else Palette.TEAL)
    }

    private fun remainingSeconds(): Long {
        val difference = expiresAtElapsed - SystemClock.elapsedRealtime()
        return ((difference + 999L) / 1_000L).coerceAtLeast(0)
    }

    private fun recordAnswer(questionId: String, answer: String, debounce: Boolean) {
        val a = exam ?: return
        if (a.status != "ACTIVE" || submitting) return
        if (a.answers[questionId] == answer) return
        a.answers[questionId] = answer
        store.stageAnswer(questionId, answer)
        updateCounters()
        essayDebounce?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable { syncPending() }
        essayDebounce = task
        mainHandler.postDelayed(task, if (debounce) 900L else 100L)
    }

    private fun syncPending() {
        val a = exam ?: return
        val client = api ?: return
        if (a.status != "ACTIVE" || submitting || isSyncing || store.pending().isEmpty()) return
        isSyncing = true
        updateCounters()
        worker.execute {
            val pending = store.pending()
            var failure: Exception? = null
            for ((questionId, answer) in pending) {
                try {
                    client.saveAnswer(a.id, a.accessKey, questionId, answer)
                    store.acknowledge(questionId, answer)
                } catch (e: Exception) {
                    failure = e
                    break
                }
            }
            runOnUiThread {
                isSyncing = false
                saveMessage = failure?.message ?: ""
                updateCounters()
            }
        }
    }

    private fun askSubmitConfirmation() {
        val a = exam ?: return
        if (submitting) return
        val emptyNumbers = ExamRules.unansweredNumbers(a.questions.map { it.id }, a.answers)
        val detail = if (emptyNumbers.isEmpty()) {
            "Semua ${a.questions.size} soal telah dijawab. Anda yakin ingin mengumpulkan ujian?"
        } else {
            val list = emptyNumbers.take(18).joinToString(", ")
            "Masih ada ${emptyNumbers.size} soal kosong. Nomor: $list${if (emptyNumbers.size > 18) ", ..." else ""}.\n\nKembali untuk memeriksa, atau tetap kumpulkan jika sudah yakin."
        }
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Selesai Ujian")
            .setMessage(detail)
            .setNegativeButton("Kembali Mengerjakan", null)
            .setPositiveButton("Tetap Kumpulkan") { _, _ -> submitAttempt(automatic = false) }
            .show()
    }

    private fun submitAttempt(automatic: Boolean) {
        val a = exam ?: return
        val client = api ?: return
        if (a.status != "ACTIVE" || submitting) return
        submitting = true
        renderBusy("Mengirim seluruh jawaban", if (automatic) "Waktu habis. Menghubungi server..." else "Memastikan jawaban tersimpan sebelum menyelesaikan ujian...")
        worker.execute {
            val result = runCatching {
                val waiting = store.pending()
                for ((questionId, answer) in waiting) {
                    client.saveAnswer(a.id, a.accessKey, questionId, answer)
                    store.acknowledge(questionId, answer)
                }
                if (store.pending().isNotEmpty()) throw IllegalStateException("Masih ada jawaban yang belum tersinkronisasi.")
                client.submit(a.id, a.accessKey)
            }
            runOnUiThread {
                submitting = false
                result.onSuccess { status ->
                    exam = a.copy(status = status)
                    if (store.pending().isEmpty()) store.clearAttempt()
                    stopPinningSafely()
                    renderFinished()
                }.onFailure { error ->
                    errorMessage = "Pengumpulan gagal: ${error.message}. Jawaban lokal yang tertunda tetap tersimpan."
                    showAlert("Belum dapat dikumpulkan", errorMessage)
                    if (isPinned()) renderExam() else renderPinGate()
                }
            }
        }
    }

    private fun renderFinished() {
        val a = exam ?: return renderSetup()
        screen = Screen.FINISHED
        stopPinningSafely()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val body = basePage(if (a.status == "EXPIRED") "Waktu ujian berakhir" else "Ujian berhasil dikumpulkan", "${a.studentName}  •  Percobaan ${a.attemptNumber}")
        val card = ui.card()
        body.addView(ui.spaced(card, 14))
        card.addView(ui.label(a.title, 19f, Palette.NAVY, true))
        val answered = a.questions.size - ExamRules.unansweredNumbers(a.questions.map { it.id }, a.answers).size
        card.addView(ui.spaced(ui.label("Terjawab $answered dari ${a.questions.size} soal", 16f), 12))
        card.addView(ui.spaced(ui.label("Semua percobaan lama tetap tersimpan di My SmartClass. Ujian ulang akan membuat percobaan baru dan urutan soal berasal dari pengacakan server.", 14f, Palette.MUTED), 10))
        if (store.pending().isNotEmpty()) {
            message(card, "Perhatian: masih ada jawaban lokal yang belum sampai ke server. Hubungi pengawas sebelum melakukan ujian ulang.", true)
        } else {
            card.addView(ui.spaced(ui.button("Ujian Ulang / Remedial") {
                exam = null
                publicInfo = null
                checkCode()
            }, 15))
        }
        body.addView(ui.spaced(ui.button("Kembali ke Halaman Awal", false) { exam = null; renderSetup() }, 10))
    }

    private fun stopPinningSafely() {
        if (isPinned()) {
            try { stopLockTask() } catch (_: Exception) { /* Android mengelola keputusan akhir */ }
        }
    }

    private fun renderBusy(title: String, detail: String) {
        screen = Screen.BUSY
        val body = basePage(title, detail)
        val card = ui.card(28)
        val progress = ProgressBar(this)
        card.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(55)))
        card.addView(ui.spaced(ui.centered("Jangan tutup aplikasi sebelum proses selesai."), 9))
        body.addView(ui.spaced(card, 16))
    }

    private fun showAlert(title: String, message: String) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Mengerti", null).show()
    }
}
