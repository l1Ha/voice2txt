package com.voice2txt.app

import android.app.Application
import com.voice2txt.app.data.db.AppDatabase
import com.voice2txt.app.data.repository.TranscriptionRepository
import java.io.File

class Voice2TxtApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TranscriptionRepository(database.transcriptionDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ensureDirectories()
    }

    private fun ensureDirectories() {
        // Directory for extracted audio cache
        val audioDir = File(cacheDir, "extracted_audio")
        if (!audioDir.exists()) audioDir.mkdirs()

        // Directory for offline speech recognition models
        val modelsDir = File(filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        // Directory for exported transcripts
        val exportsDir = File(getExternalFilesDir(null), "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
    }

    companion object {
        lateinit var instance: Voice2TxtApplication
            private set
    }
}
