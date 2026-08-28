package com.silverguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.silverguard.app.ui.SilverGuardApp
import com.silverguard.app.ui.SilverGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilverGuardTheme {
                SilverGuardApp()
            }
        }
    }
}
