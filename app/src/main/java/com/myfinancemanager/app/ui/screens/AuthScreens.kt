package com.myfinancemanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.ui.AppViewModel
import com.myfinancemanager.app.ui.components.FilledTextField
import com.myfinancemanager.app.ui.components.PillButton
import com.myfinancemanager.app.ui.components.PillButtonVariant
import com.myfinancemanager.app.ui.theme.AxioLime
import com.myfinancemanager.app.ui.theme.AxioLimeDeep
import com.myfinancemanager.app.ui.theme.InkBlack
import com.myfinancemanager.app.ui.theme.LocalMoneyColors

@Composable
private fun BrandMark() {
    Box(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(AxioLime, AxioLimeDeep))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = InkBlack
        )
    }
}

@Composable
private fun AuthError(error: String?) {
    if (error.isNullOrBlank()) return
    val money = LocalMoneyColors.current
    Spacer(Modifier.height(12.dp))
    Text(error, color = money.negative, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun BusyHint(busy: Boolean) {
    if (!busy) return
    Spacer(Modifier.height(12.dp))
    Text(
        "Talking to the server. The free hosting plan wakes on the first request, which can take up to a minute.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    busy: Boolean,
    error: String?,
    onSignup: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        BrandMark()
        Spacer(Modifier.height(24.dp))
        Text(
            "My Finance Manager",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Track income, spend, and investments in one place.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        FilledTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(16.dp))
        FilledTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation()
        )
        AuthError(error)
        Spacer(Modifier.height(24.dp))
        PillButton(
            "Log in",
            onClick = { viewModel.login(email, password) {} },
            modifier = Modifier.fillMaxWidth(),
            variant = PillButtonVariant.Lime,
            enabled = email.contains("@") && password.isNotEmpty(),
            loading = busy
        )
        BusyHint(busy)
        Spacer(Modifier.height(12.dp))
        PillButton(
            "Continue with Google",
            onClick = { viewModel.loginGoogle {} },
            modifier = Modifier.fillMaxWidth(),
            variant = PillButtonVariant.Outlined,
            enabled = !busy
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSignup, enabled = !busy) {
            Text("Create an account", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SignupScreen(
    viewModel: AppViewModel,
    busy: Boolean,
    error: String?,
    onLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        BrandMark()
        Spacer(Modifier.height(20.dp))
        Text(
            "Create account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(24.dp))
        FilledTextField(name, { name = it }, label = "Display name", enabled = !busy)
        Spacer(Modifier.height(16.dp))
        FilledTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(16.dp))
        FilledTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password (min 8)",
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation()
        )
        AuthError(error)
        Spacer(Modifier.height(24.dp))
        // The backend enforces @Size(min = 8) on registration, so the button must not invite
        // a 6- or 7-character password only to fail with a 400 afterwards.
        PillButton(
            "Sign up",
            onClick = { viewModel.signUp(email, password, name) {} },
            modifier = Modifier.fillMaxWidth(),
            variant = PillButtonVariant.Lime,
            enabled = email.contains("@") && password.length >= 8,
            loading = busy
        )
        BusyHint(busy)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onLogin, enabled = !busy) {
            Text("Already have an account", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OnboardingScreen(onFinished: (enableSms: Boolean, enableNotify: Boolean) -> Unit) {
    var sms by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(true) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandMark()
        Spacer(Modifier.height(24.dp))
        Text("Welcome", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            "Permissions are optional. You can change them later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "SMS auto-capture reads bank alerts on this device, then parks them in a review queue. Nothing is uploaded until you confirm.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PillButton(
            if (sms) "SMS capture: on" else "Enable SMS capture",
            onClick = { sms = !sms },
            modifier = Modifier.fillMaxWidth(),
            variant = if (sms) PillButtonVariant.Lime else PillButtonVariant.Outlined
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            if (notify) "Notifications: on" else "Enable notifications",
            onClick = { notify = !notify },
            modifier = Modifier.fillMaxWidth(),
            variant = if (notify) PillButtonVariant.Lime else PillButtonVariant.Outlined
        )
        Spacer(Modifier.height(24.dp))
        PillButton(
            "Continue to dashboard",
            onClick = { onFinished(sms, notify) },
            modifier = Modifier.fillMaxWidth(),
            variant = PillButtonVariant.Lime
        )
    }
}
