package com.finploit.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.currencyConfigByCode
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.FinanceItemDto
import com.finploit.android.data.dto.ShoppingListDto
import com.finploit.android.ui.grocery.GrocerySearchScreen
import com.finploit.android.ui.grocery.GrocerySearchViewModel
import com.finploit.android.ui.pantry.PantryScreen
import com.finploit.android.ui.pantry.PantryViewModel
import com.finploit.android.ui.analysis.AnalysisScreen
import com.finploit.android.ui.mealplanner.MealPlannerScreen
import com.finploit.android.ui.analysis.NotificationsScreen
import com.finploit.android.ui.analysis.NotificationsViewModel
import com.finploit.android.ui.dashboard.DashboardScreen
import com.finploit.android.ui.goals.AddGoalScreen
import com.finploit.android.ui.goals.GoalsScreen
import com.finploit.android.ui.profile.ProfileScreen
import com.finploit.android.ui.recurring.AddRecurringScreen
import com.finploit.android.ui.shopping.AddShoppingListScreen
import com.finploit.android.ui.shopping.ShoppingListDetailScreen
import com.finploit.android.ui.shopping.ShoppingScreen
import com.finploit.android.ui.shopping.ShoppingViewModel
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.transactions.AddTransactionScreen
import com.finploit.android.ui.transactions.EditTransactionScreen
import com.finploit.android.ui.transactions.TransactionsScreen

sealed class BottomNav(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : BottomNav("dashboard", "Início", Icons.Default.Dashboard)
    data object Transactions : BottomNav("transactions", "Finanças", Icons.Default.List)
    data object Goals : BottomNav("goals", "Metas", Icons.Default.EmojiEvents)
    data object Shopping : BottomNav("shopping", "Compras", Icons.Default.ShoppingCart)
    data object Meals : BottomNav("meals", "Comida", Icons.Default.RestaurantMenu)
    data object Analysis : BottomNav("analysis", "Análise", Icons.Default.BarChart)
}

