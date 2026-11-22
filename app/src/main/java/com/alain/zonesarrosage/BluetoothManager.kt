package com.alain.zonesarrosage

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean


class BluetoothManager(private val mainActivity: MainActivity, private val enableBluetoothLauncher: ActivityResultLauncher<Intent>) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // Toutes les tâches Bluetooth tournent dans ce scope, annulé à la fermeture
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readerJob: Job? = null

    // Données reçues de l'ESP32, remplies en continu par le lecteur
    private val receiveBuffer = ByteArrayOutputStream()
    private val bufferLock = Any()

    // Évite de signaler plusieurs fois la même perte de connexion
    // (lecteur et envoi peuvent échouer en même temps)
    private val disconnectNotified = AtomicBoolean(false)

    // Vrai quand c'est l'application elle-même qui ferme le socket :
    // dans ce cas, l'erreur de lecture qui en résulte n'est pas une perte de connexion
    @Volatile
    private var closing = false

    fun connectToDevice(deviceName: String) {
        val bluetoothManager = mainActivity.getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            showToast("Bluetooth non disponible")
            return
        }
        // bluetooth activé ?
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            connectToPairedDevice(bluetoothAdapter, deviceName)
        }
    }

    // Connexion au HC-05 ou à l'ESP32
    private fun connectToPairedDevice(bluetoothAdapter: BluetoothAdapter, deviceName: String) {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            mainActivity.onBluetoothConnectionFailed("Autorisation Bluetooth refusée")
            return
        }
        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices.find { it.name == deviceName }
        if (device == null) {
            Log.d("BluetoothManager", "$deviceName introuvable")
            mainActivity.onBluetoothConnectionFailed("$deviceName introuvable")
            return
        }

        // device.uuids peut être null : on se rabat sur l'UUID SPP standard
        // (port série utilisé par le HC-05 et par BluetoothSerial de l'ESP32)
        val uuid: UUID = device.uuids?.firstOrNull()?.uuid ?: SPP_UUID

        // nouvelle connexion : on repart d'un état propre
        closing = false
        disconnectNotified.set(false)
        if (!scope.isActive) scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket = socket
                socket.connect()
                inputStream = socket.inputStream
                outputStream = socket.outputStream
                startReader()
                withContext(Dispatchers.Main) {
                    if (activiteDisponible()) mainActivity.onBluetoothConnected()
                }
            } catch (e: Exception) {
                // IOException (appareil éteint, hors de portée...) ou SecurityException
                Log.d("BluetoothManager", "Echec connexion : ${e.message}")
                if (closing) return@launch
                withContext(Dispatchers.Main) {
                    if (activiteDisponible()) mainActivity.onBluetoothConnectionFailed("Echec connexion ${e.message ?: ""}")
                }
            }
        }
    }

    /**
     * Lit en permanence ce qu'envoie l'ESP32.
     * Une lecture bloquée qui se termine en erreur (ou par -1) est le seul signe
     * fiable d'une connexion perdue : BluetoothSocket.isConnected reste vrai
     * même quand l'ESP32 est éteint.
     */
    private fun startReader() {
        val input = inputStream ?: return
        readerJob = scope.launch {
            val buffer = ByteArray(1024)
            try {
                while (isActive) {
                    val n = input.read(buffer)
                    if (n < 0) throw IOException("Flux fermé par l'ESP32")
                    if (n > 0) synchronized(bufferLock) { receiveBuffer.write(buffer, 0, n) }
                }
            } catch (e: IOException) {
                Log.d("BluetoothManager", "Lecture interrompue : ${e.message}")
                signalerPerteConnexion()
            }
        }
    }

    private fun signalerPerteConnexion() {
        if (closing || !disconnectNotified.compareAndSet(false, true)) return
        mainActivity.runOnUiThread {
            if (activiteDisponible()) mainActivity.onBluetoothDisconnected()
        }
    }

    private fun activiteDisponible() = !mainActivity.isFinishing && !mainActivity.isDestroyed

    //***************************************************************************************

    /** Vide les données reçues et pas encore lues (à appeler avant d'envoyer une demande). */
    fun viderReception() {
        synchronized(bufferLock) { receiveBuffer.reset() }
    }

    /**
     * Envoie [message] à l'ESP32.
     * Renvoie false si l'appareil n'est pas connecté ou si l'envoi a échoué ;
     * dans ce dernier cas la perte de connexion est signalée.
     */
    suspend fun sendMessage(message: String): Boolean {
        val out = outputStream ?: return false
        Log.d("BluetoothManager", "Envoi : $message")
        return withContext(Dispatchers.IO) {
            try {
                out.write(message.toByteArray())
                out.flush()
                true
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Echec de l'envoi", e)
                signalerPerteConnexion()
                false
            }
        }
    }

    //***************************************************************************************

    /**
     * Attend la réponse de l'ESP32 sans bloquer l'écran.
     * La réponse peut arriver en plusieurs morceaux : on attend la première donnée
     * (au plus [delaiMaxMs]), puis on considère la réponse complète quand plus rien
     * n'arrive pendant [silenceMs].
     * Renvoie une chaîne vide si rien n'est reçu.
     */
    suspend fun receiveMessage(delaiMaxMs: Long = 5000, silenceMs: Long = 500): String {
        val debut = System.currentTimeMillis()
        var tailleConnue = 0
        var dernierAjout = debut

        while (true) {
            delay(50)
            val maintenant = System.currentTimeMillis()
            val taille = synchronized(bufferLock) { receiveBuffer.size() }

            if (taille != tailleConnue) {
                tailleConnue = taille
                dernierAjout = maintenant
            }
            if (tailleConnue > 0 && maintenant - dernierAjout >= silenceMs) break
            if (maintenant - debut >= delaiMaxMs) break
        }

        val response = synchronized(bufferLock) {
            val texte = receiveBuffer.toString()
            receiveBuffer.reset()
            texte
        }
        Log.d("BluetoothData", "Reçu : $response")
        return response
    }

    fun closeConnexion() {
        closing = true
        readerJob?.cancel()
        readerJob = null
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Erreur à la fermeture du socket", e)
        }
        scope.cancel()
        bluetoothSocket = null
        inputStream = null
        outputStream = null
        viderReception()
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }


    private fun showToast(message: String) {
        mainActivity.runOnUiThread {
            Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}
