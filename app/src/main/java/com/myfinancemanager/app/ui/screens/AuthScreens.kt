package com.myfinancemanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.ui.AppViewModel

/**
 * The sign-in buttons stay disabled and show a spinner for the whole round trip.
 *
 * That matters here more than in most apps: the backend runs on a free tier that spins down when
 * idle, so the first request of a session can take close to a minute. Without a visible busy
 * state the screen looks frozen and people tap again, which fires a duplicate request.
 */
@Composable
private fun SubmitButton(
    text: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !busy
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}

@Composable
private fun BusyHint(busy: Boolean) {
    if (!busy) return
    Spacer(Modifier.height(10.dp))
    Text(
        "Talking to the server… The free hosting plan wakes on the first request, which can take up to a minute.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("My Finance Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Track income, spend, and investments in one place.")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        SubmitButton(
            text = "Log in",
            busy = busy,
            enabled = email.contains("@") && password.isNotEmpty(),
            onClick = { viewModel.login(email, password) {} }
        )
        BusyHint(busy)
        Spacer(Modifier.height(8.dp))
        // Neon Auth can do Google sign-in, but only through a browser redirect this app does not
        // implement yet. Reporting that clearly is better than the old behaviour of minting a
        // local-only fake session that no server had ever issued.
        OutlinedButton(
            onClick = { viewModel.loginGoogle {} },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue with Google") }
        TextButton(
            onClick = onSignup,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Create an account")
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
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display name") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 8)") },
            singleLine = true,
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        // The backend enforces @Size(min = 8) on registration, so the button must not invite
        // a 6- or 7-character password only to fail with a 400 afterwards.
        SubmitButton(
            text = "Sign up",
            busy = busy,
            enabled = email.contains("@") && password.length >= 8,
            onClick = { viewModel.signUp(email, password, name) {} }
        )
        BusyHint(busy)
        TextButton(
            onClick = onLogin,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Already have an account")
        }
    }
}

@Composable
fun OnboardingScreen(onFinished: (enableSms: Boolean, enableNotify: Boolean) -> Unit) {
    var sms by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Permissions are optional. You can change them later in Settings.")
        Spacer(Modifier.height(20.dp))
        Text("SMS auto-capture reads bank alerts on this device, then parks them in a review queue. Nothing is uploaded until you confirm.")
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { sms = !sms }, modifier = Modifier.fillMaxWidth()) {
            Text(if (sms) "SMS capture: on" else "Enable SMS capture")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { notify = !notify }, modifier = Modifier.fillMaxWidth()) {
            Text(if (notify) "Notifications: on" else "Enable notifications")
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onFinished(sms, notify) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue to dashboard")
        }
    }
}
