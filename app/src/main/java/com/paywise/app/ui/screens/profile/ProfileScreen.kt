package com.paywise.app.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.UserProfile
import com.paywise.app.firebase.FirebaseService
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val userId = MutableStateFlow("")
    val user = userId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(null) else repository.getUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var name by mutableStateOf("")
    var salary by mutableStateOf("")
    var salaryDay by mutableStateOf("1")
    var currency by mutableStateOf("SAR")
    var country by mutableStateOf("")
    var isSaving by mutableStateOf(false)

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) userId.value = uid
            }
        }
    }

    fun loadUser() {
        user.value?.let { u ->
            name = u.name
            salary = if (u.salaryAmount > 0) u.salaryAmount.toString() else ""
            salaryDay = u.salaryDay.toString()
            currency = u.currency
            country = u.country
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            isSaving = true
            val uid = userId.first() ?: return@launch
            val updatedUser = user.value?.copy(
                name = name,
                salaryAmount = salary.toDoubleOrNull() ?: 0.0,
                salaryDay = salaryDay.toIntOrNull() ?: 1,
                currency = currency,
                country = country
            ) ?: return@launch

            repository.updateUser(updatedUser)
            firebaseService.syncUserProfile(updatedUser)
            preferencesManager.setCurrency(currency)
            isSaving = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()

    LaunchedEffect(user) {
        viewModel.loadUser()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Profile", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = viewModel.name.take(2).uppercase().ifEmpty { "U" },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personal Info
            Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))

            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.salary,
                        onValueChange = { viewModel.salary = it },
                        label = { Text("Monthly Salary") },
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.salaryDay,
                        onValueChange = { viewModel.salaryDay = it },
                        label = { Text("Salary Day of Month") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.currency,
                        onValueChange = { viewModel.currency = it },
                        label = { Text("Currency") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.country,
                        onValueChange = { viewModel.country = it },
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !viewModel.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Profile", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
