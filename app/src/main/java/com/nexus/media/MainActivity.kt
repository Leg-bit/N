package com.nexus.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nexus.media.core.theme.NexusTheme
import com.nexus.media.ui.NexusApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as NexusApplication).container
        setContent { NexusTheme { NexusApp(container) } }
    }
}
