package com.bmcreations.bookinfo.ui.login

import android.app.Activity
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bmcreations.bookinfo.R
import com.bmcreations.bookinfo.ui.impl.ActivityNavigation
import com.bmcreations.bookinfo.viewmodel.LiveMessageEvent
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginViewModel private constructor(): ViewModel() {

    companion object {
        const val RC_SIGN_IN = 1337

        fun create(): LoginViewModel {
            return LoginViewModel()
        }
    }

    sealed class State {
        data class Authenticated(val user: FirebaseUser): State()
        object Unauthenticated: State()
        data class InvalidAuthentication(val cause: Throwable?): State()
    }

    // Choose authentication providers
    private val providers = arrayListOf(
        //AuthUI.IdpConfig.EmailBuilder().build(),
        //AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build()
        //AuthUI.IdpConfig.FacebookBuilder().build(),
        //AuthUI.IdpConfig.TwitterBuilder().build()
        )

    val authState = MutableLiveData<State>()

    val startActivityForResultEvent = LiveMessageEvent<ActivityNavigation>()

    init {
        refreshAuth()
    }

    fun refreshAuth() {
        authState.value = State.Unauthenticated
    }

    fun authenticate() {
        startActivityForResultEvent.sendEvent {
            this.startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.mipmap.ic_launcher_round)
                .setTheme(R.style.AppTheme) // Set theme
                .build(), RC_SIGN_IN)
        }
    }

    fun onResultFromActivity(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_SIGN_IN -> {
                val response = IdpResponse.fromResultIntent(data)

                val ret = if (resultCode == Activity.RESULT_OK) {
                    FirebaseAuth.getInstance().currentUser?.let {
                        State.Authenticated(it)
                    } ?: State.Unauthenticated

                } else {
                    response?.let {
                        State.InvalidAuthentication(it.error?.cause)
                    } ?: State.InvalidAuthentication(Throwable("User cancelled flow"))
                }

                authState.value = ret
            }
        }
    }
}