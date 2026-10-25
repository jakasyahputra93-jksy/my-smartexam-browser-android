package id.mysmartclass.smartexam

import org.json.JSONObject

// Nama properti sengaja mengikuti kontrak API lokal My SmartClass RC6.19.14.
data class ExamParticipant(val id: String, val name: String, val label: String)

data class PublicExamInfo(
    val token: String,
    val title: String,
    val subjectName: String,
    val className: String,
    val durationMinutes: Int,
    val questionCount: Int,
    val participants: List<ExamParticipant>
) {
    companion object {
        fun fromJson(json: JSONObject): PublicExamInfo {
            val exam = json.getJSONObject("exam")
            val array = json.getJSONArray("participants")
            val students = (0 until array.length()).map { i ->
                val student = array.getJSONObject(i)
                ExamParticipant(
                    id = student.getString("id"),
                    name = student.getString("name"),
                    label = student.optString("label", student.getString("name"))
                )
            }
            return PublicExamInfo(
                token = exam.getString("token"),
                title = exam.getString("title"),
                subjectName = exam.optString("subjectName", ""),
                className = exam.optString("className", ""),
                durationMinutes = exam.optInt("durationMinutes", 60),
                questionCount = exam.optInt("questionCount", 0),
                participants = students
            )
        }
    }
}

data class ExamQuestion(
    val id: String,
    val type: String,
    val prompt: String,
    val options: List<String>
) {
    companion object {
        fun fromJson(json: JSONObject): ExamQuestion {
            val array = json.optJSONArray("options")
            val options = if (array == null) emptyList() else (0 until array.length()).map { array.getString(it) }
            return ExamQuestion(json.getString("id"), json.optString("type", "ESSAY"), json.getString("prompt"), options)
        }
    }
}

data class AttemptData(
    val id: String,
    val accessKey: String,
    val status: String,
    val attemptNumber: Int,
    val remainingSeconds: Long,
    val studentName: String,
    val title: String,
    val subjectName: String,
    val className: String,
    val schoolName: String,
    val questions: List<ExamQuestion>,
    val answers: MutableMap<String, String>
) {
    companion object {
        fun fromJson(json: JSONObject): AttemptData {
            val attempt = json.getJSONObject("attempt")
            val exam = json.getJSONObject("exam")
            val student = json.getJSONObject("student")
            val school = json.getJSONObject("school")
            val qArray = json.getJSONArray("questions")
            val questions = (0 until qArray.length()).map { ExamQuestion.fromJson(qArray.getJSONObject(it)) }
            val answers = mutableMapOf<String, String>()
            val raw = json.optJSONObject("answers") ?: JSONObject()
            val keys = raw.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                answers[key] = raw.optString(key, "")
            }
            return AttemptData(
                id = attempt.getString("id"),
                accessKey = attempt.getString("accessKey"),
                status = attempt.getString("status"),
                attemptNumber = attempt.optInt("attemptNumber", 1),
                remainingSeconds = json.optLong("remainingSeconds", 0).coerceAtLeast(0),
                studentName = student.optString("name", "Peserta"),
                title = exam.getString("title"),
                subjectName = exam.optString("subjectName", ""),
                className = exam.optString("className", ""),
                schoolName = school.optString("name", "My SmartClass"),
                questions = questions,
                answers = answers
            )
        }
    }
}
