package com.qixev.qfieldkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.qixev.qfieldkit.ui.inventory.InventoryViewModel
import com.qixev.qfieldkit.ui.navigation.FieldKitNavHost
import com.qixev.qfieldkit.ui.theme.QFieldKitTheme

class MainActivity : ComponentActivity() {

    private val inventoryViewModel: InventoryViewModel by viewModels {
        val app = application as FieldKitApplication
        InventoryViewModel.Factory(app.inventoryRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QFieldKitTheme {
                val navController = rememberNavController()
                FieldKitNavHost(
                    navController = navController,
                    inventoryViewModel = inventoryViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
