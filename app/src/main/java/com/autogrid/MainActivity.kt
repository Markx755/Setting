package com.autogrid

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private lateinit var etPkg: EditText
    private lateinit var etCols: EditText
    private lateinit var etMargin: EditText
    private lateinit var etInterval: EditText
    private lateinit var tvStatus: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun field(label: String, value: String, root: LinearLayout, number: Boolean): EditText {
        root.addView(TextView(this).apply { text = label; setTextColor(Color.LTGRAY); textSize = 14f })
        val e = EditText(this).apply {
            setText(value); setTextColor(Color.WHITE); setSingleLine()
            if (number) inputType = InputType.TYPE_CLASS_NUMBER
        }
        root.addView(e)
        return e
    }

    private fun btn(text: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        this.text = text; setBackgroundColor(color); setTextColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(4), dp(12), dp(4), 0) }
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = getSharedPreferences("cfg", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }
        root.addView(TextView(this).apply {
            text = "AutoGrid"; textSize = 26f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(16))
        })
        etPkg = field("ชื่อแพ็กเกจที่จะจัด (มีคำนี้ก็จับ)", sp.getString("pkg", "roblox")!!, root, false)
        etCols = field("จำนวนคอลัมน์ (0 = อัตโนมัติ)", sp.getString("cols", "0")!!, root, true)
        etMargin = field("ช่องว่างระหว่างหน้าต่าง (px)", sp.getString("margin", "0")!!, root, true)
        etInterval = field("ตรวจทุกกี่วินาที", sp.getString("interval", "3")!!, root, true)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(btn("เริ่ม", Color.parseColor("#2E7D32")) { start(sp) })
        row.addView(btn("หยุด", Color.parseColor("#C62828")) { stop() })
        root.addView(row)

        tvStatus = TextView(this).apply {
            setTextColor(Color.parseColor("#80CBC4")); textSize = 13f; setPadding(0, dp(16), 0, 0)
            text = "พร้อม"
        }
        root.addView(ScrollView(this).apply { addView(tvStatus) })
        setContentView(root)
    }

    private fun su(cmd: String): String = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor(); out
    } catch (e: Exception) { "ไม่มีสิทธิ์ root: ${e.message}" }

    private fun start(sp: android.content.SharedPreferences) {
        val pkg = etPkg.text.toString().trim().ifEmpty { "roblox" }
        val cols = etCols.text.toString().ifEmpty { "0" }
        val margin = etMargin.text.toString().ifEmpty { "0" }
        val interval = etInterval.text.toString().ifEmpty { "3" }
        sp.edit().putString("pkg", pkg).putString("cols", cols)
            .putString("margin", margin).putString("interval", interval).apply()

        val f = File(filesDir, "autogrid.sh")
        assets.open("autogrid.sh").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        tvStatus.text = "กำลังเริ่ม..."
        Thread {
            su("pkill -f 'autogri[d].sh'")
            val log = File(filesDir, "log.txt").absolutePath
            val out = su("(sh ${f.absolutePath} '$pkg' $cols $interval $margin > $log 2>&1 < /dev/null &)")
            Thread.sleep(2500)
            val running = su("pgrep -f 'autogri[d].sh'").trim().isNotEmpty()
            val logTxt = su("cat $log").takeLast(400)
            runOnUiThread {
                tvStatus.text = (if (running) "กำลังทำงาน ✔\n" else "ไม่ทำงาน (เช็ก root)\n") + out + logTxt
            }
        }.start()
    }

    private fun stop() {
        Thread {
            su("pkill -f 'autogri[d].sh'")
            runOnUiThread { tvStatus.text = "หยุดแล้ว" }
        }.start()
    }
}
