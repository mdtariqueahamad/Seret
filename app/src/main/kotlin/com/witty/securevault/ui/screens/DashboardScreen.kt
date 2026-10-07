package com.witty.securevault.ui.screens
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import com.witty.securevault.R
import com.witty.securevault.backend.VaultCredential
import com.witty.securevault.backend.VaultCredentialDetail
import com.witty.securevault.ui.theme.*
import com.witty.securevault.ui.components.NotificationState
import com.witty.securevault.ui.components.NotificationType
import com.witty.securevault.ui.components.OverlayNotificationHost
import com.witty.securevault.viewmodel.VaultSection
import com.witty.securevault.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: VaultViewModel) {
    val credentials by viewModel.credentials.collectAsState()
    val currentSection by viewModel.section.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    var notificationState by remember { mutableStateOf(NotificationState()) }

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var editingCredentialId by remember { mutableStateOf<Long?>(null) }
    var selectedCredentialId by remember { mutableStateOf<Long?>(null) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    var activeChip by remember { mutableStateOf("All") }

    val filteredCredentials = remember(credentials, searchQuery, currentSection, activeChip) {
        var list = credentials
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            list = list.filter {
                it.website.lowercase().contains(query) ||
                        it.username.lowercase().contains(query)
            }
        }
        
        list = when (currentSection) {
            VaultSection.WEAK -> list.filter { it.passwordLength < 8 }
            VaultSection.FAVORITES -> list.filter { it.isFavorite }
            else -> list
        }
        
        if (currentSection == VaultSection.ALL && activeChip != "All") {
            list = list.filter { it.category.ifBlank { "Other" } == activeChip }
        }
        
        list
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            notificationState = NotificationState(errorMessage, NotificationType.ERROR, true)
            kotlinx.coroutines.delay(1500)
            notificationState = NotificationState()
            viewModel.resetError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawerSheet(
                currentSection = currentSection,
                credentialsCount = credentials.size,
                favoritesCount = credentials.count { it.isFavorite },
                weakCount = credentials.count { it.passwordLength < 8 },
                isDarkTheme = isDarkTheme,
                onSectionSelected = { 
                    viewModel.selectSection(it)
                    coroutineScope.launch { drawerState.close() }
                },
                onClose = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingCredentialId = null
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = CircleShape,
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add credential")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                com.witty.securevault.ui.components.AuroraBackground(isDarkTheme = isDarkTheme, modifier = Modifier.fillMaxSize())
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TopBar(
                        onMenuClick = { coroutineScope.launch { drawerState.open() } }
                    )

                    VaultSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        isDarkTheme = isDarkTheme
                    )

                    // CATEGORY CHIPS
                    if (currentSection == VaultSection.ALL) {
                        val allCategories = listOf("All") + credentials.map { it.category.ifBlank { "Other" } }.distinct().sorted()
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp)
                        ) {
                            items(allCategories) { cat ->
                                val selected = activeChip == cat
                                Surface(
                                    modifier = Modifier
                                        .clickable { activeChip = cat }
                                        .then(if (selected) Modifier.shadow(2.dp, RoundedCornerShape(100.dp)) else Modifier),
                                    shape = RoundedCornerShape(100.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else if(isDarkTheme) DarkBgInput else LightBgInput,
                                    border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder)
                                ) {
                                    Text(
                                        text = cat, 
                                        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (currentSection == VaultSection.MORE) {
                        MoreSettingsScreen(viewModel, isDarkTheme, modifier = Modifier.fillMaxSize().weight(1f), onShowNotification = { msg, type -> notificationState = NotificationState(msg, type, true); coroutineScope.launch { kotlinx.coroutines.delay(1500); notificationState = NotificationState() } })
                    } else if (filteredCredentials.isEmpty()) {
                        val msg = if (currentSection == VaultSection.WEAK) "All Secure\nNo weak passwords found." else "No passwords saved yet"
                        EmptyState(message = msg, modifier = Modifier.fillMaxSize().weight(1f))
                    } else {
                        CredentialList(
                            credentials = filteredCredentials,
                            currentSection = currentSection,
                            modifier = Modifier.fillMaxSize().weight(1f),
                            onCardClick = { id ->
                                selectedCredentialId = id
                                showDetailDialog = true
                            },
                            onToggleFavorite = { id, isFavorite ->
                                viewModel.toggleFavorite(id, !isFavorite)
                            },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }

    if (showDetailDialog && selectedCredentialId != null) {
        CredentialDetailDialog(
            viewModel = viewModel,
            credentialId = selectedCredentialId!!,
            context = context,
            isDarkTheme = isDarkTheme,
            onShowNotification = { msg, type -> 
                notificationState = NotificationState(msg, type, true)
                coroutineScope.launch { kotlinx.coroutines.delay(1500); notificationState = NotificationState() }
            },
            onDismiss = { showDetailDialog = false; selectedCredentialId = null },
            onEdit = { id -> showDetailDialog = false; selectedCredentialId = null; editingCredentialId = id; showAddEditDialog = true },
            onDelete = { id -> viewModel.deleteCredential(id); showDetailDialog = false; selectedCredentialId = null }
        )
    }

    OverlayNotificationHost(state = notificationState, isDarkTheme = isDarkTheme)

    if (showAddEditDialog) {
        AddEditCredentialDialog(
            viewModel = viewModel,
            editId = editingCredentialId,
            isDarkTheme = isDarkTheme,
            autoFavorite = (currentSection == VaultSection.FAVORITES && editingCredentialId == null),
            onShowNotification = { msg, type -> 
                notificationState = NotificationState(msg, type, true)
                coroutineScope.launch { kotlinx.coroutines.delay(1500); notificationState = NotificationState() }
            },
            onDismiss = { showAddEditDialog = false; editingCredentialId = null },
            onSaved = { showAddEditDialog = false; editingCredentialId = null }
        )
    }
}

// ════════════════════════════════════════════════════════════
// Sidebar Drawer
// ════════════════════════════════════════════════════════════
@Composable
private fun SidebarDrawerSheet(
    currentSection: VaultSection,
    credentialsCount: Int,
    favoritesCount: Int,
    weakCount: Int,
    isDarkTheme: Boolean,
    onSectionSelected: (VaultSection) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.70f))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    0.5.dp,
                    if(isDarkTheme) DarkGlassBorder else LightGlassBorder,
                    RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "SERET",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = com.witty.securevault.ui.theme.OrangeAvenueFont,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "liga, dlig, calt")
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Menu",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                DrawerItem(
                    title = "All Passwords",
                    icon = Icons.AutoMirrored.Outlined.List,
                    badgeCount = credentialsCount,
                    isSelected = currentSection == VaultSection.ALL,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSectionSelected(VaultSection.ALL) }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                DrawerItem(
                    title = "Favourites",
                    icon = Icons.Outlined.Star,
                    badgeCount = favoritesCount,
                    isSelected = currentSection == VaultSection.FAVORITES,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSectionSelected(VaultSection.FAVORITES) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )

                DrawerItem(
                    title = "Weak Passwords",
                    icon = Icons.Outlined.Warning,
                    badgeCount = weakCount,
                    isSelected = currentSection == VaultSection.WEAK,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSectionSelected(VaultSection.WEAK) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )

                DrawerItem(
                    title = "More",
                    icon = Icons.Outlined.MoreVert,
                    badgeCount = 0,
                    isSelected = currentSection == VaultSection.MORE,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSectionSelected(VaultSection.MORE) }
                )
                
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.56f else 0.72f),
                    border = BorderStroke(0.5.dp, if (isDarkTheme) DarkGlassBorder else LightGlassBorder)
                ) {
                    Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    Text(
                        text = "SERET",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = com.witty.securevault.ui.theme.OrangeAvenueFont,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "liga, dlig, calt")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Secured by SERET",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "(Md Tarique)",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FooterLinkButton(iconRes = R.drawable.logo_instagram, description = "Instagram") {
                            openExternalUrl(context, "https://www.instagram.com/_.euphricx._/")
                        }
                        FooterLinkButton(iconRes = R.drawable.logo_linkedin, description = "LinkedIn") {
                            openExternalUrl(context, "https://www.linkedin.com/in/md-tarique-ahamad-92952732b/")
                        }
                        FooterLinkButton(iconRes = R.drawable.logo_github, description = "GitHub") {
                            openExternalUrl(context, "https://github.com/mdtariqueahamad")
                        }
                    }
                    Text(
                        text = "Feedback: mdtariqbist@gmail.com",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable { openExternalUrl(context, "mailto:mdtariqbist@gmail.com") }
                    )
                    Text(
                        text = "Version 2.0.1",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.34f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun FooterLinkButton(iconRes: Int, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = description,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
private fun DrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Int,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
    
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else contentColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (badgeCount > 0) {
            Surface(
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Top Bar
// ════════════════════════════════════════════════════════════
@Composable
private fun TopBar(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Open Menu",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "SERET",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = com.witty.securevault.ui.theme.OrangeAvenueFont,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "liga, dlig, calt")
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

// ════════════════════════════════════════════════════════════
// Search Bar
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSearchBar(query: String, onQueryChange: (String) -> Unit, isDarkTheme: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search passwords...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// Credential List
// ════════════════════════════════════════════════════════════
@Composable
private fun CredentialList(
    credentials: List<VaultCredential>,
    currentSection: VaultSection,
    modifier: Modifier = Modifier,
    onCardClick: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    var selectedCategory by remember(currentSection) { mutableStateOf<String?>(null) }

    if (currentSection == VaultSection.MORE) {
        val grouped = credentials.groupBy { it.category.ifBlank { "Uncategorized" } }
        
        if (selectedCategory == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                items(grouped.keys.toList()) { category ->
                    CategoryFolderCard(category, grouped[category]?.size ?: 0, isDarkTheme) {
                        selectedCategory = category
                    }
                }
            }
        } else {
            Column(modifier = modifier) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { selectedCategory = null },
                    color = Color.Transparent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedCategory!!, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                val items = grouped[selectedCategory] ?: emptyList()
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 20.dp).weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(items, key = { it.id }) { credential ->
                        CredentialCard(
                            credential = credential,
                            onClick = { onCardClick(credential.id) },
                            onToggleFavorite = { onToggleFavorite(credential.id, credential.isFavorite) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(credentials, key = { it.id }) { credential ->
                CredentialCard(
                    credential = credential,
                    onClick = { onCardClick(credential.id) },
                    onToggleFavorite = { onToggleFavorite(credential.id, credential.isFavorite) },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Credential Card
// ════════════════════════════════════════════════════════════
@Composable
private fun CredentialCard(
    credential: VaultCredential,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val logoRes = com.witty.securevault.ui.components.LogoMapper.getLogoRes(credential.website)
            if (logoRes != null) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = credential.website.firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = credential.website,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = credential.username,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (credential.category.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = credential.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (credential.passwordLength in 1..7) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Weak",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = if (credential.isFavorite) "Unfavorite" else "Favorite",
                    tint = if (credential.isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Empty State
// ════════════════════════════════════════════════════════════
@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_seret_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .alpha(0.15f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                letterSpacing = 0.3.sp
            )
            if (message == "No passwords saved yet") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add one",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Credential Detail Dialog
// ════════════════════════════════════════════════════════════
@Composable
private fun CredentialDetailDialog(
    viewModel: VaultViewModel,
    credentialId: Long,
    context: Context,
    isDarkTheme: Boolean,
    onShowNotification: (String, NotificationType) -> Unit,
    onDismiss: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val detail = remember(credentialId) { viewModel.credentialDetail(credentialId) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp))
        ) {
            if (detail == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Unable to load credential", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val logoRes = com.witty.securevault.ui.components.LogoMapper.getLogoRes(detail.website)
                        if (logoRes != null) {
                            Image(
                                painter = painterResource(logoRes),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = detail.website,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    DetailField(label = "Username", value = detail.username, isDarkTheme = isDarkTheme)
                    if (detail.websiteUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Website URL", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkTheme) DarkBgInput else LightBgInput,
                            border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder),
                            modifier = Modifier.fillMaxWidth().clickable {
                                val url = if (detail.websiteUrl.startsWith("http://") || detail.websiteUrl.startsWith("https://")) detail.websiteUrl else "https://${detail.websiteUrl}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detail.websiteUrl, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = "Open Link",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Password", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) DarkBgInput else LightBgInput,
                        border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (passwordVisible) detail.password else "•".repeat(detail.password.length.coerceAtMost(20)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                onClick = { passwordVisible = !passwordVisible },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(if (passwordVisible) "Hide" else "Show", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                                        var copyState by remember { mutableStateOf(false) }
                    LaunchedEffect(copyState) {
                        if (copyState) {
                            kotlinx.coroutines.delay(1500)
                            copyState = false
                        }
                    }

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("password", detail.password))
                            copyState = true
                        },
                        modifier = Modifier.animateContentSize()
                    ) {
                        androidx.compose.animation.Crossfade(targetState = copyState, label = "copyAnim") { copied ->
                            if (copied) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Copied", color = Color(0xFF34C759), fontSize = 14.sp)
                                }
                            } else {
                                Text("Copy Password", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            }
                        }
                    }

                    if (detail.category.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailField(label = "Category", value = detail.category, isDarkTheme = isDarkTheme)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEdit(detail.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Credential", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("This action cannot be undone. Are you sure?", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)) },
            confirmButton = { TextButton(onClick = { 
                showDeleteConfirm = false; 
                onDelete(credentialId)
                onShowNotification("Credential Deleted", NotificationType.DELETE)
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)) } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun DetailField(label: String, value: String, isDarkTheme: Boolean) {
    Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) DarkBgInput else LightBgInput,
        border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
    }
}

// ════════════════════════════════════════════════════════════
// Add / Edit Credential Dialog
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCredentialDialog(
    viewModel: VaultViewModel,
    editId: Long?,
    isDarkTheme: Boolean,
    autoFavorite: Boolean = false,
    onShowNotification: (String, NotificationType) -> Unit,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val existingDetail = remember(editId) { editId?.let { viewModel.credentialDetail(it) } }

    var website by remember { mutableStateOf(existingDetail?.website ?: "") }
    var websiteUrl by remember { mutableStateOf(existingDetail?.websiteUrl ?: "") }
    var username by remember { mutableStateOf(existingDetail?.username ?: "") }
    var password by remember { mutableStateOf(existingDetail?.password ?: "") }
    var category by remember { mutableStateOf(existingDetail?.category ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (editId != null) "Edit Credential" else "Add Credential", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        FormField("Website / Service", website, { website = it }, "e.g. instagram", isDarkTheme)
                        Spacer(modifier = Modifier.height(14.dp))
                        FormField("Website URL (Optional)", websiteUrl, { websiteUrl = it }, "e.g. https://instagram.com/login", isDarkTheme)
                        Spacer(modifier = Modifier.height(14.dp))
                        FormField("Username / Email", username, { username = it }, "e.g. user@example.com", isDarkTheme)
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        var showSuggestConfirm by remember { mutableStateOf(false) }

                        FormField("Password", password, { password = it }, "Enter password", isDarkTheme, isPassword = true)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                if (editId != null && existingDetail?.password?.isNotEmpty() == true && password == existingDetail.password) {
                                    showSuggestConfirm = true
                                } else {
                                    password = viewModel.generatePassword()
                                }
                            }
                        ) {
                            val genPassColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF000000)
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = genPassColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Suggest Password", color = genPassColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (showSuggestConfirm) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showSuggestConfirm = false },
                                title = { Text("Are you sure?") },
                                text = { Text("This will replace the currently saved password. Maybe it's by mistake?", color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f)) },
                                confirmButton = { 
                                    TextButton(onClick = { password = viewModel.generatePassword(); showSuggestConfirm = false }) { 
                                        Text("Replace") 
                                    } 
                                },
                                dismissButton = { 
                                    TextButton(onClick = { showSuggestConfirm = false }) { 
                                        Text("Cancel") 
                                    } 
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        FormField("Category", category, { category = it }, "e.g. Social, Work, Finance", isDarkTheme)

                        if (validationError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                when {
                                    website.isBlank() -> validationError = "Website is required"
                                    username.isBlank() -> validationError = "Username is required"
                                    password.isBlank() -> validationError = "Password is required"
                                    else -> {
                                        validationError = null
                                        if (viewModel.saveCredential(editId, website.trim(), websiteUrl.trim(), username.trim(), password, category.trim(), autoFavorite)) {
                                            onShowNotification(if (editId != null) "Credential Updated" else "Credential Saved", NotificationType.SUCCESS)
                                            onSaved()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                        ) {
                            Text(if (editId != null) "Update" else "Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, isDarkTheme: Boolean, isPassword: Boolean = false) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
        shape = RoundedCornerShape(14.dp), color = if (isDarkTheme) DarkBgInput else LightBgInput,
        border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder), modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), fontSize = 14.sp) },
                visualTransformation = if (isPassword && !passwordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    val iconRes = if (passwordVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_secure
                    // Fallback using icons if possible, wait let's use material icons.
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle Password Visibility",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSettingsScreen(viewModel: VaultViewModel, isDarkTheme: Boolean, modifier: Modifier = Modifier, onShowNotification: (String, NotificationType) -> Unit) {
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var changeError by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeMode by ThemeManager.themeMode.collectAsState()

    Column(
        modifier = modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("More", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        MoreActionBlock(
            title = "Change Master Password",
            subtitle = "Update the key that secures your vault",
            onClick = { showPasswordDialog = true }
        )
        MoreActionBlock(
            title = "Theme",
            subtitle = "Current: ${themeMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
            onClick = { showThemeDialog = true }
        )
    }

    if (showPasswordDialog) {
        Dialog(onDismissRequest = { showPasswordDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                border = BorderStroke(0.5.dp, if (isDarkTheme) DarkGlassBorder else LightGlassBorder),
                modifier = Modifier.shadow(16.dp, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Change Master Password", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    FormField("New Password", newPass, { newPass = it; changeError = null }, "Enter new password", isDarkTheme, isPassword = true)
                    FormField("Confirm Password", confirmPass, { confirmPass = it; changeError = null }, "Re-enter new password", isDarkTheme, isPassword = true)
                    changeError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showPasswordDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (viewModel.updateMasterPassword(newPass, confirmPass)) {
                                    onShowNotification("Password Updated", NotificationType.SUCCESS)
                                    newPass = ""
                                    confirmPass = ""
                                    showPasswordDialog = false
                                } else changeError = "Unable to update password"
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Update") }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChoiceBlock("Light", themeMode == ThemeMode.LIGHT) { ThemeManager.setTheme(ThemeMode.LIGHT); showThemeDialog = false }
                    ThemeChoiceBlock("Dark", themeMode == ThemeMode.DARK) { ThemeManager.setTheme(ThemeMode.DARK); showThemeDialog = false }
                    ThemeChoiceBlock("System default", themeMode == ThemeMode.SYSTEM) { ThemeManager.setTheme(ThemeMode.SYSTEM); showThemeDialog = false }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
        )
    }

    }

@Composable
private fun MoreActionBlock(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(5.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ThemeChoiceBlock(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Text(title, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onBackground, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun CategoryFolderCard(category: String, count: Int, isDarkTheme: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, if(isDarkTheme) DarkGlassBorder else LightGlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(category, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count items", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}
