package com.example.anapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DisplayMessageActivity : AppCompatActivity() {
    val logTag = this.javaClass.name

    val CREATE_FILE = 1

//    private fun createFile(pickerInitialUri: Uri) {
    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "invoice.txt")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
    //            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_message)

        // Get the Intent that started this activity and extract the string
        val message = intent.getStringExtra(EXTRA_MESSAGE)

//        val downloadDirectory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//        val file = downloadDirectory?.run{ File(this, "awerawer") }
//        message?.apply{ file?.writeText(this) }
//        Log.i(logTag, file?.absolutePath ?: "null!")

        val textView = findViewById<TextView>(R.id.textView).apply{
            this.text = message
        }

        createFile()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val contentResolver = applicationContext.contentResolver

        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also{uri ->
                val fileDescriptor = contentResolver.openFile(uri, "w", null)?.fileDescriptor
                val fileOutputStream = fileDescriptor?.run{ FileOutputStream(this)}
                val bytesToWrite = ByteArray(100)
                bytesToWrite.set(5, 5)
                fileOutputStream?.write(bytesToWrite)
                fileOutputStream?.flush()
                fileOutputStream?.close()

            }

        }
    }
}
