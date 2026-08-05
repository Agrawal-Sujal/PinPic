package com.sunflower.pinpic.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode

    init {
        viewModelScope.launch {
            dataStoreManager.themeModeFlow.collect { mode ->
                _themeMode.value = mode ?: "DARK"
            }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.saveThemeMode(mode)
        }
    }
}
