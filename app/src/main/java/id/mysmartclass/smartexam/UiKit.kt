package id.mysmartclass.smartexam

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object Palette {
    // Dipetik dari src/app/globals.css pada baseline My SmartClass Desktop.
    const val NAVY = 0xFF71145F.toInt()
    const val DEEP_MAGENTA = 0xFF962FBF.toInt()
    const val TEAL = 0xFFD62976.toInt()
    const val BG = 0xFFFFF8FC.toInt()
    const val CARD = Color.WHITE
    const val TEXT = 0xFF38172E.toInt()
    const val MUTED = 0xFF7B6473.toInt()
    const val BORDER = 0xFFEBD7E3.toInt()
    const val RED = 0xFFB4233B.toInt()
    const val AMBER = 0xFF92400E.toInt()
    const val LIGHT_TEAL = 0xFFFFEAF4.toInt()
}

class UiKit(private val activity: Activity) {
    fun dp(value: Int) = (value * activity.resources.displayMetrics.density + 0.5f).toInt()
    fun shape(color: Int, radius: Int = 14, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        strokeColor?.let { setStroke(dp(1), it) }
    }
    fun vertical(padding: Int = 0) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
    }
    fun horizontal(padding: Int = 0) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
    }
    fun label(value: String, size: Float = 15f, color: Int = Palette.TEXT, bold: Boolean = false): TextView =
        TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    fun textField(hintText: String, value: String, multiline: Boolean = false): EditText =
        EditText(activity).apply {
            hint = hintText
            setText(value)
            textSize = 16f
            setTextColor(Palette.TEXT)
            setHintTextColor(Palette.MUTED)
            background = shape(Palette.CARD, 12, Palette.BORDER)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            if (multiline) {
                minLines = 5
                gravity = Gravity.TOP
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            } else {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setSingleLine(true)
            }
        }
    fun button(title: String, main: Boolean = true, click: () -> Unit): Button = Button(activity).apply {
        text = title
        isAllCaps = false
        textSize = 15f
        setTextColor(if (main) Color.WHITE else Palette.NAVY)
        backgroundTintList = ColorStateList.valueOf(if (main) Palette.TEAL else 0xFFF6EAF1.toInt())
        minHeight = dp(48)
        setOnClickListener { click() }
    }
    fun card(padding: Int = 16): LinearLayout = vertical(padding).apply {
        background = shape(Palette.CARD, 16, Palette.BORDER)
        elevation = dp(1).toFloat()
    }
    fun spaced(view: View, top: Int = 10, bottom: Int = 0): View = view.apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
    }
    fun centered(value: String, size: Float = 14f, color: Int = Palette.MUTED) = label(value, size, color).apply {
        gravity = Gravity.CENTER
    }
}
