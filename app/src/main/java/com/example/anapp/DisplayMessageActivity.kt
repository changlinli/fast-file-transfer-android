package com.example.anapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.changlinli.raptorq.*
import com.changlinli.raptorq.utils.*
import net.fec.openrq.ArrayDataDecoder
import net.fec.openrq.EncodingPacket
import net.fec.openrq.OpenRQ
import net.fec.openrq.decoder.DataDecoder
import net.fec.openrq.parameters.FECParameters
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.Executors

class DisplayMessageActivity : AppCompatActivity() {
    val logTag = this.javaClass.name

    val CREATE_FILE = 1

    val currentInstanceOfDisplayMessageActivity = this

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

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val downloadedBytes = msg.obj as ByteArray
            findViewById<TextView>(R.id.textView).text = "We got this many bytes: ${downloadedBytes.size}"
        }
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

        findViewById<TextView>(R.id.textView).apply{
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
            else -> {
                val socketAddress = InetSocketAddress(address, 8012)
                UdpPacket(
                    socketAddress,
                    ImmutableByteArray.unsafeFromArray(
                        datagramPacket.data,
                        datagramPacket.offset,
                        datagramPacket.length
                    )
                )
            }
        }
    }


    private val downloadThreadPool = Executors.newFixedThreadPool(1)

    private val ClientPort = 8011

    fun feedSinglePacketDebug(packet: EncodingPacket, fecParameters: FECParameters, decoder: DataDecoder, idx: Int): Boolean =
        if (idx == 3656) {
            feedSinglePacket(packet, fecParameters, decoder)
        } else {
            feedSinglePacket(packet, fecParameters, decoder)
        }

    fun feedSinglePacket(packet: EncodingPacket, fecParameters: FECParameters, decoder: DataDecoder): Boolean {
        if (decoder.isDataDecoded) {
            return true
        } else {
            decoder.sourceBlock(packet.sourceBlockNumber()).putEncodingPacket(packet)
            return decoder.isDataDecoded
        }
    }

    val fileInQuestion = UUID(0L, 1L)

    val mappingOfUUIDsToFileSizes = mapOf<UUID, Long>(
        UUID(0L, 0L) to 36510210,
        UUID(0L, 1L) to 210124
    )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val contentResolver = applicationContext.contentResolver

        val defaultFECParameters: FECParameters =
            FECParameters.newParameters(mappingOfUUIDsToFileSizes.get(fileInQuestion)!!, 10000, 1)

        downloadThreadPool.execute{
            val decoder: ArrayDataDecoder =
                OpenRQ.newDecoder(defaultFECParameters, 5)
            val fecParameters: FECParameters = defaultFECParameters
            val inetSocketAddress = InetSocketAddress(InetAddress.getByName("10.0.2.2"), 8012)

            val socket = DatagramSocket(ClientPort)
            val fileDownloadRequest = FileDownloadRequest.createFileRequest(inetSocketAddress, fileInQuestion)
            val downloadIterator = mutableBlockingPacketIterator(socket)
                .map(::udpPacketFromDatagramPacket)
                .tap{ Log.i(logTag, "Got this packet: $it") }
                .map(ServerResponse.Companion::decode)
                .tap{ Log.i(logTag, "Finished decoding") }
                .mapFilterNull{ it?.let{ it as? FileFragment }?.toEncodingPacketWithDecoder(decoder) }
                .tap{ Log.i(logTag, "Created encoding packet") }
                .zipWithIndex()
                .map{ feedSinglePacketDebug(it.first, fecParameters, decoder, it.second) }
                .tap{ Log.i(logTag, "Fed single packet") }
                .takeWhile { finishedDecoding -> !finishedDecoding }
                .tap{ Log.i(logTag, "Checked to see if we should end") }

            socket.send(fileDownloadRequest.underlyingPacket.toDatagramPacket())
            Log.d(logTag, "Send filedownload request, which looked like $fileDownloadRequest")
            var i = 0
            downloadIterator.forEach{ _ ->
                Log.i(logTag, "Processed packet: $i")
                i += 1
            }
            Log.d(logTag, "Finished processing download iterator")
            val stopRequest = StopDownloadRequest.createStopDownloadRequest(inetSocketAddress, fileInQuestion)
            Log.d(logTag, "About to send stop download request: $stopRequest")
            socket.send(stopRequest.underlyingPacket.toDatagramPacket())
            val decodedResult = decoder.dataArray()
            Log.d(logTag, "We received this many bytes: ${decodedResult.size}")
            val messgeToSendBackToUIThread = Message()
            messgeToSendBackToUIThread.obj = decodedResult
            currentInstanceOfDisplayMessageActivity.handler.sendMessage(messgeToSendBackToUIThread)


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

