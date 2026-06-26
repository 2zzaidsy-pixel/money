package com.paywise.app.ui.screens.auth

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.UserProfile
import com.paywise.app.firebase.FirebaseService
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseService: FirebaseService,
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var isLogin by mutableStateOf(true)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var gso: GoogleSignInOptions? = null
    var googleSignInClient: GoogleSignInClient? = null

    fun initGoogleSignIn(context: android.content.Context) {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.paywise.app.R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso!!)
    }

    fun toggleMode() {
        isLogin = !isLogin
        errorMessage = null
    }

    fun authenticate(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = if (isLogin) {
                    firebaseService.signInWithEmail(email.trim(), password)
                } else {
                    firebaseService.signUpWithEmail(email.trim(), password)
                }
                result.fold(
                    onSuccess = { firebaseUser ->
                        val userProfile = UserProfile(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: email,
                            name = firebaseUser.displayName ?: email.substringBefore("@"),
                            isGuest = firebaseUser.isAnonymous
                        )
                        repository.createUser(userProfile)
                        firebaseService.syncUserProfile(userProfile)
                        preferencesManager.setLoggedIn(firebaseUser.uid)
                        preferencesManager.setOnboardingDone()
                        onSuccess()
                    },
                    onFailure = { errorMessage = it.localizedMessage ?: "Authentication failed" }
                )
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    fun signInAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val result = firebaseService.createGuestUser()
                result.fold(
                    onSuccess = { firebaseUser ->
                        val guestProfile = UserProfile(
                            id = firebaseUser.uid,
                            name = "Guest",
                            isGuest = true
                        )
                        repository.createUser(guestProfile)
                        preferencesManager.setLoggedIn(firebaseUser.uid)
                        preferencesManager.setOnboardingDone()
                        onSuccess()
                    },
                    onFailure = { errorMessage = it.localizedMessage ?: "Guest sign in failed" }
                )
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken ?: throw Exception("Google sign in failed")
                val result = firebaseService.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { firebaseUser ->
                        val userProfile = UserProfile(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            name = firebaseUser.displayName ?: "User"
                        )
                        repository.createUser(userProfile)
                        firebaseService.syncUserProfile(userProfile)
                        preferencesManager.setLoggedIn(firebaseUser.uid)
                        preferencesManager.setOnboardingDone()
                        onSuccess()
                    },
                    onFailure = { errorMessage = it.localizedMessage ?: "Google sign in failed" }
                )
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initGoogleSignIn(context)
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data, onAuthSuccess)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo area
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = PrimaryGreen.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PayWise",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )

            Text(
                text = "Your AI Financial Advisor",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = viewModel.isLogin,
                    onClick = { if (!viewModel.isLogin) viewModel.toggleMode() },
                    label = { Text("Login") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryGreen
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = !viewModel.isLogin,
                    onClick = { if (viewModel.isLogin) viewModel.toggleMode() },
                    label = { Text("Sign Up") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            viewModel.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = AccentRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login/Sign Up button
            Button(
                onClick = { viewModel.authenticate(onAuthSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (viewModel.isLogin) "Login" else "Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "  or  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google login
            OutlinedButton(
                onClick = {
                    viewModel.googleSignInClient?.let { client ->
                        googleSignInLauncher.launch(client.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !viewModel.isLoading
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guest mode
            TextButton(
                onClick = { viewModel.signInAsGuest(onAuthSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            ) {
                Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Continue as Guest")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
