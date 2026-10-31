package com.doron.gymtracker.ui.screens.account

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.account.Account
import com.doron.gymtracker.account.BackendException
import com.doron.gymtracker.account.SignInCancelled
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.doron.gymtracker.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val account: Account,
    @ApplicationContext private val context: Context,
) : ViewModel() {

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
                SyncWorker.syncSoon(context)
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
