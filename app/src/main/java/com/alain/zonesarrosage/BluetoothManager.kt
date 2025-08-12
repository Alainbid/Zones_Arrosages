package com.alain.zonesarrosage

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID




class BluetoothManager(private val mainActivity: MainActivity, private val enableBluetoothLauncher: ActivityResultLauncher<Intent>) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var connectionLostHandler: Handler? = null
    private var connectionLostRunnable: Runnable? = null

    fun connectToDevice(deviceName: String) {
        val bluetoothManager = mainActivity.getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            showToast("Bluetooth non disponible")
            return
        }
//bluetooth  activé ?
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
         //   mainActivity.onBluetoothConnected() // activé  si pas de bluetooth
            connectToPairedDevice(bluetoothAdapter, deviceName)
        }
    }
////connexion to HC-05
//    private fun connectToPairedDevice(bluetoothAdapter: BluetoothAdapter, deviceName: String) {
//        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
//        val device: BluetoothDevice? = pairedDevices.find { it.name == deviceName }
//
//        if (device != null) {
//            val uuid: UUID = device.uuids[0].uuid
//            try {
//                if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return
//                }
//                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
//
//                val connectThread = Thread {
//                    try {
//                        bluetoothSocket?.connect()
//                        if (bluetoothSocket?.isConnected == true) {
//                            inputStream = bluetoothSocket?.inputStream
//                            outputStream = bluetoothSocket?.outputStream
//                            mainActivity.runOnUiThread {
//                                mainActivity.onBluetoothConnected()
//                            }
//                            startConnectionLostWatcher()
//                        } else {
//                            mainActivity.runOnUiThread {
//                                mainActivity.onBluetoothConnectionFailed("Echec de la connexion")
//                            }
//                        }
//                    } catch (e: IOException) {
//                        mainActivity.runOnUiThread {
//                            mainActivity.onBluetoothConnectionFailed("Echec de la connexion ${e.message}")
//                        }
//                    }
//                }
//                connectThread.start()
//
//                // Timeout for connection attempt
//                connectThread.join(5000) // Wait for 6 seconds
//                if (bluetoothSocket?.isConnected != true) {
//                    connectThread.interrupt()
//
//                    mainActivity.runOnUiThread {
//                        mainActivity.onBluetoothConnectionFailed("Temps de connexion trop long")
//                    }
//                }
//            } catch (e: InterruptedException) {
//                showToast("Interruption de la connexion")
//            }
//        } else {
//            showToast("$deviceName introuvable")
//        }
//    }


    private fun startConnectionLostWatcher() {
        connectionLostHandler = Handler(Looper.getMainLooper())
        connectionLostRunnable = Runnable {
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                showToast("Connexion perdue")
                mainActivity.runOnUiThread {
                    mainActivity.onBluetoothDisconnected()
                }
            } else {
                connectionLostHandler?.postDelayed(connectionLostRunnable!!, 6000)
            }
        }
        connectionLostHandler?.postDelayed(connectionLostRunnable!!, 6000)
    }

    // Connection to HC-05
    private fun connectToPairedDevice(bluetoothAdapter: BluetoothAdapter, deviceName: String) {
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val device: BluetoothDevice? = pairedDevices.find { it.name == deviceName }

        if (device != null) {
            val uuid: UUID = device.uuids[0].uuid
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        mainActivity.runOnUiThread {
                            mainActivity.onBluetoothConnectionFailed("Bluetooth permission denied")
                        }
                        return@launch
                    }
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                    try {
                        bluetoothSocket?.connect()
                        withContext(Dispatchers.Main) {
                            if (bluetoothSocket?.isConnected == true) {
                                inputStream = bluetoothSocket?.inputStream
                                outputStream = bluetoothSocket?.outputStream
                                mainActivity.onBluetoothConnected()
                                startConnectionLostWatcher()
                            } else {
                                Log.d("--------- Error 0", "Echec")
                                mainActivity.onBluetoothConnectionFailed("Echec de la connexion")
                            }
                        }
                    } catch (e: IOException) {
                        Log.d("--------- Error 1", "${e.message}")
                        withContext(Dispatchers.Main) {
                            mainActivity.onBluetoothConnectionFailed("Echec connexion ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("--------- Error 2", "${e.message}")
                    withContext(Dispatchers.Main) {
                        mainActivity.onBluetoothConnectionFailed("Erreur de connexion ${e.message}")
                    }
                }
            }
        } else {
            Log.d("--------- Error 3", "introuvable")
            mainActivity.runOnUiThread {
                mainActivity.onBluetoothConnectionFailed("$deviceName introuvable")
            }
        }
    }

//***************************************************************************************
fun sendMessage(message: String): Boolean {
    if (outputStream != null) {
        Log.d("-------++++++----------", message)
            try {
             //   outputStream?.write( "34570$1/34680/300/0/&1/34680/300/1/&2/36680/180/0/&2/36680/240/1/&2/36680/300/3/&34570$1/34680/300/0/&1/34680/300/1/&2/36680/180/0/&2/36680/240/1/&2/36680/300/3/&34570$1/34680/300/0/&1/34680/300/1/&2/36680/180/0/&2/36680/240/1/&2/36680/300/3/&\n".toByteArray())
              outputStream?.write(message.toByteArray())
               //  showToast("Message sent: $message")
            } catch (e: IOException) {
               // showToast("Failed to send message")
            }
        return true
        } else {
            //showToast("Not connected to any device")
            return false
        }
    }

//***************************************************************************************
    fun receiveMessage(): String? {
        val buffer = ByteArray(1024)
        val byteArrayOutputStream = ByteArrayOutputStream()

        return try {
            var bytesRead: Int
            var totalBytesRead = 0

            // Wait briefly for data to be available
            Thread.sleep(100)

            // Loop to continuously read data
            while (true) {
                if ((inputStream?.available() ?: 0) > 0) {
                    bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                } else {
                    // Break the loop if no data is available
                    break
                }

                // Check for a reasonable amount of data read
                if (totalBytesRead >= 1024) {
                    break
                }

                // Small delay to avoid excessive CPU usage
                Thread.sleep(100)
            }

            val response = byteArrayOutputStream.toString()
            Log.d("BluetoothData", "Received message: $response")
            response
        } catch (e: IOException) {
            Log.e("com.alain.zonesarrosage.BluetoothManager", "Failed to receive message", e)
            null
        } catch (e: InterruptedException) {
            Log.e("com.alain.zonesarrosage.BluetoothManager", "Thread sleep interrupted", e)
            null
        } finally {
            try {
                byteArrayOutputStream.close()
            } catch (e: IOException) {
                Log.e("com.alain.zonesarrosage.BluetoothManager", "Failed to close ByteArrayOutputStream", e)
            }
        }
    }

  fun  closeConnexion(){
      bluetoothSocket?.close()
    }


    private fun showToast(message: String) {
        mainActivity.runOnUiThread {
            Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}