val bottomNavItems = listOf(
    BottomNav.Dashboard,
    BottomNav.Transactions,
    BottomNav.Goals,
    BottomNav.Shopping,
    BottomNav.Meals,
    BottomNav.Analysis,
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showAddTransaction by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddShoppingList by remember { mutableStateOf(false) }
    var showAddRecurring by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showGrocerySearch by remember { mutableStateOf(false) }
    var showPantry by remember { mutableStateOf(false) }
    var groceryInitialItems by remember { mutableStateOf<List<EnrichItem>>(emptyList()) }
    var groceryListTabLabel by remember { mutableStateOf("Lista de Itens") }
    var selectedShoppingList by remember { mutableStateOf<ShoppingListDto?>(null) }
    var editingTransaction by remember { mutableStateOf<FinanceItemDto?>(null) }

    val shoppingViewModel: ShoppingViewModel = hiltViewModel()
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val groceryViewModel: GrocerySearchViewModel = hiltViewModel()
    val recurringDueCount by mainViewModel.recurringDueCount.collectAsState()
    val currencyCode by mainViewModel.currencyCode.collectAsState()
    val currencyConfig = currencyConfigByCode(currencyCode)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            groceryViewModel.detectLocation()
        }
    }

    CompositionLocalProvider(LocalCurrencyConfig provides currencyConfig) {
        when {
            showProfile ->
                ProfileScreen(viewModel = hiltViewModel(), onLogout = onLogout, onBack = { showProfile = false })
            showNotifications ->
                NotificationsScreen(viewModel = notificationsViewModel, onBack = { showNotifications = false })
            showPantry ->
                PantryScreen(viewModel = hiltViewModel(), onBack = { showPantry = false })
            showGrocerySearch ->
                GrocerySearchScreen(
                    viewModel = groceryViewModel,
                    initialItems = groceryInitialItems,
                    listTabLabel = groceryListTabLabel,
                    onBack = { showGrocerySearch = false },
                    requestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                )
            showAddTransaction ->
                AddTransactionScreen(viewModel = hiltViewModel(), onDismiss = { showAddTransaction = false })
            editingTransaction != null ->
                EditTransactionScreen(
                    tx = editingTransaction!!,
                    viewModel = hiltViewModel(),
                    onDismiss = { editingTransaction = null },
                )
            showAddGoal ->
                AddGoalScreen(viewModel = hiltViewModel(), onDismiss = { showAddGoal = false })
            showAddShoppingList ->
                AddShoppingListScreen(viewModel = shoppingViewModel, onDismiss = { showAddShoppingList = false })
            showAddRecurring ->
                AddRecurringScreen(viewModel = hiltViewModel(), onDismiss = { showAddRecurring = false })
            selectedShoppingList != null -> {
                shoppingViewModel.selectList(selectedShoppingList!!)
                ShoppingListDetailScreen(
                    viewModel = shoppingViewModel,
                    onBack = { selectedShoppingList = null },
                    onSearchPrices = { items ->
                        groceryListTabLabel = "Lista de Compras"
                        groceryInitialItems = items
                        groceryViewModel.clearEnrichedItems()
                        showGrocerySearch = true
                    },
                )
            }
            else -> {
                val showFab = currentRoute in listOf(
                    BottomNav.Transactions.route,
                    BottomNav.Goals.route,
                    BottomNav.Shopping.route,
                )
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = CardBackground) {
                            bottomNavItems.forEach { item ->
                                val showBadge = item == BottomNav.Transactions && recurringDueCount > 0
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        if (showBadge) {
                                            BadgedBox(badge = { Badge { Text(recurringDueCount.toString()) } }) {
                                                Icon(item.icon, contentDescription = item.label)
                                            }
                                        } else {
                                            Icon(item.icon, contentDescription = item.label)
                                        }
                                    },
                                    label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                                    alwaysShowLabel = false,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GreenPrimary,
                                        selectedTextColor = GreenPrimary,
                                        unselectedIconColor = TextDisabled,
                                        unselectedTextColor = TextDisabled,
                                        indicatorColor = GreenPrimary.copy(alpha = 0.15f),
                                    )
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (showFab) {
                            FloatingActionButton(
                                onClick = {
                                    when (currentRoute) {
                                        BottomNav.Transactions.route -> showAddTransaction = true
                                        BottomNav.Goals.route -> showAddGoal = true
                                        BottomNav.Shopping.route -> showAddShoppingList = true
                                    }
                                },
                                containerColor = GreenPrimary,
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = BackgroundDark)
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNav.Dashboard.route,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(BottomNav.Dashboard.route) {
                            DashboardScreen(
                                viewModel = hiltViewModel(),
                                onProfileClick = { showProfile = true },
                                onNotificationsClick = { showNotifications = true },
                                onAddRecurringClick = { showAddRecurring = true },
                            )
                        }
                        composable(BottomNav.Transactions.route) {
                            TransactionsScreen(
                                viewModel = hiltViewModel(),
                                onEditTransaction = { editingTransaction = it },
                            )
                        }
                        composable(BottomNav.Goals.route) {
                            GoalsScreen(viewModel = hiltViewModel())
                        }
                        composable(BottomNav.Shopping.route) {
                            ShoppingScreen(
                                viewModel = shoppingViewModel,
                                onListClick = { selectedShoppingList = it },
                            )
                        }
                        composable(BottomNav.Meals.route) {
                            MealPlannerScreen(
                                viewModel = hiltViewModel(),
                                onSearchPrices = { items ->
                                    groceryListTabLabel = "Plano Alimentar"
                                    groceryInitialItems = items
                                    groceryViewModel.clearEnrichedItems()
                                    showGrocerySearch = true
                                },
                                onPantryClick = { showPantry = true },
                            )
                        }
                        composable(BottomNav.Analysis.route) {
                            AnalysisScreen(viewModel = hiltViewModel())
                        }
                    }
                }
            }
        }
    }
}
