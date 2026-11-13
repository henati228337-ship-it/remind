package com.remindme.app

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var p: Prefs
    private lateinit var status: TextView
    private lateinit var soundBtn: MaterialButton

    private val pickSound = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            p.sound = uri?.toString() ?: "none"
            soundBtn.text = soundLabel()
        }
    }

    private fun nightMode(t: Int) = when (t) {
        1 -> AppCompatDelegate.MODE_NIGHT_NO
        2 -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    override fun onCreate(b: Bundle?) {
        p = Prefs(this)
        AppCompatDelegate.setDefaultNightMode(nightMode(p.theme))
        super.onCreate(b)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(32))
        }
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(root) })

        root.put(tv("Напоминалка", 30, bold = true), 0)
        status = tv("", 14, secondary = true)
        root.put(status, 4)

        root.put(card("🔔 По расписанию") {
            put(sw("Включено", p.intEnabled) { p.intEnabled = it; changed() })
            put(tv("Окно времени и частота", 13, secondary = true))
            put(row(tv("с"), timeBtn({ p.intStart }, { p.intStart = it }), tv("до"), timeBtn({ p.intEnd }, { p.intEnd = it })))
            put(field("Каждые N минут", p.intEvery.toString(), number = true) {
                p.intEvery = it.toIntOrNull()?.coerceIn(1, 1440) ?: 60; changed()
            })
            put(field("Текст уведомления", p.intText) { p.intText = it })
        }, 20)

        root.put(card("🎲 Случайные напоминания") {
            put(sw("Включено", p.rndEnabled) { p.rndEnabled = it; changed() })
            put(tv("В течение дня, в случайное время", 13, secondary = true))
            put(row(tv("с"), timeBtn({ p.rndStart }, { p.rndStart = it }), tv("до"), timeBtn({ p.rndEnd }, { p.rndEnd = it })))
            put(field("Сколько раз в день", p.rndCount.toString(), number = true) {
                p.rndCount = it.toIntOrNull()?.coerceIn(1, 200) ?: 5; changed()
            })
            put(field("Минимум минут между напоминаниями", p.rndGap.toString(), number = true) {
                p.rndGap = it.toIntOrNull()?.coerceIn(0, 600) ?: 30; changed()
            })
            put(field("Тексты (каждый с новой строки)", p.rndTexts, multi = true) { p.rndTexts = it })
        }, 16)

        root.put(card("⚙️ Настройки") {
            put(tv("Тема", 13, secondary = true))
            val names = listOf("Авто", "Светлая", "Тёмная")
            val ids = names.map { View.generateViewId() }
            val g = MaterialButtonToggleGroup(this@MainActivity).apply { isSingleSelection = true; isSelectionRequired = true }
            names.forEachIndexed { i, n ->
                g.addView(MaterialButton(this@MainActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { id = ids[i]; text = n })
            }
            g.check(ids[p.theme])
            g.addOnButtonCheckedListener { _, id, checked ->
                if (checked) {
                    val t = ids.indexOf(id)
                    if (t != p.theme) { p.theme = t; AppCompatDelegate.setDefaultNightMode(nightMode(t)) }
                }
            }
            put(g)

            soundBtn = btn(soundLabel()) {
                pickSound.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Звук напоминаний")
                    if (p.sound.isNotEmpty() && p.sound != "none")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(p.sound))
                })
            }
            put(soundBtn)
            put(sw("Вибрация", p.vibrate) { p.vibrate = it })
            put(sw("Пауза (отключить всё на время)", p.paused) { p.paused = it; refresh() })
            put(btn("Проверить уведомление") {
                Notifier.show(this@MainActivity, "Проверка", "Так будут выглядеть напоминания", 99)
            })
            if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                put(btn("Разрешить точные уведомления") {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                })
            }
            put(btn("Не усыплять приложение (Samsung)") {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            })
        }, 16)
    }

    override fun onResume() { super.onResume(); changed() }

    // ---------- helpers ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun fmt(m: Int) = "%02d:%02d".format(m / 60 % 24, m % 60)

    private fun LinearLayout.put(v: View, top: Int = 12) {
        addView(v, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(top) })
    }

    private fun tv(text: String, sp: Int = 16, bold: Boolean = false, secondary: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = sp.toFloat()
        if (bold) setTypeface(typeface, Typeface.BOLD)
        if (secondary) setTextColor(MaterialColors.getColor(this@MainActivity, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
    }

    private fun card(title: String, build: LinearLayout.() -> Unit): MaterialCardView {
        val c = MaterialCardView(this).apply {
            radius = dp(20).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
            setStrokeColor(ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)) }
        col.put(tv(title, 18, bold = true), 0)
        col.build()
        c.addView(col)
        return c
    }

    private fun row(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        v.forEach { addView(it, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = dp(10) }) }
    }

    private fun sw(label: String, v: Boolean, on: (Boolean) -> Unit) = MaterialSwitch(this).apply {
        text = label; isChecked = v; setOnCheckedChangeListener { _, c -> on(c) }
    }

    private fun btn(label: String, click: () -> Unit) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label; setOnClickListener { click() }
        }

    private fun timeBtn(get: () -> Int, set: (Int) -> Unit): MaterialButton {
        val b = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        b.text = fmt(get())
        b.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> set(h * 60 + m); b.text = fmt(get()); changed() }, get() / 60 % 24, get() % 60, true).show()
        }
        return b
    }

    private fun field(label: String, value: String, number: Boolean = false, multi: Boolean = false, onChange: (String) -> Unit): TextInputLayout {
        val til = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle)
        til.hint = label
        val et = TextInputEditText(til.context)
        et.setText(value)
        et.inputType = when {
            number -> InputType.TYPE_CLASS_NUMBER
            multi -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else -> InputType.TYPE_CLASS_TEXT
        }
        et.doAfterTextChanged { onChange(it.toString()) }
        til.addView(et)
        return til
    }

    private fun soundLabel(): String {
        val name = when (p.sound) {
            "" -> "по умолчанию"
            "none" -> "без звука"
            else -> RingtoneManager.getRingtone(this, Uri.parse(p.sound))?.getTitle(this) ?: "выбран"
        }
        return "Звук: $name"
    }

    private fun changed() { Scheduler.rescheduleAll(this); refresh() }

    private fun refresh() {
        val f = SimpleDateFormat("EEE HH:mm", Locale("ru"))
        val parts = mutableListOf<String>()
        if (p.intEnabled) parts += "по расписанию ${f.format(Date(Scheduler.nextInterval(p)))}"
        if (p.rndEnabled) parts += "случайное ${f.format(Date(Scheduler.nextRandom(p)))}"
        status.text = when {
            p.paused -> "На паузе"
            parts.isEmpty() -> "Ничего не включено"
            else -> "Далее: " + parts.joinToString(" · ")
        }
    }
}
