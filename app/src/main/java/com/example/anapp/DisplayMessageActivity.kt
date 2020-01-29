package com.example.anapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.changlinli.raptorq.ImmutableByteArray
import com.changlinli.raptorq.UdpPacket
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class DisplayMessageActivity : AppCompatActivity() {
    val logTag = this.javaClass.name

    val CREATE_FILE = 1

//    private fun createFile(pickerInitialUri: Uri) {
    private fun openFilePicker() {
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

        openFilePicker()
    }

    private val readBuffer = ByteArray(20000)

    private fun mutableBlockingPacketIterator(socket: DatagramSocket): Iterator<DatagramPacket> {
        val packet = DatagramPacket(readBuffer, readBuffer.size)
        return object : Iterator<DatagramPacket> {
            override fun hasNext(): Boolean = true

            override fun next(): DatagramPacket {
                socket.receive(packet)
                return packet
            }

        }
    }

    private fun <T, S> Iterator<T>.map(f : (T) -> S): Iterator<S> {
        val originalIterator = this

        return object : Iterator<S> {
            override fun hasNext(): Boolean = originalIterator.hasNext()

            override fun next(): S = f(originalIterator.next())

        }
    }

    private fun udpPacketFromDatagramPacket(datagramPacket: DatagramPacket): UdpPacket {
        return when (val address = datagramPacket.address) {
            is InetSocketAddress -> UdpPacket(
                address,
                ImmutableByteArray.unsafeFromArray(
                    datagramPacket.data,
                    datagramPacket.offset,
                    datagramPacket.length
                )
            )
            else -> TODO()
        }
    }

    private val downloadThreadPool = Executors.newFixedThreadPool(1)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val contentResolver = applicationContext.contentResolver

        downloadThreadPool.execute{

        }

        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also{uri ->
                val fileDescriptor = contentResolver.openFile(uri, "w", null)?.fileDescriptor
                val fileOutputStream = fileDescriptor?.run{ FileOutputStream(this)}
                val bytesToWrite = ByteArray(100)
                bytesToWrite[5] = 5
                fileOutputStream?.write(bytesToWrite)
                fileOutputStream?.flush()
                fileOutputStream?.close()

            }

        }
    }
}

