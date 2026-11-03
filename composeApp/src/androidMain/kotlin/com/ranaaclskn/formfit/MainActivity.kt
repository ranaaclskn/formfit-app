package com.ranaaclskn.formfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ranaaclskn.formfit.data.AndroidContext
import com.ranaaclskn.formfit.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContext.app = applicationContext
        enableEdgeToEdge()
        setContent { App() }
    }
}
