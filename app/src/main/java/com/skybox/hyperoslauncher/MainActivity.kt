package com.skybox.hyperoslauncher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var search: EditText
    private lateinit var appGrid: GridLayout
    private var allApps: List<android.content.pm.ApplicationInfo> = emptyList()
    private val prefs by lazy { getSharedPreferences("launcher_settings", MODE_PRIVATE) }
    private var wallpaperIndex = 0
    private var iconSize = 52
    private var currentScreen = "home"
    private val backgrounds = listOf(
        intArrayOf(Color.rgb(31, 25, 48), Color.rgb(86, 54, 104)),
        intArrayOf(Color.rgb(12, 54, 74), Color.rgb(36, 125, 135)),
        intArrayOf(Color.rgb(70, 34, 52), Color.rgb(155, 74, 100)),
        intArrayOf(Color.rgb(26, 48, 39), Color.rgb(78, 116, 83)),
        intArrayOf(Color.rgb(20, 9, 15), Color.rgb(70, 30, 58))
    )
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockUpdater = object : Runnable {
        override fun run() {
            if (currentScreen == "home") updateClock()
            clockHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 19, 29)
        window.navigationBarColor = Color.rgb(23, 19, 29)
        wallpaperIndex = prefs.getInt("wallpaper", 0).coerceIn(backgrounds.indices)
        iconSize = prefs.getInt("icon_size", 52).coerceIn(44, 64)
        allApps = packageManager.getInstalledApplications(0)
            .filter { it.packageName != packageName && packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        showHome()
        clockHandler.post(clockUpdater)
    }

    override fun onDestroy() {
        clockHandler.removeCallbacks(clockUpdater)
        super.onDestroy()
    }

    override fun onBackPressed() {
        when (currentScreen) {
            "drawer", "settings" -> showHome()
            else -> super.onBackPressed()
        }
    }

    private fun newRoot(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, backgrounds[wallpaperIndex])
        }
        setContentView(root)
        return root
    }

    private fun updateClock() {
        val clock = findViewWithTag<TextView>("home_clock")
        val date = findViewWithTag<TextView>("home_date")
        clock?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        date?.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
            .format(Date()).replaceFirstChar { it.uppercase() }
    }

    private fun showHome() {
        currentScreen = "home"
        newRoot()
        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val date = text("", 13f, 0xD9FFFFFF.toInt()).apply { tag = "home_date" }
        val clock = text("", 68f, Color.WHITE).apply {
            tag = "home_clock"
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, dp(4), 0, 0)
        }
        top.addView(date)
        top.addView(clock)
        top.addView(text("Seu espaço, do seu jeito", 14f, 0xE6FFFFFF.toInt()))
        root.addView(top)

        val searchHome = Button(this).apply {
            text = "⌕    Pesquisar"
            isAllCaps = false
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            background = rounded(0x33FFFFFF, 26)
            setOnClickListener { showDrawer() }
        }
        root.addView(searchHome, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(24) })

        root.addView(text("Aplicativos favoritos", 18f, Color.WHITE, true).apply {
            setPadding(0, dp(22), 0, dp(8))
        })
        val favorites = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        allApps.take(8).forEach { favorites.addView(appTile(it), tileParams()) }
        root.addView(favorites)

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(0x33FFFFFF, 30)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        allApps.take(4).forEach { info ->
            val tile = appTile(info, true)
            dock.addView(tile, LinearLayout.LayoutParams(0, dp(72), 1f))
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(82)))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(actionButton("Todos os apps") { showDrawer() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(actionButton("Configurações") { showSettings() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(actions)
        updateClock()
    }

    private fun showDrawer() {
        currentScreen = "drawer"
        newRoot()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(text("Aplicativos", 25f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(actionButton("Início") { showHome() })
        root.addView(header)
        search = EditText(this).apply {
            hint = "Pesquisar aplicativos"
            textSize = 15f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(0xCCFFFFFF.toInt())
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0x33FFFFFF, 26)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(16); bottomMargin = dp(12) })
        appGrid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        val scroll = ScrollView(this).apply { addView(appGrid) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        renderApps("")
    }

    private fun showSettings() {
        currentScreen = "settings"
        newRoot()
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(text("Personalização", 24f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(actionButton("Voltar") { showHome() })
        root.addView(header)
        root.addView(text("Papel de parede", 17f, Color.WHITE, true).apply { setPadding(0, dp(24), 0, dp(10)) })
        backgrounds.indices.forEach { index ->
            val names = listOf("Roxo", "Oceano", "Rosa", "Verde", "Ameixa")
            val button = actionButton(names[index] + if (wallpaperIndex == index) "  ✓" else "") {
                wallpaperIndex = index
                prefs.edit().putInt("wallpaper", wallpaperIndex).apply()
                showSettings()
            }
            root.addView(button, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(8) })
        }
        root.addView(text("Tamanho dos ícones", 17f, Color.WHITE, true).apply { setPadding(0, dp(20), 0, dp(10)) })
        listOf(44 to "Pequeno", 52 to "Médio", 64 to "Grande").forEach { (size, label) ->
            root.addView(actionButton(label + if (iconSize == size) "  ✓" else "") {
                iconSize = size
                prefs.edit().putInt("icon_size", iconSize).apply()
                showSettings()
            }, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(8) })
        }
        root.addView(text("Sobre", 17f, Color.WHITE, true).apply { setPadding(0, dp(24), 0, dp(8)) })
        root.addView(text("HyperOS Launcher • versão 1.1\nProtótipo independente inspirado no visual do HyperOS.", 13f, 0xCCFFFFFF.toInt()))
    }

    private fun renderApps(filter: String) {
        if (!::appGrid.isInitialized) return
        appGrid.removeAllViews()
        allApps.filter { packageManager.getApplicationLabel(it).toString().contains(filter, ignoreCase = true) }
            .forEach { appGrid.addView(appTile(it), tileParams()) }
    }

    private fun appTile(info: android.content.pm.ApplicationInfo, dock: Boolean = false): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(8), dp(3), dp(8))
            setOnClickListener {
                packageManager.getLaunchIntentForPackage(info.packageName)?.let { startActivity(it) }
            }
        }
        val icon = ImageView(this).apply {
            setImageDrawable(packageManager.getApplicationIcon(info))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val size = if (dock) dp(42) else dp(iconSize)
        item.addView(icon, LinearLayout.LayoutParams(size, size))
        if (!dock) {
            item.addView(text(packageManager.getApplicationLabel(info).toString(), 10.5f, Color.WHITE).apply {
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
        }
        return item
    }

    private fun tileParams() = GridLayout.LayoutParams().apply {
        width = 0
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(dp(2), dp(2), dp(2), dp(2))
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        setTextColor(Color.WHITE)
        background = rounded(0x33FFFFFF, 24)
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inline fun <reified T : View> ViewGroup.findViewWithTag(tagValue: String): T? {
        return findViewWithTag<View>(tagValue) as? T
    }
}
