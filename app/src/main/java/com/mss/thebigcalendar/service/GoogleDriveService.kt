package com.mss.thebigcalendar.service

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File

class GoogleDriveService(
    private val context: Context,
    private val account: GoogleSignInAccount
) {

    private val drive: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("The Big Calendar")
            .build()
    }

    companion object {
        const val BACKUP_FOLDER = "appDataFolder"
    }

    fun uploadBackupFile(
        file: File,
        appProperties: Map<String, String>
    ): com.google.api.services.drive.model.File? {
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = file.name
            parents = listOf(BACKUP_FOLDER)
            this.appProperties = appProperties
        }
        val mediaContent = com.google.api.client.http.FileContent("application/json", file)

        return drive.files().create(fileMetadata, mediaContent).execute()
    }

    fun getBackupFiles(): List<com.google.api.services.drive.model.File> {
        return drive.files().list()
            .setSpaces(BACKUP_FOLDER)
            .setFields("files(id, name, createdTime, appProperties)")
            .execute()
            .files
    }

    fun downloadBackupFile(fileId: String, destination: File) {
        drive.files().get(fileId).executeMediaAndDownloadTo(destination.outputStream())
    }

    fun deleteBackupFile(fileId: String) {
        drive.files().delete(fileId).execute()
    }
}
