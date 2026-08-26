package com.paperdesk.companion

import org.json.JSONArray
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.event.TableModelEvent
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel

class CompanionFrame : JFrame("ProjectOS Companion") {

    private val black = Color(0, 0, 0)
    private val white = Color(255, 255, 255)
    private val accent = Color(255, 106, 0)
    private val fontUi = Font("SansSerif", Font.PLAIN, 14)
    private val fontSmall = Font("SansSerif", Font.PLAIN, 12)
    private val fontTitle = Font("SansSerif", Font.BOLD, 18)
    private val fontMono = Font("Monospaced", Font.PLAIN, 13)

    private val cards = CardLayout()
    private val content = JPanel(cards)
    private val status = JLabel("USB disconnected")
    private var bundle: JSONObject = emptyBundle()
    private var lastSync = 0L
    private var painting = false
    private var selectedProjectId: String? = null
    private val projectCards = CardLayout()
    private val projectStack = JPanel(projectCards)
    private val detailCards = CardLayout()
    private val detailBody = JPanel(detailCards)
    private val detailTitle = JLabel("PROJECT")

    private val dashInfo = JTextArea()
    private val projectModel = tableModel("id", "Name", "Number", "Updated", "Meas", "Notes", "Sched", "Todos")
    private val schedModel = tableModel("id", "Time", "Title", "Project", "Date", "Notes", "Done")
    private val todoModel = tableModel("id", "Done", "Title", "Project", "Due", "Priority", "Notes")
    private val noteModel = tableModel("id", "Title", "Project", "Updated", "Body")
    private val measModel = object : DefaultTableModel(arrayOf("id", "Item", "Project", "Desc", "L", "B", "H", "UOM", "Type", "Qty"), 0) {
        override fun isCellEditable(row: Int, column: Int) = column in 3..7
    }
    private val pNoteModel = tableModel("id", "Title", "Updated", "Body")
    private val pSchedModel = tableModel("id", "Time", "Title", "Date", "Notes", "Done")
    private val pTodoModel = tableModel("id", "Done", "Title", "Due", "Priority", "Notes")
    private val appModel = tableModel("Name", "Package", "Kind", "Enabled")
    private lateinit var appsTable: JTable
    private lateinit var projectTable: JTable
    private lateinit var schedTable: JTable
    private lateinit var todoTable: JTable
    private lateinit var noteTable: JTable
    private lateinit var measTable: JTable
    private lateinit var pNoteTable: JTable
    private lateinit var pSchedTable: JTable
    private lateinit var pTodoTable: JTable
    private val measTotal = JLabel(" ")
    private val fileList = JTextArea()
    private val fileProject = JComboBox<String>()
    private val fileFolder = JComboBox(arrayOf("Measurements", "Notes", "Drawings", "PDFs", "Photos"))
    private val settingsArea = JTextArea()
    private val pagesField = JTextField()
    private val advLog = JTextArea()
    private val advCmd = JTextField()
    private val search = JTextField()
    private val featureBoxes = linkedMapOf<String, JCheckBox>()
    private val backupBoxes = linkedMapOf<String, JCheckBox>()

    private val required = setOf(
        "android", "com.android.systemui", "com.android.phone", "com.android.settings",
        "com.android.providers.settings", "com.android.shell", "com.paperdesk.launcher",
        "com.topjohnwu.magisk", "eu.chainfire.supersu",
        "com.google.android.packageinstaller", "com.android.packageinstaller",
        "com.samsung.android.packageinstaller"
    )

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        preferredSize = Dimension(1280, 800)
        contentPane.background = black
        background = black

        val nav = JPanel().apply {
            background = black
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(16, 12, 16, 12)
            preferredSize = Dimension(200, 800)
        }
        nav.add(JLabel("PROJECTOS").apply {
            foreground = accent
            font = fontTitle
            alignmentX = LEFT_ALIGNMENT
        })
        nav.add(Box.createVerticalStrut(16))
        listOf(
            "Dashboard", "Projects", "Schedule", "Todo", "Notes",
            "Files", "Apps", "OS Settings", "Device", "Backup", "Sync", "Advanced"
        ).forEach { name ->
            nav.add(navBtn(name) { show(name) })
            nav.add(Box.createVerticalStrut(4))
        }

        content.background = black
        content.add(wrap(dashPanel()), "Dashboard")
        content.add(wrap(projectsPanel()), "Projects")
        content.add(wrap(schedulePanel()), "Schedule")
        content.add(wrap(todoPanel()), "Todo")
        content.add(wrap(notesPanel()), "Notes")
        content.add(wrap(filesPanel()), "Files")
        content.add(wrap(appsPanel()), "Apps")
        content.add(wrap(settingsPanel()), "OS Settings")
        content.add(wrap(devicePanel()), "Device")
        content.add(wrap(backupPanel()), "Backup")
        content.add(wrap(syncPanel()), "Sync")
        content.add(wrap(advancedPanel()), "Advanced")

        search.background = black
        search.foreground = white
        search.caretColor = accent
        search.preferredSize = Dimension(220, 28)
        search.toolTipText = "Search projects, schedule, todos, notes"
        search.addActionListener { paintData() }

