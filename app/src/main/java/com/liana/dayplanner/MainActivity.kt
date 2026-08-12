package com.liana.dayplanner

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liana.dayplanner.brief.BriefController
import com.liana.dayplanner.data.Category
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.data.VisionCheckStore
import com.liana.dayplanner.data.WeeklyReflectionStore
import com.liana.dayplanner.reminder.DailyNudge
import com.liana.dayplanner.ui.screens.BriefOverlay
import com.liana.dayplanner.ui.screens.CalendarScreen
import com.liana.dayplanner.ui.screens.CompletionNoteDialog
import com.liana.dayplanner.ui.screens.MeetingScreen
import com.liana.dayplanner.ui.screens.ResearchScreen
import com.liana.dayplanner.ui.screens.StatsScreen
import com.liana.dayplanner.ui.screens.TaskEditorSheet
import com.liana.dayplanner.ui.screens.TasksScreen
import com.liana.dayplanner.ui.screens.TodayScreen
import com.liana.dayplanner.ui.screens.VisionScreen
import com.liana.dayplanner.ui.screens.WeeklyReflectionScreen
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.Ink
import com.liana.dayplanner.ui.theme.IvoryFaint
import com.liana.dayplanner.ui.theme.MeridianTheme
import com.liana.dayplanner.ui.theme.OnChampagne
import com.liana.dayplanner.ui.theme.Surface1
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.WbSunny),
    TASKS("Tasks", Icons.Outlined.ChecklistRtl),
    CALENDAR("Calendar", Icons.Outlined.CalendarMonth),
    VISION("Vision", Icons.Outlined.TrackChanges),
    STATS("Progress", Icons.Outlined.Insights),
    RESEARCH("Research", Icons.Outlined.Science)
}

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private var pendingBrief = mutableStateOf(false)
    private var pendingScreen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Location powers accurate local weather in the morning brief.
        locationPermission.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
        DailyNudge.ensureChannel(this)
        DailyNudge.scheduleAll(this)
        pendingBrief.value = intent?.getBooleanExtra("startBrief", false) == true
        pendingScreen.value = intent?.getStringExtra("openScreen")
        setContent {
            MeridianTheme {
                ArcApp(pendingBrief, pendingScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("startBrief", false)) pendingBrief.value = true
        intent.getStringExtra("openScreen")?.let { pendingScreen.value = it }
    }
}

