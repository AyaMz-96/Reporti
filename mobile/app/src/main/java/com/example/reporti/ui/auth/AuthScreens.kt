package com.example.reporti.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.AuthViewModel

private enum class AuthScreen { LOGIN, REGISTER, FORGOT }

/** Affiche la connexion tant que l'utilisateur n'est pas connecte, puis ton contenu. */
@Composable
fun AuthGate(vm: AuthViewModel = viewModel(), content: @Composable () -> Unit) {
    val state = vm.state
    when {
        !state.ready && state.user == null ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.user == null -> AuthFlow(vm)
        else -> content()
    }
}

@Composable
private fun AuthFlow(vm: AuthViewModel) {
    var screen by remember { mutableStateOf(AuthScreen.LOGIN) }
    fun go(s: AuthScreen) { vm.clearFeedback(); screen = s }

    when (screen) {
        AuthScreen.LOGIN -> LoginScreen(vm, onRegister = { go(AuthScreen.REGISTER) }, onForgot = { go(AuthScreen.FORGOT) })
        AuthScreen.REGISTER -> RegisterScreen(vm, onLogin = { go(AuthScreen.LOGIN) })
        AuthScreen.FORGOT -> ForgotPasswordScreen(vm, onBack = { go(AuthScreen.LOGIN) })
    }
}

@Composable
private fun AuthLayout(title: String, subtitle: String, vm: AuthViewModel, body: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        vm.state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
        vm.state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
        body()
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String,
                  type: KeyboardType = KeyboardType.Text, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else type),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PrimaryButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        Text(if (loading) "Please wait…" else text)
    }
}

@Composable
private fun LoginScreen(vm: AuthViewModel, onRegister: () -> Unit, onForgot: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var local by remember { mutableStateOf<String?>(null) }

    AuthLayout("Welcome back", "Log in to follow your reports.", vm) {
        local?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
        Field(email, { email = it }, "Email", KeyboardType.Email)
        Field(password, { password = it }, "Password", password = true)
        PrimaryButton("Log in", vm.state.loading) {
            local = if (!email.contains("@") || password.isBlank()) "Enter your email and password." else null
            if (local == null) vm.login(email, password)
        }
        TextButton(onClick = onForgot) { Text("Forgot password?") }
        TextButton(onClick = onRegister) { Text("Create an account") }
    }
}

@Composable
private fun RegisterScreen(vm: AuthViewModel, onLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var local by remember { mutableStateOf<String?>(null) }

    AuthLayout("Create your account", "Report issues and track their progress.", vm) {
        local?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
        Field(name, { name = it }, "Full name")
        Field(email, { email = it }, "Email", KeyboardType.Email)
        Field(phone, { phone = it }, "Phone (8 digits, optional)", KeyboardType.Phone)
        Field(password, { password = it }, "Password", password = true)
        Field(confirm, { confirm = it }, "Confirm password", password = true)
        PrimaryButton("Create account", vm.state.loading) {
            local = when {
                name.isBlank() -> "Full name is required."
                !email.contains("@") -> "Enter a valid email."
                phone.isNotBlank() && !Regex("^[0-9]{8}$").matches(phone.trim()) -> "Phone must be 8 digits."
                password.length < 8 -> "Password must have at least 8 characters."
                password != confirm -> "Passwords do not match."
                else -> null
            }
            if (local == null) vm.register(name, email, phone.trim().ifBlank { null }, password)
        }
        TextButton(onClick = onLogin) { Text("Already registered? Log in") }
    }
}

@Composable
private fun ForgotPasswordScreen(vm: AuthViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var local by remember { mutableStateOf<String?>(null) }

    AuthLayout("Forgot your password?", "Enter your email and we will send you a reset link.", vm) {
        local?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
        Field(email, { email = it }, "Email", KeyboardType.Email)
        PrimaryButton("Send reset link", vm.state.loading) {
            local = if (!email.contains("@")) "Enter a valid email." else null
            if (local == null) vm.forgotPassword(email)
        }
        TextButton(onClick = onBack) { Text("Back to login") }
    }
}

/** Le "dashboard" : profil + deconnexion. A brancher sur ton onglet profil. */
@Composable
fun AccountScreen(vm: AuthViewModel = viewModel()) {
    val user = vm.state.user ?: return
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Hello, ${user.fullName}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Email: ${user.email}")
        Text("Phone: ${user.phone ?: "—"}")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
    }
}