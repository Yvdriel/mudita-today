package com.mosquishe.today

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mosquishe.today.ui.nav.AppShell
import com.mosquishe.today.ui.theme.TodayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodayTheme {
                AppShell()
            }
        }
    }
}
