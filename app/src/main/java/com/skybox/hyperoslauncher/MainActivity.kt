package com.skybox.hyperoslauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.content.pm.ApplicationInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private data class AppEntry(
        val info: ApplicationInfo,
        val label: String,
        val icon: Drawable
    )

    private lateinit var root: LinearLayout
    private lateinit var appGrid: GridLayout
    private lateinit var search: EditText
    private val prefs by lazy { getSharedPreferences("launcher_settings", MODE_PRIVATE) }
    private var apps: List<AppEntry> = emptyList()
    private var favorites = mutableSetOf<String>()
    private var wallpaperIndex = 0
    private var iconSize = 52
    private var columns = 4
    private var showLabels = true
    private var showDate = true
    private var currentScreen = Screen.HOME
    private var firstResume = true
    private var favoritesOnly = false

    private enum class Screen { HOME, DRAWER, SETTINGS }

    private val backgrounds = listOf(
        intArrayOf(Color.rgb(31, 25, 48), Color.rgb(86, 54, 104)),
        intArrayOf(Color.rgb(12, 54, 74), Color.rgb(36, 125, 135)),
        intArrayOf(Color.rgb(70, 34, 52), Color.rgb(155, 74, 100)),
        intArrayOf(Color.rgb(26, 48, 39), Color.rgb(78, 116, 83)),
        intArrayOf(Color.rgb(20, 9, 15), Color.rgb(70, 30, 58)), intArrayOf(Color.rgb(17, 35, 65), Color.rgb(85, 125, 190)), intArrayOf(Color.rgb(45, 25, 65), Color.rgb(130, 85, 160)), intArrayOf(Color.rgb(35, 40, 48), Color.rgb(100, 115, 130))
    )

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockUpdater = object : Runnable {
        override fun run() {
            if (currentScreen == Screen.HOME) updateClock()
            clockHandler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 19, 29)
        window.navigationBarColor = Color.rgb(23, 19, 29)
        loadPreferences()
        loadApps()
        showHome()
        clockHandler.post(clockUpdater)
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
            return
        }
        val previousPackages = apps.map { it.info.packageName }
        loadApps()
        val currentPackages = apps.map { it.info.packageName }
        if (previousPackages != currentPackages) {
            if (currentScreen == Screen.HOME) showHome()
            if (currentScreen == Screen.DRAWER && ::appGrid.isInitialized) renderApps(search.text.toString())
        }
    }

    override fun onDestroy() {
        clockHandler.removeCallbacks(clockUpdater)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onBackPressed() {
        when (currentScreen) {
            Screen.DRAWER, Screen.SETTINGS -> showHome()
            else -> super.onBackPressed()
        }
    }

    private fun loadPreferences() {
        wallpaperIndex = prefs.getInt("wallpaper", 0).coerceIn(backgrounds.indices)
        iconSize = prefs.getInt("icon_size", 52).coerceIn(40, 68)
        columns = prefs.getInt("columns", 4).coerceIn(3, 5)
        showLabels = prefs.getBoolean("show_labels", true)
        showDate = prefs.getBoolean("show_date", true)
        favorites = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveFavorites() {
        prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
    }

    private fun loadApps() {
        val manager = packageManager
        apps = manager.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != packageName && manager.getLaunchIntentForPackage(it.packageName) != null }
            .mapNotNull { info ->
                try {
                    AppEntry(info, manager.getApplicationLabel(info).toString(), manager.getApplicationIcon(info))
                } catch (_: Exception) { null }
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
        favorites.retainAll(apps.map { it.info.packageName }.toSet())
        saveFavorites()
    }

    private fun newRoot(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, backgrounds[wallpaperIndex])
        }
        setContentView(root)
        return root
    }

    private fun updateClock() {
        if (!::root.isInitialized) return
        root.findViewWithTag<TextView>("home_clock")?.text =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        root.findViewWithTag<TextView>("home_date")?.apply {
            text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
                .format(Date()).replaceFirstChar { it.uppercase() }
            visibility = if (showDate) View.VISIBLE else View.GONE
        }
    }

    private fun showHome() {
        currentScreen = Screen.HOME
        newRoot()

        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header.addView(text("", 13f, 0xD9FFFFFF.toInt()).apply {
            tag = "home_date"
            visibility = if (showDate) View.VISIBLE else View.GONE
        })
        header.addView(text("", 66f, Color.WHITE).apply {
            tag = "home_clock"
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(text("Seu espaço, do seu jeito", 14f, 0xE6FFFFFF.toInt()))
        root.addView(header)

        val widgetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        widgetRow.addView(widgetCard("☀", "Clima", "Ver previsão") { try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=clima+hoje"))) } catch (_: Exception) { } }, LinearLayout.LayoutParams(0, dp(92), 1f))
        widgetRow.addView(widgetCard("▦", "Calendário", "Ver agenda") { try { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)) } catch (_: Exception) { } }, LinearLayout.LayoutParams(0, dp(92), 1f).apply { leftMargin = dp(8) })
        root.addView(widgetRow, LinearLayout.LayoutParams(-1, dp(100)).apply { topMargin = dp(16) })

        root.addView(actionButton("⌕    Pesquisar aplicativos") { showDrawer() },
            LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(20) })

        val section = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        section.addView(text("Favoritos", 18f, Color.WHITE, true).apply {
            setPadding(0, dp(20), 0, dp(8))
        })
        val favoriteApps = apps.filter { favorites.contains(it.info.packageName) }
        if (favoriteApps.isEmpty()) {
            section.addView(text("Seus aplicativos favoritos aparecerão aqui.", 13f, 0xCCFFFFFF.toInt())
                .apply { setPadding(0, dp(8), 0, dp(12)) })
            section.addView(actionButton("Escolher aplicativos") { showDrawer() })
        } else {
            val grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
            favoriteApps.take(8).forEach { grid.addView(appTile(it), tileParams()) }
            section.addView(grid)
        }
        root.addView(section)

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val dockApps = apps.filter { favorites.contains(it.info.packageName) }.take(5)
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(0x33FFFFFF, 28)
            setPadding(dp(6), dp(5), dp(6), dp(5))
        }
        dockApps.forEach { dock.addView(appTile(it, true), LinearLayout.LayoutParams(0, dp(64), 1f)) }
        if (dockApps.isEmpty()) {
            dock.addView(text("Adicione favoritos para preencher o dock", 12f, Color.WHITE))
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(74)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(actionButton("Todos os apps") { showDrawer() },
            LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(actionButton("Configurações") { showSettings() },
            LinearLayout.LayoutParams(0, dp(46), 1f))
        root.addView(actions)
        animateEntrance(root)
        updateClock()
    }

    private fun showDrawer() {
        currentScreen = Screen.DRAWER
        newRoot()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(text("Aplicativos", 24f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(actionButton("Início") { showHome() })
        root.addView(header)

        search = EditText(this).apply {
            hint = "Pesquisar aplicativos"
            textSize = 15f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setTextColor(Color.WHITE)
            setHintTextColor(0xCCFFFFFF.toInt())
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0x33FFFFFF, 26)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(14); bottomMargin = dp(10)
        })

        appGrid = GridLayout(this).apply { columnCount = columns; useDefaultMargins = false }
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(appGrid)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        animateEntrance(root)
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        renderApps("")
    }

    private fun showSettings() {
        currentScreen = Screen.SETTINGS
        newRoot()
        root.setPadding(dp(20), dp(18), dp(20), dp(14))

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = ripple(0x22FFFFFF, 40)
            setOnClickListener { showHome() }
        }
        topBar.addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
        topBar.addView(text("Configurações", 25f, Color.WHITE, true).apply {
            setPadding(dp(8), 0, 0, 0)
        })
        root.addView(topBar)

        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(text("Personalize seu espaço", 14f, 0xCCFFFFFF.toInt()).apply {
            setPadding(dp(4), dp(4), 0, dp(18))
        })

        content.addView(settingsCard("Aparência", "Cores e tamanho dos elementos"))
        content.addView(sectionTitle("Papel de parede"))
        val names = listOf("Lavanda", "Oceano", "Rosa", "Sálvia", "Ameixa", "Azul", "Violeta", "Grafite")
        val wallpaperRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        backgrounds.indices.forEach { index ->
            val swatch = View(this).apply {
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, backgrounds[index]).apply {
                    cornerRadius = dp(18).toFloat()
                    if (wallpaperIndex == index) setStroke(dp(3), Color.WHITE)
                }
                contentDescription = names[index]
                setOnClickListener {
                    wallpaperIndex = index
                    prefs.edit().putInt("wallpaper", index).apply()
                    showSettings()
                }
            }
            wallpaperRow.addView(swatch, LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                setMargins(dp(3), dp(2), dp(3), dp(2))
            })
        }
        content.addView(wallpaperRow)

        content.addView(sectionTitle("Tamanho dos ícones"))
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf(44 to "Pequeno", 52 to "Médio", 64 to "Grande").forEach { (size, label) ->
            sizeRow.addView(choiceChip(label, iconSize == size) {
                iconSize = size
                prefs.edit().putInt("icon_size", size).apply()
                showSettings()
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        content.addView(sizeRow)

        content.addView(settingsCard("Tela inicial", "Organize a gaveta e os ícones"))
        content.addView(sectionTitle("Colunas da gaveta"))
        val columnsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf(3, 4, 5).forEach { count ->
            columnsRow.addView(choiceChip("$count", columns == count) {
                columns = count
                prefs.edit().putInt("columns", count).apply()
                showSettings()
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        content.addView(columnsRow)

        content.addView(sectionTitle("Elementos visíveis"))
        content.addView(settingsToggle("Mostrar nomes dos aplicativos", "Exibe o nome abaixo dos ícones", showLabels) {
            showLabels = it
            prefs.edit().putBoolean("show_labels", it).apply()
        })
        content.addView(settingsToggle("Mostrar data", "Exibe a data abaixo do relógio", showDate) {
            showDate = it
            prefs.edit().putBoolean("show_date", it).apply()
            root.findViewWithTag<TextView>("home_date")?.visibility = if (it) View.VISIBLE else View.GONE
        })

        content.addView(settingsCard("Sistema", "Opções e informações do launcher"))
        content.addView(sectionTitle("Manutenção"))
        content.addView(settingsAction("Restaurar configurações", "Voltar às opções padrão") {
            AlertDialog.Builder(this)
                .setTitle("Restaurar configurações?")
                .setMessage("Seus favoritos e preferências serão apagados.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar") { _, _ ->
                    prefs.edit().clear().apply()
                    loadPreferences()
                    showSettings()
                }.show()
        })
        content.addView(text("HyperOS Launcher 2.0\nInspirado nas interfaces Android 12+ • Projeto independente",
            12f, 0xBFFFFFFF.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(8))
        })
        animateEntrance(root)
    }

    private fun widgetCard(symbol: String, title: String, subtitle: String, action: () -> Unit) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(10), dp(10), dp(10)); background = ripple(0x33FFFFFF, 22); addView(text(symbol, 22f, Color.WHITE, true)); addView(text(title, 14f, Color.WHITE, true)); addView(text(subtitle, 11f, 0xCCFFFFFF.toInt())); setOnClickListener { action() } }

    private fun settingsCard(title: String, subtitle: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(0x22FFFFFF, 22)
        addView(text(title, 18f, Color.WHITE, true))
        addView(text(subtitle, 12f, 0xCCFFFFFF.toInt()).apply { setPadding(0, dp(3), 0, 0) })
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(18); bottomMargin = dp(4)
        }
    }

    private fun choiceChip(label: String, selected: Boolean, action: () -> Unit) =
        TextView(this).apply {
            text = if (selected) "✓  $label" else label
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = ripple(if (selected) 0x665D91FF else 0x22FFFFFF, 18)
            setOnClickListener { action() }
        }

    private fun settingsToggle(title: String, subtitle: String, checked: Boolean, changed: (Boolean) -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background = ripple(0x1CFFFFFF, 18)
            val labels = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            labels.addView(text(title, 14f, Color.WHITE, true))
            labels.addView(text(subtitle, 11f, 0xBFFFFFFF.toInt()).apply { setPadding(0, dp(3), 0, 0) })
            addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            val toggle = Switch(this@MainActivity).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, value -> changed(value) }
            }
            addView(toggle)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
        }

    private fun settingsAction(title: String, subtitle: String, action: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = ripple(0x22FFFFFF, 18)
            addView(text(title, 14f, Color.WHITE, true))
            addView(text(subtitle, 11f, 0xBFFFFFFF.toInt()).apply { setPadding(0, dp(3), 0, 0) })
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
        }

    private fun animateEntrance(view: View) {
        view.alpha = 0f
        view.translationY = dp(12).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(280L).start()
    }

    private fun ripple(color: Int, radius: Int): Drawable {
        val shape = rounded(color, radius)
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x33FFFFFF), shape, null
        )
    }

    private fun sectionTitle(value: String) = text(value, 17f, Color.WHITE, true).apply {
        setPadding(0, dp(22), 0, dp(9))
    }

    private fun settingButton(label: String, action: () -> Unit) =
        actionButton(label, action).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(7) }
        }

    private fun renderApps(filter: String) {
        if (!::appGrid.isInitialized) return
        appGrid.removeAllViews()
        apps.asSequence()
            .filter { it.label.contains(filter, ignoreCase = true) }
            .forEach { appGrid.addView(appTile(it), tileParams()) }
    }

    private fun appTile(entry: AppEntry, dock: Boolean = false): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(7), dp(3), dp(7))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                packageManager.getLaunchIntentForPackage(entry.info.packageName)?.let { launch ->
                    try { startActivity(launch) } catch (_: Exception) { }
                }
            }
            setOnLongClickListener {
                val isFavorite = favorites.contains(entry.info.packageName)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(entry.label)
                    .setItems(arrayOf(
                        if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        "Abrir aplicativo"
                    )) { _, which ->
                        if (which == 0) {
                            if (isFavorite) favorites.remove(entry.info.packageName)
                            else favorites.add(entry.info.packageName)
                            saveFavorites()
                            if (currentScreen == Screen.HOME) showHome()
                            else renderApps(search.text.toString())
                        } else {
                            packageManager.getLaunchIntentForPackage(entry.info.packageName)?.let { startActivity(it) }
                        }
                    }.show()
                true
            }
        }
        val icon = ImageView(this).apply {
            setImageDrawable(entry.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = entry.label
        }
        val size = if (dock) dp(42) else dp(iconSize)
        item.addView(icon, LinearLayout.LayoutParams(size, size))
        if (!dock && showLabels) {
            item.addView(text(entry.label, 10.5f, Color.WHITE).apply {
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

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
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
}
