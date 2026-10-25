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
import android.widget.ImageView
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * My SmartExam Browser v0.3.0, companion resmi My SmartClass.
 * LAN mempertahankan kontrak lokal, Online memakai Cloud v0.4.1 dan daftar nama.
 * Screen Pinning adalah perlindungan best-effort pada HP pribadi, bukan kiosk MDM.
 */
class MainActivity : Activity() {
    private enum class Screen { WELCOME, SETUP, PICK_STUDENT, PIN_GATE, EXAM, BUSY, FINISHED }

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
        server = if (mode == ExamConnectionMode.ONLINE) ExamRules.CLOUD_BASE_URL else store.savedServer()
        code = store.savedCode()
        if (store.savedAttempt() != null) {
            renderBusy("Memulihkan ujian", "Menghubungkan kembali sesi yang telah tersimpan...")
            restoreSession()
        } else {
            renderWelcome()
        }
        mainHandler.post(ticker)
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
        } else if (screen == Screen.SETUP) {
            renderWelcome()
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

    private fun renderWelcome() {
        screen = Screen.WELCOME
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        timerView = null
        progressView = null
        saveView = null
        val scroller = ScrollView(this)
        scroller.isFillViewport = true
        scroller.setBackgroundColor(Palette.BG)
        val root = ui.vertical(24)
        root.gravity = Gravity.CENTER_HORIZONTAL
        scroller.addView(root)
        setContentView(scroller)

        val hero = ui.card(22)
        hero.gravity = Gravity.CENTER_HORIZONTAL
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.mysmartclass_logo)
            contentDescription = "Logo resmi My SmartClass"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        hero.addView(logo, LinearLayout.LayoutParams(ui.dp(120), ui.dp(120)))
        hero.addView(ui.spaced(ui.centered("MY SMARTCLASS ECOSYSTEM", 12f, Palette.DEEP_MAGENTA), 16))
        hero.addView(ui.spaced(ui.centered("My SmartExam Browser", 25f, Palette.NAVY).apply {
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, 8))
        hero.addView(ui.spaced(ui.centered("Aplikasi ujian digital untuk siswa", 14f, Palette.MUTED), 8))
        root.addView(ui.spaced(hero, 24))

        val card = ui.card(18)
        card.addView(ui.label("Selamat Datang!", 20f, Palette.NAVY, true))
        card.addView(ui.spaced(ui.label("Gunakan kode ujian dari guru dan pilih nama Anda. Aplikasi mendukung jaringan sekolah atau internet.", 14f, Palette.MUTED), 10))
        card.addView(ui.spaced(ui.button("Mulai Ujian") { renderSetup() }, 20))
        root.addView(ui.spaced(card, 18))
        val footer = ui.vertical(6)
        footer.gravity = Gravity.CENTER_HORIZONTAL
        footer.addView(ui.centered("Dibuat & didesain Oleh:", 12f, Palette.MUTED))
        footer.addView(ui.centered("Jaka Syahputra,S.Sos.,Gr.", 13f, Palette.NAVY).apply {
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        footer.addView(ui.spaced(ui.centered("WA 081222282393  •  IG @jakasyahputra93", 11f, Palette.MUTED), 7))
        footer.addView(ui.spaced(ui.centered("My SmartExam Browser v0.3.0", 11f, Palette.MUTED), 5))
        root.addView(ui.spaced(footer, 25))
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
        val header = ui.vertical(16)
        header.background = ui.shape(Palette.NAVY, 18)
        val logoRow = ui.horizontal()
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.mysmartclass_logo)
            contentDescription = "My SmartClass"
            background = ui.shape(Palette.CARD, 9)
            setPadding(ui.dp(5), ui.dp(5), ui.dp(5), ui.dp(5))
        }
        logoRow.addView(icon, LinearLayout.LayoutParams(ui.dp(42), ui.dp(42)))
        val heading = ui.vertical(0)
        heading.addView(ui.label("MY SMARTEXAM BROWSER", 13f, 0xFFFFFFFF.toInt(), true))
        heading.addView(ui.spaced(ui.label("Ekosistem My SmartClass", 11f, 0xFFFFD9EA.toInt()), 3))
        val hp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        hp.leftMargin = ui.dp(12)
        logoRow.addView(heading, hp)
        header.addView(logoRow)
        header.addView(ui.spaced(ui.label(title, 23f, 0xFFFFFFFF.toInt(), true), 8))
        header.addView(ui.spaced(ui.label(subtitle, 13f, 0xFFFFEDF6.toInt()), 4))
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
        val body = basePage("Siap mengikuti ujian?", "Pilih jalur ujian, lalu masukkan kode dari guru.")
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

        val cloudStatus = ui.vertical(12)
        cloudStatus.background = ui.shape(Palette.LIGHT_TEAL, 12, Palette.BORDER)
        cloudStatus.addView(ui.label("Server Online Otomatis", 15f, Palette.NAVY, true))
        cloudStatus.addView(ui.spaced(ui.label("exam.mysmartclass.my.id", 13f, Palette.DEEP_MAGENTA), 5))
        cloudStatus.addView(ui.spaced(ui.label("Tidak perlu mengisi alamat atau kode peserta pribadi.", 12f, Palette.MUTED), 5))
        card.addView(ui.spaced(cloudStatus, 12))

        val lanBlock = ui.vertical(0)
        lanBlock.addView(ui.label("Alamat server LAN Sekolah", 15f, Palette.NAVY, true))
        val defaultLan = store.savedLanServer()
        val serverField = ui.textField("http://192.168.1.10:3210", defaultLan)
        lanBlock.addView(ui.spaced(serverField, 7))
        lanBlock.addView(ui.spaced(ui.label("Salin alamat /ujian-siswa dari komputer guru. HP harus tersambung ke jaringan sekolah.", 12f, Palette.MUTED), 8))
        card.addView(ui.spaced(lanBlock, 12))
        fun refreshModeFields() {
            val onlineMode = choice.checkedRadioButtonId == 102
            cloudStatus.visibility = if (onlineMode) View.VISIBLE else View.GONE
            lanBlock.visibility = if (onlineMode) View.GONE else View.VISIBLE
        }
        choice.setOnCheckedChangeListener { _, _ -> refreshModeFields() }
        refreshModeFields()

        card.addView(ui.spaced(ui.label("2. Kode ujian", 16f, Palette.NAVY, true), 16))
        val codeField = ui.textField("Kode ujian dari guru", code)
        card.addView(ui.spaced(codeField, 7))
        message(card, "Setelah kode cocok, pilih nama sendiri dari daftar peserta.")

        val check = ui.button("Periksa Kode Ujian") {
            val selectedMode = if (choice.checkedRadioButtonId == 102) ExamConnectionMode.ONLINE else ExamConnectionMode.LAN
            val normalized = try { ExamRules.baseUrlForMode(selectedMode, serverField.text.toString()) }
            catch (e: IllegalArgumentException) { showAlert("Alamat belum benar", e.message ?: "Periksa alamat server LAN."); return@button }
            val clean = ExamRules.cleanCode(codeField.text.toString().ifBlank { ExamRules.codeFromExamLink(serverField.text.toString()).orEmpty() })
            val validLength = if (selectedMode == ExamConnectionMode.ONLINE) clean.length in 5..24 else clean.length in 4..24
            if (!validLength) {
                showAlert("Kode belum lengkap", if (selectedMode == ExamConnectionMode.ONLINE) "Kode ujian online harus 5 sampai 24 karakter." else "Kode ujian LAN minimal 4 karakter.")
                return@button
            }
            if (store.savedAttempt() != null) {
                showAlert("Sesi sebelumnya masih tersimpan", "Pulihkan ujian sebelumnya terlebih dahulu. Jangan membuat sesi baru pada perangkat ini sebelum jawaban selesai tersinkron.")
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
        message(body, "Screen Pinning memerlukan persetujuan Android. Pada HP pribadi, pemilik perangkat tetap dapat melepaskan sematan sesuai aturan Android.")
        body.addView(ui.spaced(ui.button("Kembali ke Welcome Screen", false) { renderWelcome() }, 12))
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
        val body = basePage("Pilih peserta ujian", "Pastikan nama dan kelas sesuai data yang diberikan guru.")
        val card = ui.card()
        body.addView(ui.spaced(card, 14))
        card.addView(ui.label(info.title, 19f, Palette.NAVY, true))
        card.addView(ui.spaced(ui.label("${info.subjectName}  •  ${info.className}", 14f, Palette.MUTED), 6))
        card.addView(ui.spaced(ui.label("${info.questionCount} soal  •  ${info.durationMinutes} menit", 14f), 6))
        if (info.participants.isEmpty()) {
            message(card, "Belum ada peserta dalam daftar untuk ujian ini. Hubungi guru agar menerbitkan ujian dengan pilihan Login: Kode Ujian + Nama.", true)
        } else {
            card.addView(ui.spaced(ui.label("Nama peserta", 15f, Palette.NAVY, true), 16))
            val spinner = Spinner(this)
            val names = listOf("Pilih nama siswa") + info.participants.map { participant ->
                if (participant.status == "ACTIVE") "${participant.label}  •  Sedang ujian" else participant.label
            }
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
            card.addView(ui.spaced(spinner, 7))
            message(card, "Satu nama hanya boleh memiliki satu sesi ujian aktif. Jika HP mati atau berganti perangkat, minta bantuan guru pengawas.")
            val action = ui.button("Masuk dan Aktifkan Penguncian") {
                val selected = spinner.selectedItemPosition - 1
                if (selected !in info.participants.indices) {
                    showAlert("Pilih peserta", "Pilih nama siswa yang sesuai terlebih dahulu.")
                    return@button
                }
                val participant = info.participants[selected]
                if (participant.status == "ACTIVE") {
                    showAlert("Identitas sedang digunakan", "Nama ini sedang mengikuti ujian. Pulihkan sesi dari HP yang sama atau hubungi guru untuk membuka kunci peserta.")
                    return@button
                }
                joinExam(participant)
            }
            card.addView(ui.spaced(action, 16))
        }
        body.addView(ui.spaced(ui.button("Segarkan Daftar Peserta", false) { checkCode() }, 12))
        body.addView(ui.spaced(ui.button("Kembali ke Pengaturan", false) { renderSetup() }, 8))
        message(body, "Setelah masuk, Android akan meminta izin untuk menyematkan aplikasi. Urutan soal ditentukan server dan tidak berubah saat melanjutkan sesi.")
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
                    showAlert("Gagal masuk ujian", it.message ?: "Periksa koneksi atau minta guru memeriksa sesi peserta.")
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
        body.addView(ui.spaced(ui.button("Kembali ke Halaman Awal", false) { exam = null; renderWelcome() }, 10))
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
