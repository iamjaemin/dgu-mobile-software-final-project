package com.example.dgumobilesoftwarefinalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.example.dgumobilesoftwarefinalproject.ui.MealApp
import com.example.dgumobilesoftwarefinalproject.ui.MealTheme
import com.example.dgumobilesoftwarefinalproject.ui.Orange

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Orange.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Orange.toArgb()))
        setContent { MealTheme { MealApp() } }
    }
}
