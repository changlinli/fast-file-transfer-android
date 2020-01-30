package com.example.anapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.changlinli.raptorq.*
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

    private fun <T> Iterator<T>.tap(f: (T) -> Unit): Iterator<T> {
        val originalIterator = this

        return object : Iterator<T> {
            override fun hasNext(): Boolean = originalIterator.hasNext()

            override fun next(): T {
                val result = originalIterator.next()
                f(result)
                return result
            }

        }
    }

    private fun <T> Iterator<T>.filter(f : (T) -> Boolean): Iterator<T> {
        val originalIterator = this

        return object : Iterator<T> {
            private var nextElement: T? = null
            // We need this boolean check as well because our Iterator might
            // have nulls in it itself, which means nextElement being null
            // is ambiguous as to whether our iterator actually stopped
            private var stoppedBecauseFilterReturnedTrue = false

            override fun hasNext(): Boolean {
                var shouldStop = false

                while(!shouldStop) {
                    if (!originalIterator.hasNext()) {
                        shouldStop = true
                    } else {
                        val nextIteratedElement = originalIterator.next()
                        nextElement = nextIteratedElement
                        shouldStop = !f(nextIteratedElement)
                        stoppedBecauseFilterReturnedTrue = shouldStop
                    }
                }

                return nextElement == null && stoppedBecauseFilterReturnedTrue
            }

            override fun next(): T = if (hasNext()) {
                // We know that if hasNext() returns true, nextElement must be
                // non-null, because hasNext puts elements there
                val result = nextElement!!
                nextElement = null
                stoppedBecauseFilterReturnedTrue = false
                result
            } else {
                // TODO: Fix message
                throw Exception("This iterator has exhausted all its elements!")
            }

        }
    }

    private fun <T> Iterator<T>.takeWhile(f : (T) -> Boolean): Iterator<T> {
        val originalIterator = this

        // Very similar to filter, we just don't reset our variables after calling next
        return object : Iterator<T> {

            private var nextElement: T? = null
            // In case our iterator itself has nulls we need to record whether
            // it was written to since nextElement being null is ambiguous
            private var nextElementWritten = false

            override fun hasNext(): Boolean =
                if (originalIterator.hasNext()) {
                    nextElement = originalIterator.next()
                    nextElementWritten = true
                    f(originalIterator.next())
                } else {
                    false
                }

            override fun next(): T = if (hasNext()) {
                // We know that if hasNext() returns true, nextElement must be
                // non-null, because hasNext puts elements there
                nextElement!!
            } else {
                // TODO: Fix message
                throw Exception("This iterator has exhausted all its elements!")
            }

        }
    }

    private fun <T, S> Iterator<T>.mapFilterNull(f : (T) -> S?): Iterator<S> {
        val originalIterator = this

        return originalIterator
            .map(f)
            .filter{ it != null }
            .map{ it!! }
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


    fun feedSinglePacket(packet: EncodingPacket, fecParameters: FECParameters, decoder: DataDecoder): Boolean {
        decoder.sourceBlock(packet.sourceBlockNumber()).putEncodingPacket(packet)
        return decoder.isDataDecoded
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val contentResolver = applicationContext.contentResolver

        val defaultFECParameters: FECParameters =
            FECParameters.newParameters(36510210, 10000, 1)

        downloadThreadPool.execute{
            val decoder: ArrayDataDecoder =
                OpenRQ.newDecoder(defaultFECParameters, 5)
            val fecParameters: FECParameters = defaultFECParameters
            val inetSocketAddress = InetSocketAddress(InetAddress.getByName("10.0.2.2"), 8012)

            val socket = DatagramSocket(ClientPort)
            val fileDownloadRequest = FileDownloadRequest.createFileRequest(inetSocketAddress, UUID(0, 0))
            val downloadIterator = mutableBlockingPacketIterator(socket)
                .map(::udpPacketFromDatagramPacket)
                .tap{ Log.i(logTag, "Got this packet: $it") }
                .map(ServerResponse.Companion::decode)
                .mapFilterNull{ it?.let{ it as? FileFragment }?.toEncodingPacketWithDecoder(decoder) }
                .map{ feedSinglePacket(it, fecParameters, decoder) }
                .takeWhile { finishedDecoding -> !finishedDecoding }

            socket.send(fileDownloadRequest.underlyingPacket.toDatagramPacket())
            Log.d(logTag, "Send filedownload request, which looked like $fileDownloadRequest")
            var i = 0
            downloadIterator.forEach{ _ ->
                Log.i(logTag, "Processed packet: $i")
                i += 1
            }
            val stopRequest = StopDownloadRequest.createStopDownloadRequest(inetSocketAddress, UUID(0, 0))
            socket.send(stopRequest.underlyingPacket.toDatagramPacket())
            Log.d(logTag, "We received this many bytes: ${decoder.dataArray().size}")

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

