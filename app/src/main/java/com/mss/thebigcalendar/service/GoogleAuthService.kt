package com.mss.thebigcalendar.service

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.DriveScopes
import com.google.api.services.calendar.CalendarScopes

class GoogleAuthService(private val context: Context) {

    private val gso by lazy {
        val webClientId = "662891819317-lngjv15bpi5v5asttejhmc1rlaoatefd.apps.googleusercontent.com"
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(
                Scope(CalendarScopes.CALENDAR),
                Scope(CalendarScopes.CALENDAR_EVENTS),
                Scope(DriveScopes.DRIVE_APPDATA)
            )
            .build()
    }

    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>): GoogleSignInAccount? {
        return try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            android.util.Log.e("GoogleAuthService", "Sign-in failed with status code: ${e.statusCode}")
            android.util.Log.e("GoogleAuthService", "Error details: ${com.google.android.gms.common.api.CommonStatusCodes.getStatusCodeString(e.statusCode)}")
            null
        }
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener { 
            onComplete()
        }
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
}