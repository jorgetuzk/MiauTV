package com.telegramtv.ui.mobile

import androidx.compose.runtime.Composable
import com.telegramtv.ui.theme.MiauTVMobileTheme

@Composable
fun MobileApp(startDestination: String = "login") {
    MiauTVMobileTheme {
        MobileScaffold(startDestination = startDestination)
    }
}


