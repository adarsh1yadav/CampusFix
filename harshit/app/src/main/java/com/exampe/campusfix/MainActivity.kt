package com.exampe.campusfix

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen navigation routes for CampusFix.
 */
enum class AppScreen {
    LOGIN,
    DASHBOARD,
    NEW_COMPLAINT,
    MY_COMPLAINTS,
    ADMIN_DASHBOARD
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FirebaseRepository(applicationContext)

        setContent {
            CampusFixApp(repository = repository)
        }
    }
}

// =============================================================================
// COLOR PALETTE & HELPERS
// =============================================================================

val PrimaryBlue = Color(0xFF1E88E5)
val PrimaryDark = Color(0xFF1565C0)
val BackgroundGray = Color(0xFFF5F7FA)
val SurfaceWhite = Color(0xFFFFFFFF)

val StatusPending = Color(0xFFE65100)
val StatusPendingBg = Color(0xFFFFF3E0)

val StatusProgress = Color(0xFF1565C0)
val StatusProgressBg = Color(0xFFE3F2FD)

val StatusResolved = Color(0xFF2E7D32)
val StatusResolvedBg = Color(0xFFE8F5E9)

fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        "Recent"
    }
}

// =============================================================================
// ROOT COMPOSABLE
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusFixApp(repository: FirebaseRepository) {
    val context = LocalContext.current
    var currentScreen by remember {
        mutableStateOf(
            if (repository.getCurrentUser() != null) {
                val email = repository.getCurrentUser()?.email
                if (AdminConfig.isAdmin(email)) AppScreen.ADMIN_DASHBOARD else AppScreen.DASHBOARD
            } else {
                AppScreen.LOGIN
            }
        )
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            onPrimary = Color.White,
            surface = SurfaceWhite,
            background = BackgroundGray
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.LOGIN -> LoginScreen(
                    repository = repository,
                    onLoginSuccess = { email ->
                        if (AdminConfig.isAdmin(email)) {
                            currentScreen = AppScreen.ADMIN_DASHBOARD
                        } else {
                            currentScreen = AppScreen.DASHBOARD
                        }
                    }
                )

                AppScreen.DASHBOARD -> DashboardScreen(
                    repository = repository,
                    onNavigateToNewComplaint = { currentScreen = AppScreen.NEW_COMPLAINT },
                    onNavigateToMyComplaints = { currentScreen = AppScreen.MY_COMPLAINTS },
                    onLogout = {
                        repository.logout()
                        currentScreen = AppScreen.LOGIN
                    }
                )

                AppScreen.NEW_COMPLAINT -> {
                    BackHandler { currentScreen = AppScreen.DASHBOARD }
                    NewComplaintScreen(
                        repository = repository,
                        onBack = { currentScreen = AppScreen.DASHBOARD },
                        onSuccess = {
                            Toast.makeText(context, "Complaint submitted successfully!", Toast.LENGTH_SHORT).show()
                            currentScreen = AppScreen.DASHBOARD
                        }
                    )
                }

                AppScreen.MY_COMPLAINTS -> {
                    BackHandler { currentScreen = AppScreen.DASHBOARD }
                    MyComplaintsScreen(
                        repository = repository,
                        onBack = { currentScreen = AppScreen.DASHBOARD }
                    )
                }

                AppScreen.ADMIN_DASHBOARD -> {
                    AdminDashboardScreen(
                        repository = repository,
                        onLogout = {
                            repository.logout()
                            currentScreen = AppScreen.LOGIN
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 1: LOGIN / REGISTER
// =============================================================================

@Composable
fun LoginScreen(
    repository: FirebaseRepository,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isFirebaseConfigured = remember { repository.isConfigured }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo / Icon Header
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PrimaryBlue,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "CampusFix Logo",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CampusFix",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryDark
        )
        Text(
            text = "College Complaint Management App",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Warning banner if Firebase configuration is missing
        if (!isFirebaseConfigured) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusPendingBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = StatusPending
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Firebase not detected. Please add google-services.json to the 'app' directory to enable live backend.",
                        fontSize = 12.sp,
                        color = StatusPending
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Login / Register Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Toggle Tabs
                TabRow(
                    selectedTabIndex = if (isRegisterMode) 1 else 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = !isRegisterMode,
                        onClick = { isRegisterMode = false; errorMessage = null },
                        text = { Text("Sign In") }
                    )
                    Tab(
                        selected = isRegisterMode,
                        onClick = { isRegisterMode = true; errorMessage = null },
                        text = { Text("Register") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("College Email") },
                    placeholder = { Text("student@campus.edu") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message display
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button (Login / Register)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both email and password."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                            return@Button
                        }

                        // Check for fixed admin credentials shortcut
                        if (email.trim().equals(AdminConfig.ADMIN_EMAIL, ignoreCase = true) &&
                            password == AdminConfig.ADMIN_PASSWORD
                        ) {
                            // If live Firebase Auth has admin registered, sign in; otherwise direct route
                            isLoading = true
                            repository.login(email, password) { result ->
                                isLoading = false
                                result.onSuccess {
                                    onLoginSuccess(AdminConfig.ADMIN_EMAIL)
                                }
                                result.onFailure {
                                    // Fallback for hackathon demo if admin user not registered in Auth
                                    Toast.makeText(context, "Logged in as Admin (Hackathon Mode)", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(AdminConfig.ADMIN_EMAIL)
                                }
                            }
                            return@Button
                        }

                        isLoading = true
                        if (isRegisterMode) {
                            repository.register(email, password) { result ->
                                isLoading = false
                                result.onSuccess { user ->
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(user.email ?: email)
                                }
                                result.onFailure { err ->
                                    errorMessage = err.localizedMessage ?: "Registration failed."
                                }
                            }
                        } else {
                            repository.login(email, password) { result ->
                                isLoading = false
                                result.onSuccess { user ->
                                    onLoginSuccess(user.email ?: email)
                                }
                                result.onFailure { err ->
                                    errorMessage = err.localizedMessage ?: "Invalid credentials or user not found."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "Create Account" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Demo Shortcut for Hackathon Judges & Admin Testing
        OutlinedButton(
            onClick = {
                email = AdminConfig.ADMIN_EMAIL
                password = AdminConfig.ADMIN_PASSWORD
                isRegisterMode = false
                errorMessage = null
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fill Admin Demo Credentials", fontSize = 13.sp)
        }
    }
}

// =============================================================================
// SCREEN 2: STUDENT DASHBOARD
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: FirebaseRepository,
    onNavigateToNewComplaint: () -> Unit,
    onNavigateToMyComplaints: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser = repository.getCurrentUser()
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.getMyComplaints { result ->
            isLoading = false
            result.onSuccess { complaints = it }
        }
    }

    val totalCount = complaints.size
    val pendingCount = complaints.count { it.status.equals(ComplaintStatuses.PENDING, ignoreCase = true) }
    val inProgressCount = complaints.count { it.status.equals(ComplaintStatuses.IN_PROGRESS, ignoreCase = true) }
    val resolvedCount = complaints.count { it.status.equals(ComplaintStatuses.RESOLVED, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CampusFix", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Welcome Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome Back,",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = currentUser?.email ?: "Student",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track campus issues and get them resolved quickly.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Complaint Status Summary
            Text(
                text = "My Complaint Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricCard(
                    title = "Total",
                    count = totalCount,
                    color = PrimaryBlue,
                    bgColor = PrimaryBlue.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "Pending",
                    count = pendingCount,
                    color = StatusPending,
                    bgColor = StatusPendingBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "In Progress",
                    count = inProgressCount,
                    color = StatusProgress,
                    bgColor = StatusProgressBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "Resolved",
                    count = resolvedCount,
                    color = StatusResolved,
                    bgColor = StatusResolvedBg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Actions Section
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action 1: Submit Complaint Button Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNewComplaint() }
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddComment, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Submit Complaint",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Report classroom, electricity, water, or clean issues",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: View My Complaints Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMyComplaints() }
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, tint = PrimaryBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Complaints",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                        Text(
                            text = "View history and track real-time resolution status",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SummaryMetricCard(
    title: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================================
// SCREEN 3: COMPLAINT FORM
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewComplaintScreen(
    repository: FirebaseRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var selectedCategory by remember { mutableStateOf(ComplaintCategories.CLASSROOM) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit Complaint", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {

                    // 1. Category Dropdown
                    Text(
                        text = "Category",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            ComplaintCategories.list.forEach { categoryItem ->
                                DropdownMenuItem(
                                    text = { Text(categoryItem) },
                                    onClick = {
                                        selectedCategory = categoryItem
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Location Field
                    Text(
                        text = "Location",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            formError = null
                        },
                        placeholder = { Text("e.g. Science Block, Room 304") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Description Field (Max 300 characters)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = "${description.length} / 300",
                            fontSize = 12.sp,
                            color = if (description.length > 300) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            if (it.length <= 300) {
                                description = it
                                formError = null
                            }
                        },
                        placeholder = { Text("Briefly describe the issue...") },
                        minLines = 4,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (formError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (location.isBlank()) {
                                formError = "Please enter the location."
                                return@Button
                            }
                            if (description.isBlank()) {
                                formError = "Please describe the problem."
                                return@Button
                            }

                            isSubmitting = true
                            repository.submitComplaint(
                                category = selectedCategory,
                                location = location,
                                description = description
                            ) { result ->
                                isSubmitting = false
                                result.onSuccess {
                                    onSuccess()
                                }
                                result.onFailure { err ->
                                    formError = err.localizedMessage ?: "Failed to submit complaint. Check network/Firebase."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 4: MY COMPLAINTS LIST
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyComplaintsScreen(
    repository: FirebaseRepository,
    onBack: () -> Unit
) {
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        isLoading = true
        errorMessage = null
        repository.getMyComplaints { result ->
            isLoading = false
            result.onSuccess { complaints = it }
            result.onFailure { err -> errorMessage = err.localizedMessage }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Complaints", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = errorMessage ?: "Failed to load complaints", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { refresh() }) {
                            Text("Retry")
                        }
                    }
                }
                complaints.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No complaints found", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                        Text("Any issues you submit will appear here.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(complaints, key = { it.id }) { complaint ->
                            ComplaintCard(complaint = complaint)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 5: ADMIN DASHBOARD
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: FirebaseRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var statusUpdatingId by remember { mutableStateOf<String?>(null) }

    fun refreshComplaints() {
        isLoading = true
        repository.getAllComplaints { result ->
            isLoading = false
            result.onSuccess { complaints = it }
            result.onFailure { err ->
                Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshComplaints()
    }

    val filteredComplaints = when (selectedFilter) {
        ComplaintStatuses.PENDING -> complaints.filter { it.status.equals(ComplaintStatuses.PENDING, ignoreCase = true) }
        ComplaintStatuses.IN_PROGRESS -> complaints.filter { it.status.equals(ComplaintStatuses.IN_PROGRESS, ignoreCase = true) }
        ComplaintStatuses.RESOLVED -> complaints.filter { it.status.equals(ComplaintStatuses.RESOLVED, ignoreCase = true) }
        else -> complaints
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CampusFix Admin", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshComplaints() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf("All", ComplaintStatuses.PENDING, ComplaintStatuses.IN_PROGRESS, ComplaintStatuses.RESOLVED)
                filters.forEach { filterText ->
                    val isSelected = selectedFilter == filterText
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filterText },
                        label = { Text(filterText, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredComplaints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No complaints found for '$selectedFilter'", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredComplaints, key = { it.id }) { complaint ->
                        AdminComplaintCard(
                            complaint = complaint,
                            isUpdating = statusUpdatingId == complaint.id,
                            onStatusChange = { newStatus ->
                                statusUpdatingId = complaint.id
                                repository.updateComplaintStatus(complaint.id, newStatus) { result ->
                                    statusUpdatingId = null
                                    result.onSuccess {
                                        Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                                        refreshComplaints()
                                    }
                                    result.onFailure { err ->
                                        Toast.makeText(context, "Update failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENT: STUDENT COMPLAINT CARD
// =============================================================================

@Composable
fun ComplaintCard(complaint: Complaint) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = complaint.category,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                StatusBadge(status = complaint.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = complaint.location,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = complaint.description,
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Timestamp
            Text(
                text = formatDate(complaint.timestamp),
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
    }
}

// =============================================================================
// REUSABLE COMPONENT: ADMIN COMPLAINT CARD
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintCard(
    complaint: Complaint,
    isUpdating: Boolean,
    onStatusChange: (String) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Student Email & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.userEmail,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                StatusBadge(status = complaint.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Category: ${complaint.category} • Location: ${complaint.location}",
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = complaint.description,
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(complaint.timestamp),
                fontSize = 11.sp,
                color = Color.Gray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFEEEEEE))

            // Status Update Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Change Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Box {
                        OutlinedButton(
                            onClick = { isMenuOpen = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(complaint.status, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false }
                        ) {
                            ComplaintStatuses.all.forEach { statusOption ->
                                DropdownMenuItem(
                                    text = { Text(statusOption) },
                                    onClick = {
                                        isMenuOpen = false
                                        if (statusOption != complaint.status) {
                                            onStatusChange(statusOption)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENT: STATUS BADGE
// =============================================================================

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        ComplaintStatuses.PENDING -> Pair(StatusPendingBg, StatusPending)
        ComplaintStatuses.IN_PROGRESS -> Pair(StatusProgressBg, StatusProgress)
        ComplaintStatuses.RESOLVED -> Pair(StatusResolvedBg, StatusResolved)
        else -> Pair(Color(0xFFEEEEEE), Color.DarkGray)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
