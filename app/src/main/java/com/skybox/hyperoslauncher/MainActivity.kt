package com.skybox.hyperoslauncher

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private var allApps: List<android.content.pm.ApplicationInfo> = emptyList()
    private var wallpaperIndex = 0
    private val backgrounds = listOf(
        intArrayOf(Color.rgb(31, 25, 48), Color.rgb(86, 54, 104)),
        intArrayOf(Color.rgb(12, 54, 74), Color.rgb(36, 125, 135)),
        intArrayOf(Color.rgb(70, 34, 52), Color.rgb(155, 74, 100)),
        intArrayOf(Color.rgb(26, 48, 39), Color.rgb(78, 116, 83))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 19, 29)
        window.navigationBarColor = Color.rgb(23, 19, 29)
        allApps = packageManager.getInstalledApplications(0)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        buildHome()
    }

    private fun buildHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, backgrounds[wallpaperIndex]).apply {
                cornerRadius = dp(0).toFloat()
            }
        }
        val date = TextView(this).apply {
            text = "TERÇA-FEIRA  •  10 DE NOVEMBRO"
            textSize = 12f
            setTextColor(Color.WHITE)
            alpha = 0.82f
            letterSpacing = 0.08f
        }
        root.addView(date)
        val clock = TextView(this).apply {
            text = "09:07"
            textSize = 64f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(2))
        }
        root.addView(clock)
        val subtitle = TextView(this).apply {
            text = "Bem-vindo"
            textSize = 15f
            setTextColor(Color.WHITE)
            alpha = 0.9f
        }
        root.addView(subtitle)
        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val drawerTitle = TextView(this).apply {
            text = "Todos os aplicativos"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(drawerTitle)
        search = EditText(this).apply {
            hint = "Pesquisar aplicativos"
            textSize = 15f
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(0xCCFFFFFF.toInt())
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0x33FFFFFF, 28)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(grid)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 2.3f))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        val customize = Button(this).apply {
            text = "Personalizar papel de parede"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(0x44FFFFFF, 24)
            setOnClickListener { wallpaperIndex = (wallpaperIndex + 1) % backgrounds.size; buildHome() }
        }
        root.addView(customize, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
        setContentView(root)
        renderApps("")
    }

    private fun renderApps(filter: String) {
        if (!::grid.isInitialized) return
        grid.removeAllViews()
        val apps = allApps.filter {
            packageManager.getApplicationLabel(it).toString().contains(filter, ignoreCase = true)
        }
        apps.forEach { info ->
            val label = packageManager.getApplicationLabel(info).toString()
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(10), dp(3), dp(10))
                setOnClickListener {
                    packageManager.getLaunchIntentForPackage(info.packageName)?.let { startActivity(it) }
                }
            }
            val icon = ImageView(this).apply {
                setImageDrawable(packageManager.getApplicationIcon(info))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            item.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
            val name = TextView(this).apply {
                text = label
                textSize = 10.5f
                maxLines = 1
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(0, dp(5), 0, 0)
            }
            item.addView(name, LinearLayout.LayoutParams(-1, -2))
            val params = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            grid.addView(item, params)
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
