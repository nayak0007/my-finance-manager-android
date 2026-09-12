package com.myfinancemanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.login(email, password) {} },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.contains("@") && password.length >= 6
        ) { Text("Log in") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val demo = email.ifBlank { "demo@finance.app" }
                viewModel.loginGoogle(demo, demo.substringBefore("@")) {}
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue with Google") }
        TextButton(onClick = onSignup, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Create an account")
        }
    }
}

@Composable
fun SignupScreen(
    viewModel: AppViewModel,
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
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.signUp(email, password, name) {} },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.contains("@") && password.length >= 6
        ) { Text("Sign up") }
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
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