@Composable
private fun ArcApp(
    pendingBrief: androidx.compose.runtime.MutableState<Boolean>,
    pendingScreen: androidx.compose.runtime.MutableState<String?>,
    viewModel: PlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(Screen.TODAY) }
    var showReflection by remember { mutableStateOf(false) }
    var showMeeting by remember { mutableStateOf(false) }
    LaunchedEffect(pendingScreen.value) {
        when (pendingScreen.value) {
            "calendar" -> screen = Screen.CALENDAR
            "today" -> screen = Screen.TODAY
            "reflection" -> showReflection = true
            "meeting" -> showMeeting = true
            else -> return@LaunchedEffect
        }
        pendingScreen.value = null
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var focusTask by remember { mutableStateOf<Task?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showCurrentAffairs by remember { mutableStateOf(false) }
    var completedTask by remember { mutableStateOf<CompletedTask?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val briefController = remember { BriefController(context.applicationContext) }

    // First-run onboarding
    if (!viewModel.settings.onboarded) {
        com.liana.dayplanner.ui.screens.OnboardingScreen(
            initial = viewModel.settings,
            onFinish = { s, visionText ->
                viewModel.saveSettings(s)
                if (visionText.isNotBlank())
                    viewModel.saveVision(viewModel.vision.copy(statement = visionText))
                com.liana.dayplanner.reminder.DailyNudge.scheduleAll(context)
            }
        )
        return
    }

    // Daily auto-backup + keep the brief's vision statement current
    LaunchedEffect(Unit) { viewModel.autoBackupIfNeeded() }
    LaunchedEffect(viewModel.vision.statement) {
        briefController.visionStatement = viewModel.vision.statement
    }

    LaunchedEffect(Unit) {
        viewModel.undoEvents.collect { op ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = op.label, actionLabel = "Undo",
                    duration = androidx.compose.material3.SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) op.undo()
            }
        }
    }

    // After completing a task, offer to note its outcome.
    LaunchedEffect(Unit) {
        viewModel.completedForNote.collect { completedTask = it }
    }

    LaunchedEffect(pendingBrief.value, tasks) {
        if (pendingBrief.value && tasks.isNotEmpty()) {
            pendingBrief.value = false
            briefController.start(scope, tasks)
        } else if (pendingBrief.value && tasks.isEmpty()) {
            // still start after a beat even with no tasks loaded yet
            pendingBrief.value = false
            briefController.start(scope, tasks)
        }
    }

    // Back button: unwind any open overlay first, then return to Today,
    // then require a second press within 2s to actually exit the app.
    val activity = context as? ComponentActivity
    var lastBackAt by remember { mutableStateOf(0L) }
    BackHandler {
        when {
            completedTask != null -> completedTask = null
            briefController.visible -> briefController.end()
            editorOpen -> editorOpen = false
            focusTask != null -> focusTask = null
            showSettings -> showSettings = false
            showCurrentAffairs -> showCurrentAffairs = false
            showReflection -> showReflection = false
            showMeeting -> showMeeting = false
            screen != Screen.TODAY -> screen = Screen.TODAY
            else -> {
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastBackAt < 2000) {
                    activity?.finish()
                } else {
                    lastBackAt = nowMs
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri) { ok ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) "Backup saved." else "Backup failed.")
            }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri) { ok ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) "Backup restored." else "Not a valid ARC backup.")
            }
        }
    }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                Screen.entries.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s,
                        onClick = { screen = s },
                        icon = { Icon(s.icon, s.label) },
                        label = { Text(s.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnChampagne,
                            selectedTextColor = Champagne,
                            indicatorColor = Champagne,
                            unselectedIconColor = IvoryFaint,
                            unselectedTextColor = IvoryFaint
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (screen != Screen.STATS && screen != Screen.RESEARCH && !briefController.visible && !showMeeting) {
                FloatingActionButton(
                    onClick = { editing = null; editorOpen = true },
                    containerColor = Champagne,
                    contentColor = OnChampagne
                ) {
                    Icon(Icons.Rounded.Add, "Add task")
                }
            }
        }
    ) { innerPadding ->
        val padding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding()
        )
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.TODAY -> TodayScreen(
                    tasks = tasks,
                    onToggle = viewModel::toggleDone,
                    onEdit = { editing = it; editorOpen = true },
                    onTriage = viewModel::triage,
                    onStartBrief = { briefController.start(scope, tasks) },
                    onVoiceSettings = { briefController.openSettings(scope) },
                    onFocus = { viewModel.setInProgress(it, true); focusTask = it },
                    onOpenSettings = { showSettings = true },
                    onOpenCurrentAffairs = { showCurrentAffairs = true },
                    onOpenMeeting = { showMeeting = true },
                    onReorderPlan = { from, to, blocks -> viewModel.reorderPlan(blocks, from, to) },
                    settings = viewModel.settings,
                    contentPadding = padding
                )
                Screen.TASKS -> TasksScreen(
                    tasks = tasks,
                    onToggle = viewModel::toggleDone,
                    onEdit = { editing = it; editorOpen = true },
                    contentPadding = padding
                )
                Screen.CALENDAR -> CalendarScreen(
                    tasks = tasks,
                    selectedDate = selectedDate,
                    onSelectDate = { selectedDate = it },
                    onToggle = viewModel::toggleDone,
                    onEdit = { editing = it; editorOpen = true },
                    contentPadding = padding
                )
                Screen.VISION -> VisionScreen(
                    vision = viewModel.vision,
                    tasks = tasks,
                    onSave = viewModel::saveVision,
                    onVisionCheck = viewModel::saveVisionCheck,
                    onMarkReviewed = { VisionCheckStore.saveLastReview(context) },
                    initialTodayCheck = VisionCheckStore.getTodayCheck(context),
                    daysSinceReview = VisionCheckStore.daysSinceLastReview(context),
                    contentPadding = padding
                )
                Screen.STATS -> StatsScreen(
                    tasks = tasks,
                    onExport = {
                        exportLauncher.launch("arc-backup-${LocalDate.now()}.json")
                    },
                    onImport = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    contentPadding = padding
                )
                Screen.RESEARCH -> ResearchScreen(
                    projects = viewModel.researchProjects,
                    onUpdateProject = viewModel::updateResearchProject,
                    onAddProject = viewModel::addResearchProject,
                    onDeleteProject = viewModel::deleteResearchProject,
                    contentPadding = padding
                )
            }

            BriefOverlay(briefController)

            com.liana.dayplanner.ui.screens.FocusOverlay(
                task = focusTask,
                onComplete = { t -> viewModel.toggleDone(t); focusTask = null },
                onDismiss = { focusTask = null }
            )

            if (showSettings) {
                com.liana.dayplanner.ui.screens.SettingsScreen(
                    settings = viewModel.settings,
                    onSave = { viewModel.saveSettings(it)
                        com.liana.dayplanner.reminder.DailyNudge.scheduleAll(context) },
                    onClose = { showSettings = false }
                )
            }

            if (showCurrentAffairs) {
                val (caDate, caList) = briefController.capturedHeadlines()
                com.liana.dayplanner.ui.screens.CurrentAffairsScreen(
                    date = caDate,
                    headlines = caList,
                    onSaveForRevision = { viewModel.addRevisionTask(it) },
                    onClose = { showCurrentAffairs = false }
                )
            }

            if (showReflection) {
                val existing = WeeklyReflectionStore.load(context)
                    .firstOrNull { it.weekOf == WeeklyReflectionStore.currentWeekOf() }
                WeeklyReflectionScreen(
                    existing = existing,
                    onSave = { r -> viewModel.saveReflection(r); showReflection = false },
                    onClose = { showReflection = false }
                )
            }

            if (showMeeting) {
                MeetingScreen(
                    tasks = tasks,
                    researchProjects = viewModel.researchProjects,
                    onToggle = viewModel::toggleDone,
                    onEdit = { editing = it; editorOpen = true },
                    onAddWorkTask = viewModel::addMeetingWorkTask,
                    onToggleResearchTask = viewModel::toggleResearchTask,
                    onAddResearchTask = viewModel::addResearchTask,
                    onClose = { showMeeting = false }
                )
            }

            completedTask?.let { ct ->
                CompletionNoteDialog(
                    taskTitle = ct.previous.title,
                    onSave = { note -> viewModel.saveOutcome(ct.id, note); completedTask = null },
                    onSkip = { completedTask = null },
                    onUndo = { viewModel.undoComplete(ct); completedTask = null }
                )
            }
        }

        if (editorOpen) {
            TaskEditorSheet(
                existing = editing,
                defaultDate = if (screen == Screen.CALENDAR) selectedDate else LocalDate.now(),
                defaultCategory = if (screen == Screen.VISION) Category.VISION else Category.WORK,
                vision = viewModel.vision,
                onSave = { viewModel.save(it); editorOpen = false },
                onDelete = { viewModel.delete(it); editorOpen = false },
                onDismiss = { editorOpen = false }
            )
        }
    }
}
