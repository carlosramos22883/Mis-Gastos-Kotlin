package com.carlosramos.misgastos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.repository.AuthRepository
import com.carlosramos.misgastos.data.repository.AuthResult
import com.carlosramos.misgastos.data.remote.dto.UserResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados de la UI de autenticación.
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserResponse) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /**
     * Verifica si hay un token guardado al abrir la app.
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            _isAuthenticated.value = repository.isAuthenticated()
        }
    }

    /**
     * Iniciar sesión
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(email, password)
            handleAuthResult(result)
        }
    }

    /**
     * Registrar usuario
     */
    fun register(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(name, email, password, passwordConfirmation)
            handleAuthResult(result)
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.logout()
            _isAuthenticated.value = false
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Maneja el resultado del repositorio y actualiza el estado de la UI.
     */
    private fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _isAuthenticated.value = true
                _uiState.value = AuthUiState.Success(result.user)
            }
            is AuthResult.Error -> {
                _uiState.value = AuthUiState.Error(result.message)
            }
            else -> {}
        }
    }

    /**
     * Limpia el estado de error (útil cuando el usuario vuelve a escribir).
     */
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Login con Google
     */
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.loginWithGoogle(idToken)
            handleAuthResult(result)
        }
    }
}