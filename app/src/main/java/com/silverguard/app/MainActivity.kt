package com.silverguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.silverguard.app.ui.SilverGuardApp
import com.silverguard.app.ui.SilverGuardTheme
import com.silverguard.app.ui.SilverGuardViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SilverGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilverGuardTheme {
                SilverGuardApp(viewModel)
            }
        }
    }
}
