package com.doron.gymtracker.ui.screens.account

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.account.Account
import com.doron.gymtracker.account.BackendException
import com.doron.gymtracker.account.SignInCancelled
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(private val account: Account) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Busy : State
        data object Done : State
        data class Failed(val notAllowed: Boolean) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun agreeAndSignIn(activity: Activity, routes: Boolean) {
        if (_state.value == State.Busy) return
        _state.value = State.Busy
        viewModelScope.launch {
            _state.value = try {
                account.signIn(activity, routes)
                State.Done
            } catch (e: SignInCancelled) {
                State.Idle
            } catch (e: BackendException.NotAllowed) {
                State.Failed(notAllowed = true)
            } catch (e: Exception) {
                State.Failed(notAllowed = false)
            }
        }
    }
}