        val top = JPanel(BorderLayout()).apply {
            background = black
            border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
            add(status.apply { foreground = white; font = fontUi }, BorderLayout.WEST)
            val actions = JPanel().apply {
                background = black
                add(JLabel("SEARCH").apply { foreground = accent; font = fontSmall })
                add(search)
                add(accentBtn("SYNC NOW") { syncNow() })
                add(plainBtn("PUSH TO PHONE") { pushBundle() })
            }
            add(actions, BorderLayout.EAST)
        }

        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nav, content).apply {
            dividerLocation = 200
            dividerSize = 1
            background = black
            border = null
        }
        add(top, BorderLayout.NORTH)
        add(split, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(null)
        show("Dashboard")
        measModel.addTableModelListener { e ->
            if (!painting && e.type == TableModelEvent.UPDATE) onMeasureEdit(e.firstRow)
        }
        val cached = Paths.localBundle()
        if (cached.exists()) {
            bundle = runCatching { JSONObject(cached.readText()) }.getOrElse { emptyBundle() }
            paintData()
        }
        refreshConnection()
    }

    private fun show(name: String) {
        cards.show(content, name)
        when (name) {
            "Dashboard", "Device" -> refreshConnection()
            "Apps" -> loadApps()
            "Files" -> {
                fillFileProjects()
                loadFiles()
            }
            "OS Settings" -> paintSettings()
        }
    }

    private fun wrap(inner: JPanel): JPanel = JPanel(BorderLayout()).apply {
        background = black
        border = BorderFactory.createEmptyBorder(16, 20, 16, 20)
        add(inner, BorderLayout.CENTER)
    }

    private fun dashPanel(): JPanel {
        dashInfo.isEditable = false
        dashInfo.background = black
        dashInfo.foreground = white
        dashInfo.font = fontMono
        dashInfo.caretColor = accent
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("DEVICE DASHBOARD").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(JScrollPane(dashInfo).dark(), BorderLayout.CENTER)
        }
    }

    private fun projectsPanel(): JPanel {
        projectTable = JTable(projectModel).darkTable()
        hideId(projectTable)
        projectTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount >= 2) openSelectedProject()
            }
        })
        val list = dataPanel(
            "PROJECTS",
            projectTable,
            accentBtn("NEW") { editProject(null) },
            plainBtn("EDIT") { editProject(selectedId(projectTable, projectModel)) },
            plainBtn("OPEN") { openSelectedProject() },
            plainBtn("DELETE") { deleteProject() }
        )
        projectStack.add(list, "list")
        projectStack.add(projectDetailPanel(), "detail")
        projectStack.background = black
        return JPanel(BorderLayout()).apply {
            background = black
            add(projectStack, BorderLayout.CENTER)
        }
    }

    private fun projectDetailPanel(): JPanel {
        measTable = JTable(measModel).darkTable()
        hideId(measTable)
        measTotal.foreground = accent
        measTotal.font = fontUi
        detailTitle.foreground = accent
        detailTitle.font = fontTitle
        val measBtns = JPanel().apply {
            background = black
            add(accentBtn("NEW") { editMeasure(null) })
            add(plainBtn("EDIT") { editMeasure(selectedId(measTable, measModel)) })
            add(plainBtn("DELETE") { deleteFrom("measurements", selectedId(measTable, measModel)) })
        }
        val measure = JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("MEASUREMENT BOOK").apply { foreground = accent; font = fontUi }, BorderLayout.NORTH)
            add(JScrollPane(measTable).dark(), BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                background = black
                add(measTotal, BorderLayout.WEST)
                add(measBtns, BorderLayout.EAST)
            }, BorderLayout.SOUTH)
        }
        val pNotes = run {
            pNoteTable = JTable(pNoteModel).darkTable()
            hideId(pNoteTable)
            dataPanel(
                "NOTES",
                pNoteTable,
                accentBtn("NEW") { editNote(null, selectedProjectId) },
                plainBtn("EDIT") { editNote(selectedId(pNoteTable, pNoteModel), selectedProjectId) },
                plainBtn("DELETE") { deleteFrom("notes", selectedId(pNoteTable, pNoteModel)) }
            )
        }
        val pSched = run {
            pSchedTable = JTable(pSchedModel).darkTable()
            hideId(pSchedTable)
            dataPanel(
                "SCHEDULE",
                pSchedTable,
                accentBtn("NEW") { editSchedule(null, selectedProjectId) },
                plainBtn("EDIT") { editSchedule(selectedId(pSchedTable, pSchedModel), selectedProjectId) },
                plainBtn("DELETE") { deleteFrom("schedules", selectedId(pSchedTable, pSchedModel)) }
            )
        }
        val pTodos = run {
            pTodoTable = JTable(pTodoModel).darkTable()
            hideId(pTodoTable)
            dataPanel(
                "TODO",
                pTodoTable,
                accentBtn("NEW") { editTodo(null, selectedProjectId) },
                plainBtn("EDIT") { editTodo(selectedId(pTodoTable, pTodoModel), selectedProjectId) },
                plainBtn("DELETE") { deleteFrom("todos", selectedId(pTodoTable, pTodoModel)) }
            )
        }
        detailBody.background = black
        detailBody.add(measure, "measure")
        detailBody.add(pNotes, "notes")
        detailBody.add(pSched, "schedule")
        detailBody.add(pTodos, "todo")
        val sections = JPanel().apply {
            background = black
            add(accentBtn("MEASUREMENT BOOK") { showProjectSection("measure") })
            add(plainBtn("NOTES") { showProjectSection("notes") })
            add(plainBtn("SCHEDULE") { showProjectSection("schedule") })
            add(plainBtn("TODO") { showProjectSection("todo") })
        }
        val head = JPanel(BorderLayout()).apply {
            background = black
            add(plainBtn("BACK") { closeProject() }, BorderLayout.WEST)
            add(detailTitle, BorderLayout.CENTER)
            add(sections, BorderLayout.SOUTH)
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(head, BorderLayout.NORTH)
            add(detailBody, BorderLayout.CENTER)
        }
    }

    private fun showProjectSection(name: String) {
        detailCards.show(detailBody, name)
    }

    private fun openSelectedProject() {
        val id = selectedId(projectTable, projectModel) ?: return
        selectedProjectId = id
        val name = projectName(bundle, id).ifBlank { "PROJECT" }
        detailTitle.text = name.uppercase(Locale.US)
        paintData()
        showProjectSection("measure")
        projectCards.show(projectStack, "detail")
    }

    private fun closeProject() {
        selectedProjectId = null
        projectCards.show(projectStack, "list")
        paintData()
    }

    private fun schedulePanel(): JPanel {
        schedTable = JTable(schedModel).darkTable()
        hideId(schedTable)
        return dataPanel("SCHEDULE", schedTable, accentBtn("NEW") { editSchedule(null) }, plainBtn("EDIT") { editSchedule(selectedId(schedTable, schedModel)) }, plainBtn("DELETE") { deleteFrom("schedules", selectedId(schedTable, schedModel)) })
    }

    private fun todoPanel(): JPanel {
        todoTable = JTable(todoModel).darkTable()
        hideId(todoTable)
        return dataPanel("TODO", todoTable, accentBtn("NEW") { editTodo(null) }, plainBtn("EDIT") { editTodo(selectedId(todoTable, todoModel)) }, plainBtn("DELETE") { deleteFrom("todos", selectedId(todoTable, todoModel)) })
    }

    private fun notesPanel(): JPanel {
        noteTable = JTable(noteModel).darkTable()
        hideId(noteTable)
        return dataPanel("NOTES", noteTable, accentBtn("NEW") { editNote(null) }, plainBtn("EDIT") { editNote(selectedId(noteTable, noteModel)) }, plainBtn("DELETE") { deleteFrom("notes", selectedId(noteTable, noteModel)) })
    }

    private fun dataPanel(title: String, table: JTable, vararg buttons: JButton): JPanel {
        val btns = JPanel().apply {
            background = black
            buttons.forEach { add(it) }
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel(title).apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(JScrollPane(table).dark(), BorderLayout.CENTER)
            add(btns, BorderLayout.SOUTH)
        }
    }

    private fun filesPanel(): JPanel {
        fileList.isEditable = false
        fileList.background = black
        fileList.foreground = white
        fileList.font = fontMono
        styleCombo(fileProject)
        styleCombo(fileFolder)
        val top = JPanel().apply {
            background = black
            add(JLabel("PROJECT").apply { foreground = accent; font = fontSmall })
            add(fileProject)
            add(JLabel("FOLDER").apply { foreground = accent; font = fontSmall })
            add(fileFolder)
        }
        val btns = JPanel().apply {
            background = black
            add(accentBtn("PC TO PHONE") { sendFile() })
            add(plainBtn("PHONE TO PC") { recvFile() })
            add(plainBtn("REFRESH") { loadFiles() })
        }
        fileProject.addActionListener { loadFiles() }
        fileFolder.addActionListener { loadFiles() }
        return JPanel(BorderLayout()).apply {
            background = black
            val north = JPanel(BorderLayout()).apply {
                background = black
                add(JLabel("FILES").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
                add(top, BorderLayout.SOUTH)
            }
            add(north, BorderLayout.NORTH)
            add(JScrollPane(fileList).dark(), BorderLayout.CENTER)
            add(btns, BorderLayout.SOUTH)
        }
    }

    private fun appsPanel(): JPanel {
        appsTable = JTable(appModel).darkTable()
        appsTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val btns = JPanel().apply {
            background = black
            add(plainBtn("ENABLE") { pkgAction("enable") })
            add(plainBtn("DISABLE") { pkgAction("disable") })
            add(plainBtn("UNINSTALL") { pkgAction("uninstall") })
            add(plainBtn("CLEAR DATA") { pkgAction("clear") })
            add(plainBtn("OPEN") { pkgAction("open") })
            add(plainBtn("REFRESH") { loadApps() })
        }
        val note = JLabel("Required OS components cannot be uninstalled or disabled.").apply {
            foreground = accent
            font = fontSmall
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("APPLICATIONS").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(JScrollPane(appsTable).dark(), BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                background = black
                add(note, BorderLayout.NORTH)
                add(btns, BorderLayout.SOUTH)
            }, BorderLayout.SOUTH)
        }
    }

    private fun settingsPanel(): JPanel {
        val features = JPanel().apply {
            background = black
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        listOf(
            "landscape" to "Landscape launcher",
            "statusBar" to "Status bar",
            "navBar" to "Navigation bar",
            "alwaysOn" to "Always-on display",
            "clockAnalog" to "Analog clock",
            "calcScientific" to "Scientific calculator",
            "mobileData" to "Mobile network",
            "nfc" to "NFC",
            "gps" to "GPS",
            "sensors" to "Sensors",
            "autoRotate" to "Auto rotation",
            "usbDebug" to "USB debugging",
            "developerOptions" to "Developer options",
            "lockScreen" to "Lock screen",
            "notifications" to "Notifications",
            "sound" to "Sound",
            "vibration" to "Vibration",
            "projectEnabled" to "Project",
            "calcEnabled" to "Calculator",
            "todoEnabled" to "Todo",
            "scheduleEnabled" to "Schedule",
            "notesEnabled" to "Notes",
            "measureEnabled" to "Measurement Book (inside Project)"
        ).forEach { (key, label) ->
            val box = JCheckBox(label).apply {
                background = black
                foreground = white
                font = fontSmall
            }
            featureBoxes[key] = box
            features.add(box)
        }
        pagesField.background = black
        pagesField.foreground = white
        pagesField.caretColor = accent
        settingsArea.background = black
        settingsArea.foreground = white
        settingsArea.font = fontMono
        settingsArea.caretColor = accent
        val appearance = JPanel(BorderLayout()).apply {
            background = black
            val head = JPanel(GridLayout(0, 1, 4, 4)).apply {
                background = black
                add(JLabel("LAUNCHER PAGES").apply { foreground = accent; font = fontSmall })
                add(pagesField)
                add(JLabel("desk project todo schedule notes tools").apply { foreground = white; font = fontSmall })
            }
            add(head, BorderLayout.NORTH)
            add(JScrollPane(settingsArea).dark(), BorderLayout.CENTER)
        }
        val split = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JScrollPane(features).dark(),
            appearance
        ).apply {
            dividerLocation = 280
            dividerSize = 1
            background = black
            border = null
        }
        val btns = JPanel().apply {
            background = black
            add(accentBtn("APPLY TO PHONE") { applySettings() })
            add(plainBtn("ARCHITECTURE PROFILE") { loadProfile() })
            add(plainBtn("SAVE PROFILE") { saveProfile() })
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("OS SETTINGS").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(split, BorderLayout.CENTER)
            add(btns, BorderLayout.SOUTH)
        }
    }

    private fun devicePanel(): JPanel {
        val grid = JPanel(GridLayout(0, 2, 8, 8)).apply { background = black }
        listOf(
            "SYNC NOW" to { syncNow() },
            "BACKUP" to { backup() },
            "RESTORE" to { restore() },
            "RESTART DEVICE" to { confirm("Restart the phone?") { Adb.run("reboot") } },
            "REBOOT RECOVERY" to { confirm("Reboot into recovery?") { Adb.run("reboot", "recovery") } },
            "INSTALL UPDATE" to { installUpdate() }
        ).forEach { (label, act) -> grid.add(accentBtn(label, act)) }
        val note = JLabel("OS package updates are enabled for APK sideload. Full ROM OTA stays off until the build supports a safe rollback.").apply {
            foreground = white
            font = fontSmall
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("DEVICE ACTIONS").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(grid, BorderLayout.CENTER)
            add(note, BorderLayout.SOUTH)
        }
    }

    private fun backupPanel(): JPanel {
        val flags = JPanel().apply {
            background = black
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        listOf(
            "projects" to "Projects",
            "schedules" to "Schedules",
            "todos" to "Todos",
            "notes" to "Notes",
            "measurements" to "Measurements",
            "settings" to "Settings / launcher / OS configuration",
            "alarms" to "Alarms",
            "calculatorHistory" to "Calculator history"
        ).forEach { (key, label) ->
            val box = JCheckBox(label, true).apply {
                background = black
                foreground = white
            }
            backupBoxes[key] = box
            flags.add(box)
        }
        val btns = JPanel().apply {
            background = black
            add(accentBtn("BACKUP") { backup() })
            add(plainBtn("RESTORE DATA") { restore(mode = "data") })
            add(plainBtn("RESTORE CONFIG") { restore(mode = "config") })
            add(plainBtn("FULL RESTORE") { restore(mode = "full") })
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("BACKUP AND RESTORE").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(flags, BorderLayout.CENTER)
            add(btns, BorderLayout.SOUTH)
        }
    }

    private fun syncPanel(): JPanel {
        val body = JLabel(
            "<html>USB is the transport.<br>" +
                "Sync Now asks the OS data service to export, then pulls the bundle.<br>" +
                "Push To Phone writes the inbox and asks the OS to import.</html>"
        ).apply {
            foreground = white
            font = fontUi
        }
        val btns = JPanel().apply {
            background = black
            add(accentBtn("SYNC NOW") { syncNow() })
            add(plainBtn("PUSH TO PHONE") { pushBundle() })
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("SYNCHRONIZATION").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
            add(btns, BorderLayout.SOUTH)
        }
    }

    private fun advancedPanel(): JPanel {
        advLog.isEditable = false
        advLog.background = black
        advLog.foreground = white
        advLog.font = Font("Monospaced", Font.PLAIN, 12)
        advCmd.background = black
        advCmd.foreground = white
        advCmd.caretColor = accent
        advCmd.font = fontMono
        advCmd.addActionListener { runAdv() }
        val row = JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("adb ").apply { foreground = accent }, BorderLayout.WEST)
            add(advCmd, BorderLayout.CENTER)
            add(accentBtn("RUN") { runAdv() }, BorderLayout.EAST)
        }
        val quick = JPanel().apply {
            background = black
            add(plainBtn("PROPERTIES") { advPreset("shell getprop") })
            add(plainBtn("PACKAGES") { advPreset("shell pm list packages") })
            add(plainBtn("LOGCAT") { advPreset("logcat -d -t 80") })
            add(plainBtn("DF") { advPreset("shell df") })
        }
        return JPanel(BorderLayout()).apply {
            background = black
            add(JLabel("ADVANCED  ADB").apply { foreground = accent; font = fontTitle }, BorderLayout.NORTH)
            add(quick, BorderLayout.PAGE_START)
            add(JScrollPane(advLog).dark(), BorderLayout.CENTER)
            add(row, BorderLayout.SOUTH)
        }
    }

    private fun syncNow() {
        work("Exporting from phone...") {
            if (!Adb.connected()) return@work "Phone not connected"
            Adb.broadcast("com.paperdesk.os.EXPORT")
            Thread.sleep(900)
            val local = Paths.localBundle()
            Adb.pull(Paths.REMOTE_BUNDLE, local)
            bundle = runCatching { JSONObject(local.readText()) }.getOrElse { emptyBundle() }
            val wx = WeatherSync.push(bundle)
            local.writeText(bundle.toString(2))
            lastSync = System.currentTimeMillis()
            SwingUtilities.invokeLater { paintData(); refreshConnection() }
            if (wx.isNullOrBlank()) "Synced" else "Synced  Weather: $wx"
        }
    }

    private fun pushBundle() {
        work("Pushing to phone...") {
            if (!Adb.connected()) return@work "Phone not connected"
            collectSettings()
            val local = Paths.localBundle()
            local.writeText(bundle.toString(2))
            WeatherSync.push(bundle)
            local.writeText(bundle.toString(2))
            Adb.shell("mkdir -p ${Paths.REMOTE_ROOT}/inbox")
            Adb.push(local, Paths.REMOTE_INBOX)
            Adb.broadcast("com.paperdesk.os.IMPORT")
            lastSync = System.currentTimeMillis()
            "Pushed"
        }
    }

    private fun applySettings() {
        work("Applying settings...") {
            if (!Adb.connected()) return@work "Phone not connected"
            collectSettings()
            val obj = bundle.optJSONObject("settings") ?: JSONObject()
            val f = File(Paths.cacheDir(), "settings.json")
            f.writeText(obj.toString(2))
            Adb.shell("mkdir -p ${Paths.REMOTE_ROOT}/inbox")
            Adb.push(f, Paths.REMOTE_SETTINGS)
            Adb.broadcast("com.paperdesk.os.APPLY_SETTINGS")
            Paths.localBundle().writeText(bundle.toString(2))
            "Settings applied"
        }
    }

    private fun loadProfile() {
        val p = architectureProfile()
        val s = bundle.optJSONObject("settings") ?: JSONObject()
        p.keys().forEach { s.put(it, p.get(it)) }
        bundle.put("settings", s)
        paintSettings()
        status.text = "Architecture Mode loaded. Apply to phone to use it."
        status.foreground = accent
    }

    private fun saveProfile() {
        collectSettings()
        val s = bundle.optJSONObject("settings") ?: JSONObject()
        val name = JOptionPane.showInputDialog(this, "Profile name", s.optString("name", "Architecture Mode")) ?: return
        s.put("name", name)
        val profiles = s.optJSONArray("profiles") ?: JSONArray()
        profiles.put(JSONObject(s.toString()))
        s.put("profiles", profiles)
        bundle.put("settings", s)
        paintSettings()
        status.text = "Profile saved in the bundle. Push or apply to keep it on the phone."
        status.foreground = accent
    }

    private fun backup() {
        val chooser = JFileChooser(Paths.cacheDir()).apply {
            selectedFile = File(Paths.cacheDir(), "projectos-backup-${stamp()}.zip")
            fileFilter = FileNameExtensionFilter("Zip", "zip")
        }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return
        work("Creating backup...") {
            collectSettings()
            val out = chooser.selectedFile
            val pack = JSONObject()
            pack.put("version", bundle.optInt("version", 1))
            pack.put("os", bundle.optString("os", "ProjectOS"))
            pack.put("osVersion", bundle.optString("osVersion", "1.0"))
            pack.put("exportedAt", System.currentTimeMillis())
            backupBoxes.forEach { (key, box) ->
                if (box.isSelected) {
                    if (key == "settings") pack.put("settings", bundle.optJSONObject("settings") ?: JSONObject())
                    else pack.put(key, bundle.optJSONArray(key) ?: JSONArray())
                }
            }
            ZipOutputStream(out.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("os-bundle.json"))
                zip.write(pack.toString(2).toByteArray())
                zip.closeEntry()
            }
            "Backup written: ${out.name}"
        }
    }

    private fun restore(mode: String = "full") {
        val chooser = JFileChooser(Paths.cacheDir()).apply {
            fileFilter = FileNameExtensionFilter("Zip or JSON", "zip", "json")
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        work("Restoring...") {
            val f = chooser.selectedFile
            val raw = if (f.extension.equals("zip", true)) {
                ZipInputStream(f.inputStream()).use { zip ->
                    var found: String? = null
                    var e = zip.nextEntry
                    while (e != null) {
                        if (e.name.endsWith("os-bundle.json")) {
                            found = zip.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        e = zip.nextEntry
                    }
                    found
                }
            } else f.readText()
            if (raw == null) return@work "No os-bundle.json in backup"
            val incoming = JSONObject(raw)
            when (mode) {
                "config" -> bundle.put("settings", incoming.optJSONObject("settings") ?: JSONObject())
                "data" -> listOf("projects", "schedules", "todos", "notes", "measurements", "alarms", "calculatorHistory").forEach {
                    if (incoming.has(it)) bundle.put(it, incoming.optJSONArray(it) ?: JSONArray())
                }
                else -> bundle = incoming
            }
            Paths.localBundle().writeText(bundle.toString(2))
            SwingUtilities.invokeLater { paintData() }
            if (Adb.connected()) {
                Adb.shell("mkdir -p ${Paths.REMOTE_ROOT}/inbox")
                Adb.push(Paths.localBundle(), Paths.REMOTE_INBOX)
                Adb.broadcast("com.paperdesk.os.IMPORT")
            }
            "Restore complete"
        }
    }

    private fun installUpdate() {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("APK", "apk")
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        work("Installing...") {
            if (!Adb.connected()) return@work "Phone not connected"
            Adb.run("install", "-r", chooser.selectedFile.absolutePath, timeoutSec = 120)
        }
    }

    private fun remoteProjectDir(): String {
        val name = (fileProject.selectedItem as? String).orEmpty().ifBlank { "Project" }
        val folder = (fileFolder.selectedItem as? String).orEmpty().ifBlank { "Photos" }
        val safe = name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "Project" }
        return "${Paths.REMOTE_PROJECTS}/$safe/$folder"
    }

    private fun sendFile() {
        val chooser = JFileChooser()
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val remote = "${remoteProjectDir()}/${chooser.selectedFile.name}"
        work("Sending file...") {
            Adb.shell("mkdir -p ${remoteProjectDir()}")
            Adb.push(chooser.selectedFile, remote)
            SwingUtilities.invokeLater { loadFiles() }
            "Sent ${chooser.selectedFile.name}"
        }
    }

    private fun recvFile() {
        val remote = JOptionPane.showInputDialog(this, "Remote path", "${remoteProjectDir()}/") ?: return
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return
        work("Receiving file...") {
            Adb.pull(remote, File(chooser.selectedFile, File(remote).name))
            "Pulled"
        }
    }

    private fun fillFileProjects() {
        val names = bundle.optJSONArray("projects")?.objects()?.map { it.optString("name") }?.filter { it.isNotBlank() }.orEmpty()
        fileProject.removeAllItems()
        if (names.isEmpty()) fileProject.addItem("Project")
        names.forEach { fileProject.addItem(it) }
    }

    private fun loadFiles() {
        work("Listing files...") {
            val dir = remoteProjectDir()
            val out = if (Adb.connected()) Adb.shell("ls -la $dir") else "Phone not connected"
            SwingUtilities.invokeLater { fileList.text = "$dir\n\n$out" }
            "OK"
        }
    }

    private fun loadApps() {
        work("Reading packages...") {
            if (!Adb.connected()) return@work "Phone not connected"
            val third = Adb.shell("pm list packages -3").lines().map { it.removePrefix("package:") }.filter { it.isNotBlank() }.toSet()
            val all = Adb.shell("pm list packages").lines().map { it.removePrefix("package:") }.filter { it.isNotBlank() }
            val disabled = Adb.shell("pm list packages -d").lines().map { it.removePrefix("package:") }.toSet()
            SwingUtilities.invokeLater {
                appModel.rowCount = 0
                all.sorted().forEach { pkg ->
                    val kind = when {
                        pkg in required -> "REQUIRED"
                        pkg in third -> "USER"
                        else -> "SYSTEM"
                    }
                    val en = if (pkg in disabled) "no" else "yes"
                    appModel.addRow(arrayOf(pkg.substringAfterLast('.'), pkg, kind, en))
                }
            }
            "OK"
        }
    }

    private fun pkgAction(op: String) {
        val row = appsTable.selectedRow
        if (row < 0) return
        val pkg = appModel.getValueAt(row, 1).toString()
        val kind = appModel.getValueAt(row, 2).toString()
        if (kind == "REQUIRED" && op in setOf("disable", "uninstall", "clear")) {
            JOptionPane.showMessageDialog(this, "Required OS component: $pkg")
            return
        }
        if (op == "uninstall" && kind != "USER") {
            JOptionPane.showMessageDialog(this, "Only user-installed apps can be uninstalled.")
            return
        }
        work("Package $op...") {
            val r = when (op) {
                "enable" -> Adb.shell("pm enable $pkg")
                "disable" -> Adb.shell("pm disable-user --user 0 $pkg")
                "uninstall" -> Adb.shell("pm uninstall $pkg")
                "clear" -> Adb.shell("pm clear $pkg")
                "open" -> Adb.shell("monkey -p $pkg -c android.intent.category.LAUNCHER 1")
                else -> ""
            }
            SwingUtilities.invokeLater { loadApps() }
            r.ifBlank { "OK" }
        }
    }

    private fun runAdv() {
        val cmd = advCmd.text.trim()
        if (cmd.isEmpty()) return
        advPreset(cmd)
    }

    private fun advPreset(cmd: String) {
        advCmd.text = cmd
        work("adb $cmd") {
            val parts = cmd.split(Regex("\\s+"))
            val out = Adb.run(*parts.toTypedArray(), timeoutSec = 30)
            SwingUtilities.invokeLater {
                advLog.append("> adb $cmd\n$out\n\n")
                advLog.caretPosition = advLog.document.length
            }
            "OK"
        }
    }

    private fun refreshConnection() {
        work("Reading device...") {
            val serial = Adb.serial()
            val connected = serial != null
            val lines = mutableListOf<String>()
            if (!connected) {
                lines += "Connection: USB disconnected"
            } else {
                lines += "Connection: USB connected  $serial"
                val device = bundle.optJSONObject("device")
                lines += "Device: " + (device?.optString("name")?.ifBlank { null } ?: Adb.shell("getprop ro.product.model"))
                lines += "Android: " + (device?.optString("android")?.ifBlank { null } ?: Adb.shell("getprop ro.build.version.release"))
                lines += "Build: " + (device?.optString("build")?.ifBlank { null } ?: Adb.shell("getprop ro.build.display.id"))
                lines += "Battery: " + (device?.opt("battery")?.toString() ?: Adb.shell("dumpsys battery | grep level"))
                if (device != null) {
                    val total = device.optLong("storageTotal")
                    val avail = device.optLong("storageAvail")
                    lines += "Storage: ${fmtBytes(avail)} free of ${fmtBytes(total)}"
                    lines += "Available storage: ${fmtBytes(avail)}"
                    lines += "Database size: ${fmtBytes(device.optLong("databaseBytes"))}"
                } else {
                    lines += "Storage: " + Adb.shell("df /data | tail -n 1")
                    lines += "Database size: ${fmtBytes(bundle.toString().toByteArray().size.toLong())}"
                }
                lines += "OS: ${bundle.optString("os", "ProjectOS")} ${bundle.optString("osVersion", "1.0")}"
                lines += "Last sync: " + if (lastSync == 0L) "never" else stamp(lastSync)
                lines += "Backup: local cache ${Paths.localBundle().absolutePath}"
            }
            SwingUtilities.invokeLater {
                status.text = if (connected) "USB connected" else "USB disconnected"
                status.foreground = if (connected) accent else white
                dashInfo.text = lines.joinToString("\n")
            }
            "OK"
        }
    }

    private fun paintData() {
        painting = true
        val q = search.text.trim().lowercase(Locale.US)
        fun match(s: String) = q.isEmpty() || s.lowercase(Locale.US).contains(q)
        projectModel.rowCount = 0
        schedModel.rowCount = 0
        todoModel.rowCount = 0
        noteModel.rowCount = 0
        measModel.rowCount = 0
        pNoteModel.rowCount = 0
        pSchedModel.rowCount = 0
        pTodoModel.rowCount = 0
        val df = SimpleDateFormat("d MMM yyyy HH:mm", Locale.US)
        bundle.optJSONArray("projects")?.objects()?.forEach { p ->
            if (!match(p.optString("name") + p.optString("number"))) return@forEach
            val id = p.optString("id")
            val meas = bundle.optJSONArray("measurements")?.objects()?.count { it.optString("projectId") == id } ?: 0
            val notes = bundle.optJSONArray("notes")?.objects()?.count { it.optString("projectId") == id } ?: 0
            val sched = bundle.optJSONArray("schedules")?.objects()?.count { it.optString("projectId") == id } ?: 0
            val todos = bundle.optJSONArray("todos")?.objects()?.count { it.optString("projectId") == id } ?: 0
            projectModel.addRow(arrayOf(id, p.optString("name"), p.optString("number"), df.format(Date(p.optLong("updatedAt"))), meas, notes, sched, todos))
        }
        bundle.optJSONArray("schedules")?.objects()?.sortedBy { it.optInt("hour") * 60 + it.optInt("minute") }?.forEach { e ->
            val pname = projectName(bundle, e.optString("projectId"))
            val title = e.optString("title")
            if (!match(title + pname)) return@forEach
            val clock = String.format("%02d:%02d", e.optInt("hour"), e.optInt("minute"))
            val day = df.format(Date(e.optLong("dayStart")))
            schedModel.addRow(arrayOf(e.optString("id"), clock, title, if (pname.isBlank()) "" else "[$pname]", day, e.optString("notes"), if (e.optBoolean("done")) "yes" else "no"))
            if (e.optString("projectId") == selectedProjectId) {
                pSchedModel.addRow(arrayOf(e.optString("id"), clock, title, day, e.optString("notes"), if (e.optBoolean("done")) "yes" else "no"))
            }
        }
        bundle.optJSONArray("todos")?.objects()?.forEach { t ->
            val pname = projectName(bundle, t.optString("projectId")).ifBlank { t.optString("project") }
            val title = t.optString("text")
            if (!match(title + pname)) return@forEach
            todoModel.addRow(arrayOf(t.optString("id"), if (t.optBoolean("done")) "yes" else "no", title, if (pname.isBlank()) "" else "[$pname]", df.format(Date(t.optLong("dueAt"))), t.optInt("priority"), t.optString("notes")))
            if (t.optString("projectId") == selectedProjectId) {
                pTodoModel.addRow(arrayOf(t.optString("id"), if (t.optBoolean("done")) "yes" else "no", title, df.format(Date(t.optLong("dueAt"))), t.optInt("priority"), t.optString("notes")))
            }
        }
        bundle.optJSONArray("notes")?.objects()?.forEach { n ->
            val pname = projectName(bundle, n.optString("projectId"))
            val title = n.optString("title").ifBlank { n.optString("body") }
            if (!match(title + pname + n.optString("body"))) return@forEach
            noteModel.addRow(arrayOf(n.optString("id"), title, if (pname.isBlank()) "" else "[$pname]", df.format(Date(n.optLong("updatedAt"))), n.optString("body")))
            if (n.optString("projectId") == selectedProjectId) {
                pNoteModel.addRow(arrayOf(n.optString("id"), title, df.format(Date(n.optLong("updatedAt"))), n.optString("body")))
            }
        }
        val pid = selectedProjectId
        val meas = bundle.optJSONArray("measurements") ?: JSONArray()
        val scoped = JSONArray()
        var itemNo = 0
        meas.objects().forEach { m ->
            if (pid.isNullOrBlank() || m.optString("projectId") != pid) return@forEach
            val pname = projectName(bundle, m.optString("projectId"))
            val desc = m.optString("description")
            if (!match(desc + pname)) return@forEach
            scoped.put(m)
            itemNo += 1
            val auto = m.optBoolean("autoKind", true)
            val kind = MeasureMath.kind(m.optString("length"), m.optString("breadth"), m.optString("height"), auto, m.optString("kind", "AREA"))
            val qty = MeasureMath.label(m.optString("length"), m.optString("breadth"), m.optString("height"), m.optString("uom", "M").lowercase(Locale.US), auto, kind)
            measModel.addRow(arrayOf(m.optString("id"), itemNo, if (pname.isBlank()) "" else "[$pname]", desc, m.optString("length"), m.optString("breadth"), m.optString("height"), m.optString("uom"), kind, qty))
        }
        measTotal.text = "TOTAL  " + MeasureMath.totals(scoped)
        fillFileProjects()
        paintSettings()
        painting = false
    }

    private fun paintSettings() {
        val s = bundle.optJSONObject("settings") ?: JSONObject()
        featureBoxes.forEach { (key, box) -> box.isSelected = s.optBoolean(key, box.isSelected) }
        val pages = s.optJSONArray("pages")
        pagesField.text = if (pages == null) "desk project todo schedule notes tools"
        else (0 until pages.length()).joinToString(" ") { pages.optString(it) }
        settingsArea.text = s.toString(2)
    }

    private fun collectSettings() {
        val s = runCatching { JSONObject(settingsArea.text) }.getOrElse { bundle.optJSONObject("settings") ?: JSONObject() }
        featureBoxes.forEach { (key, box) -> s.put(key, box.isSelected) }
        val pages = JSONArray()
        pagesField.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { pages.put(it) }
        if (pages.length() > 0) s.put("pages", pages)
        bundle.put("settings", s)
        settingsArea.text = s.toString(2)
    }

    private fun onMeasureEdit(row: Int) {
        if (row < 0 || row >= measModel.rowCount) return
        val id = measModel.getValueAt(row, 0).toString()
        val obj = findObj("measurements", id) ?: return
        obj.put("description", measModel.getValueAt(row, 3).toString())
        obj.put("length", measModel.getValueAt(row, 4).toString())
        obj.put("breadth", measModel.getValueAt(row, 5).toString())
        obj.put("height", measModel.getValueAt(row, 6).toString())
        obj.put("uom", measModel.getValueAt(row, 7).toString().uppercase(Locale.US))
        val kind = MeasureMath.kind(obj.optString("length"), obj.optString("breadth"), obj.optString("height"), obj.optBoolean("autoKind", true), obj.optString("kind", "AREA"))
        obj.put("kind", kind)
        painting = true
        measModel.setValueAt(kind, row, 8)
        measModel.setValueAt(MeasureMath.label(obj.optString("length"), obj.optString("breadth"), obj.optString("height"), obj.optString("uom", "M").lowercase(Locale.US), true, kind), row, 9)
        painting = false
        measTotal.text = "TOTAL  " + MeasureMath.totals(
            JSONArray().also { next ->
                bundle.optJSONArray("measurements")?.objects()
                    ?.filter { selectedProjectId.isNullOrBlank() || it.optString("projectId") == selectedProjectId }
                    ?.forEach { next.put(it) }
            }
        )
        Paths.localBundle().writeText(bundle.toString(2))
    }

    private fun editProject(id: String?) {
        val existing = id?.let { findObj("projects", it) }
        val values = prompt(
            if (id == null) "New project" else "Edit project",
            listOf("Name" to existing?.optString("name").orEmpty(), "Number" to existing?.optString("number").orEmpty())
        ) ?: return
        val obj = existing ?: JSONObject().put("id", UUID.randomUUID().toString())
        obj.put("name", values["Name"].orEmpty())
        obj.put("number", values["Number"].orEmpty())
        obj.put("updatedAt", System.currentTimeMillis())
        upsert("projects", obj)
        paintData()
    }

    private fun deleteProject() {
        val id = selectedId(projectTable, projectModel) ?: return
        if (JOptionPane.showConfirmDialog(this, "Delete this project and its measurements, notes, schedules, and todos?", "Confirm", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return
        deleteFrom("projects", id)
        listOf("measurements", "notes", "schedules", "todos").forEach { key ->
            val arr = bundle.optJSONArray(key) ?: return@forEach
            val next = JSONArray()
            arr.objects().filter { it.optString("projectId") != id }.forEach { next.put(it) }
            bundle.put(key, next)
        }
        if (selectedProjectId == id) closeProject() else paintData()
    }

    private fun editSchedule(id: String?, forceProjectId: String? = null) {
        val existing = id?.let { findObj("schedules", it) }
        val preset = forceProjectId ?: existing?.optString("projectId").orEmpty()
        val values = prompt(
            if (id == null) "New schedule" else "Edit schedule",
            listOf(
                "Title" to existing?.optString("title").orEmpty(),
                "Project" to projectName(bundle, preset),
                "Date yyyy-MM-dd" to dayInput(existing?.optLong("dayStart") ?: System.currentTimeMillis()),
                "Time HH:mm" to String.format("%02d:%02d", existing?.optInt("hour") ?: 9, existing?.optInt("minute") ?: 0),
                "Notes" to existing?.optString("notes").orEmpty(),
                "Done yes/no" to if (existing?.optBoolean("done") == true) "yes" else "no"
            )
        ) ?: return
        val time = values["Time HH:mm"].orEmpty().split(":")
        val obj = existing ?: JSONObject().put("id", UUID.randomUUID().toString())
        obj.put("title", values["Title"].orEmpty())
        obj.put("projectId", idForName(values["Project"].orEmpty()))
        obj.put("dayStart", parseDay(values["Date yyyy-MM-dd"].orEmpty()))
        obj.put("hour", time.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9)
        obj.put("minute", time.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0)
        obj.put("timeLabel", String.format("%02d:%02d", obj.optInt("hour"), obj.optInt("minute")))
        obj.put("notes", values["Notes"].orEmpty())
        obj.put("done", values["Done yes/no"].orEmpty().equals("yes", true))
        upsert("schedules", obj)
        paintData()
    }

    private fun editTodo(id: String?, forceProjectId: String? = null) {
        val existing = id?.let { findObj("todos", it) }
        val preset = forceProjectId ?: existing?.optString("projectId").orEmpty()
        val values = prompt(
            if (id == null) "New todo" else "Edit todo",
            listOf(
                "Title" to existing?.optString("text").orEmpty(),
                "Project" to projectName(bundle, preset).ifBlank { existing?.optString("project").orEmpty() },
                "Due yyyy-MM-dd" to dayInput(existing?.optLong("dueAt") ?: System.currentTimeMillis()),
                "Priority 0-2" to (existing?.optInt("priority") ?: 1).toString(),
                "Notes" to existing?.optString("notes").orEmpty(),
                "Done yes/no" to if (existing?.optBoolean("done") == true) "yes" else "no"
            )
        ) ?: return
        val obj = existing ?: JSONObject().put("id", UUID.randomUUID().toString())
        val project = values["Project"].orEmpty()
        obj.put("text", values["Title"].orEmpty())
        obj.put("projectId", idForName(project))
        obj.put("project", project)
        obj.put("dueAt", parseDay(values["Due yyyy-MM-dd"].orEmpty()))
        obj.put("priority", values["Priority 0-2"].orEmpty().toIntOrNull()?.coerceIn(0, 2) ?: 1)
        obj.put("notes", values["Notes"].orEmpty())
        obj.put("done", values["Done yes/no"].orEmpty().equals("yes", true))
        upsert("todos", obj)
        paintData()
    }

    private fun editNote(id: String?, forceProjectId: String? = null) {
        val existing = id?.let { findObj("notes", it) }
        val preset = forceProjectId ?: existing?.optString("projectId").orEmpty()
        val values = prompt(
            if (id == null) "New note" else "Edit note",
            listOf(
                "Title" to existing?.optString("title").orEmpty(),
                "Project" to projectName(bundle, preset),
                "Body" to existing?.optString("body").orEmpty()
            )
        ) ?: return
        val obj = existing ?: JSONObject().put("id", UUID.randomUUID().toString()).put("createdAt", System.currentTimeMillis())
        obj.put("title", values["Title"].orEmpty())
        obj.put("projectId", idForName(values["Project"].orEmpty()))
        obj.put("body", values["Body"].orEmpty())
        obj.put("updatedAt", System.currentTimeMillis())
        upsert("notes", obj)
        paintData()
    }

    private fun editMeasure(id: String?) {
        val pid = selectedProjectId ?: existingProjectOf(id)
        if (pid.isNullOrBlank()) {
            JOptionPane.showMessageDialog(this, "Open a project to use Measurement Book.")
            return
        }
        val existing = id?.let { findObj("measurements", it) }
        val values = prompt(
            if (id == null) "New measurement" else "Edit measurement",
            listOf(
                "Description" to existing?.optString("description").orEmpty(),
                "Length" to existing?.optString("length").orEmpty(),
                "Breadth" to existing?.optString("breadth").orEmpty(),
                "Height" to existing?.optString("height").orEmpty(),
                "UOM" to existing?.optString("uom", "M").orEmpty().ifBlank { "M" }
            )
        ) ?: return
        val obj = existing ?: JSONObject().put("id", UUID.randomUUID().toString()).put("autoKind", true)
        obj.put("projectId", pid)
        obj.put("description", values["Description"].orEmpty())
        obj.put("length", values["Length"].orEmpty())
        obj.put("breadth", values["Breadth"].orEmpty())
        obj.put("height", values["Height"].orEmpty())
        obj.put("uom", values["UOM"].orEmpty().ifBlank { "M" }.uppercase(Locale.US))
        obj.put("kind", MeasureMath.kind(obj.optString("length"), obj.optString("breadth"), obj.optString("height"), true, "AREA"))
        upsert("measurements", obj)
        paintData()
    }

    private fun existingProjectOf(id: String?): String? =
        id?.let { findObj("measurements", it)?.optString("projectId") }

    private fun upsert(key: String, obj: JSONObject) {
        val arr = bundle.optJSONArray(key) ?: JSONArray().also { bundle.put(key, it) }
        val id = obj.optString("id")
        var found = false
        for (i in 0 until arr.length()) {
            if (arr.optJSONObject(i)?.optString("id") == id) {
                arr.put(i, obj)
                found = true
                break
            }
        }
        if (!found) arr.put(obj)
        Paths.localBundle().writeText(bundle.toString(2))
    }

    private fun deleteFrom(key: String, id: String?) {
        if (id.isNullOrBlank()) return
        val arr = bundle.optJSONArray(key) ?: return
        val next = JSONArray()
        arr.objects().filter { it.optString("id") != id }.forEach { next.put(it) }
        bundle.put(key, next)
        Paths.localBundle().writeText(bundle.toString(2))
        paintData()
    }

    private fun findObj(key: String, id: String): JSONObject? =
        bundle.optJSONArray(key)?.objects()?.firstOrNull { it.optString("id") == id }

    private fun selectedId(table: JTable, model: DefaultTableModel): String? {
        val view = table.selectedRow
        if (view < 0) return null
        val row = table.convertRowIndexToModel(view)
        return model.getValueAt(row, 0)?.toString()
    }

    private fun idForName(name: String): String {
        if (name.isBlank()) return ""
        val clean = name.trim().removePrefix("[").removeSuffix("]")
        return bundle.optJSONArray("projects")?.objects()?.firstOrNull { it.optString("name").equals(clean, true) }?.optString("id").orEmpty()
    }

    private fun prompt(title: String, fields: List<Pair<String, String>>): Map<String, String>? {
        val panel = JPanel(GridLayout(0, 2, 8, 8)).apply { background = black }
        val inputs = linkedMapOf<String, JTextField>()
        fields.forEach { (label, value) ->
            panel.add(JLabel(label).apply { foreground = white })
            val field = JTextField(value).apply {
                background = black
                foreground = white
                caretColor = accent
            }
            inputs[label] = field
            panel.add(field)
        }
        val ok = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
        if (ok != JOptionPane.OK_OPTION) return null
        return inputs.mapValues { it.value.text }
    }

    private fun work(msg: String, block: () -> String) {
        status.text = msg
        status.foreground = accent
        object : SwingWorker<String, Void>() {
            override fun doInBackground(): String = runCatching { block() }.getOrElse { it.message ?: "Error" }
            override fun done() {
                status.text = runCatching { get() }.getOrElse { it.message ?: "Error" }
                status.foreground = white
            }
        }.execute()
    }

    private fun confirm(msg: String, act: () -> Unit) {
        if (JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            work(msg) { act(); "OK" }
        }
    }

    private fun navBtn(text: String, onClick: () -> Unit) = JButton(text).apply {
        alignmentX = LEFT_ALIGNMENT
        background = black
        foreground = white
        font = fontUi
        isBorderPainted = false
        isFocusPainted = false
        horizontalAlignment = SwingConstants.LEFT
        maximumSize = Dimension(180, 32)
        addActionListener { onClick() }
    }

    private fun accentBtn(text: String, onClick: () -> Unit) = JButton(text).apply {
        background = accent
        foreground = black
        font = fontSmall
        isFocusPainted = false
        addActionListener { onClick() }
    }

    private fun plainBtn(text: String, onClick: () -> Unit) = JButton(text).apply {
        background = black
        foreground = white
        font = fontSmall
        isFocusPainted = false
        addActionListener { onClick() }
    }

    private fun tableModel(vararg cols: String) = object : DefaultTableModel(cols, 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    private fun hideId(table: JTable) {
        if (table.columnCount > 0) table.removeColumn(table.columnModel.getColumn(0))
    }

    private fun JTable.darkTable(): JTable = apply {
        background = black
        foreground = white
        gridColor = accent
        tableHeader.background = black
        tableHeader.foreground = accent
        selectionBackground = accent
        selectionForeground = black
        font = fontSmall
        rowHeight = 24
    }

    private fun JScrollPane.dark(): JScrollPane = apply {
        border = BorderFactory.createLineBorder(accent)
        viewport.background = black
        background = black
    }

    private fun styleCombo(box: JComboBox<*>) {
        box.background = black
        box.foreground = white
    }

    private fun stamp(ms: Long = System.currentTimeMillis()) =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(ms))

    private fun dayInput(ms: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    private fun parseDay(s: String): Long {
        val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)?.time }.getOrNull()
        return parsed ?: System.currentTimeMillis()
    }

    private fun fmtBytes(n: Long): String = when {
        n >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", n / 1_000_000_000.0)
        n >= 1_000_000 -> String.format(Locale.US, "%.1f MB", n / 1_000_000.0)
        n >= 1_000 -> String.format(Locale.US, "%.1f KB", n / 1_000.0)
        else -> "$n B"
    }
}
