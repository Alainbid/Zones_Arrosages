package com.alain.zonesarrosage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.alain.zonesarrosage.R.color.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


var messageBT = ""
private var ajustement = "000"

// limites des saisies avant envoi à l'ESP32
private const val AJUSTEMENT_MIN = 1     // ajustement saisonnier en %
private const val AJUSTEMENT_MAX = 300
private const val MANUEL_MIN = 1         // durée de l'arrosage manuel en mn
private const val MANUEL_MAX = 60

class MainActivity : AppCompatActivity() {

    private var deviceName = "ESP32 Bluetooth"
    private lateinit var resultat1: TextView
    private lateinit var resultat2: TextView
    private lateinit var resultat3: TextView
    private lateinit var resultat4: TextView
    private val tableNomZones = arrayOf("Potager", "Parking", "Portillon", "Grand Tour")
    private lateinit var bluetoothManager: BluetoothManager


    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions are granted
            bluetoothManager.connectToDevice(deviceName)
            //bluetoothManager.connectToDevice("HC-05")
        } else {
            // Permissions not granted
            Toast.makeText(this, "Authorisations Bluetooth sont requises", Toast.LENGTH_LONG).show()
            val txt = findViewById<TextView>(R.id.textView)
            "Vérifiez les autorisations Bluetooth".also { txt.text = it }
            val pgb = findViewById<ProgressBar>(R.id.progressBar)
            pgb.visibility = View.GONE
            val btn = findViewById<Button>(R.id.button)
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                finishAndRemoveTask()
            }
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // bluetoothManager.connectToDevice("HC-05" ou "ESP32 Bluetooth")
            bluetoothManager.connectToDevice(deviceName)
            //Toast.makeText(this, "Connexion établie", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth non activé", Toast.LENGTH_LONG).show()
            val txt = findViewById<TextView>(R.id.textView)
            "Le Bluetooth n'est pas activé".also { txt.text = it }
            val pgb = findViewById<ProgressBar>(R.id.progressBar)
            pgb.visibility = View.GONE
            val btn = findViewById<Button>(R.id.button)
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//onBluetoothConnected()// activer la methode si pas  de BT
        // Show the loading screen

        setContentView(R.layout.activity_loading)
        supportActionBar?.title = "Attente Connexion ..."
        supportActionBar?.setBackgroundDrawable(
            ResourcesCompat.getDrawable(resources, teal_700, null)
        )

        bluetoothManager = BluetoothManager(this, enableBluetoothLauncher)

        // Request permissions if needed
        requestBluetoothPermissions.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        )

    }


    fun onBluetoothConnected() {
        // Switch to the main activity layout
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Initialize views and listeners
        initializeViews()
    }


    fun onBluetoothConnectionFailed(message: String) {
        // Set the layout to activity_connection_failed
        setContentView(R.layout.activity_connection_failed)

        // Initialize buttons and set their click listeners
        val retryhc05: Button = findViewById(R.id.btn_hc05)
        val retryesp32: Button = findViewById(R.id.btn_esp32)

        retryhc05.setOnClickListener {
            deviceName = "HC-05"
            retryConnection()
        }

        retryesp32.setOnClickListener {
            deviceName = "ESP32 Bluetooth"
            retryConnection()
        }

        // Show a toast message with the error
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Method to handle retry connection logic
    private fun retryConnection() {
        // Set the layout to activity_loading
        setContentView(R.layout.activity_loading)

        // fermer l'ancien socket avant de se reconnecter
        bluetoothManager.closeConnexion()
        // Attempt to reconnect to the device
        bluetoothManager.connectToDevice(deviceName)
    }


    @SuppressLint("ResourceAsColor")
    private fun initializeViews() {
        resultat1 = findViewById(R.id.resultat1)
        resultat2 = findViewById(R.id.resultat2)
        resultat3 = findViewById(R.id.resultat3)
        resultat4 = findViewById(R.id.resultat4)

        resultat1.text = buildString {
            append("Programmer le ")
            append(tableNomZones[0])
        }
        resultat2.text = buildString {
            append("Programmer le ")
            append(tableNomZones[1])
        }
        resultat3.text = buildString {
            append("Programmer le ")
            append(tableNomZones[2])
        }
        resultat4.text = buildString {
            append("Programmer le ")
            append(tableNomZones[3])
        }

        supportActionBar?.title = "Connecté ..."
        supportActionBar?.setBackgroundDrawable(
            ResourcesCompat.getDrawable(resources, teal_700, null)
        )

        // L'ancienne programmation de la zone n'est effacée qu'au retour
        // validé de ProgrammingActivity (voir programmingActivityResultLauncher)
        findViewById<View>(R.id.zone1).setOnClickListener { openProgrammingActivity(1) }
        findViewById<View>(R.id.zone2).setOnClickListener { openProgrammingActivity(2) }
        findViewById<View>(R.id.zone3).setOnClickListener { openProgrammingActivity(3) }
        findViewById<View>(R.id.zone4).setOnClickListener { openProgrammingActivity(4) }
        "".also { resultat1.text = it }
        "".also { resultat2.text = it }
        "".also { resultat3.text = it }
        "".also { resultat4.text = it }

        // la lettre 'a' demande la lecture de l'EEPROM de l'ESP32
        // La réponse est attendue en tâche de fond : l'écran reste utilisable
        lifecycleScope.launch {
            bluetoothManager.viderReception()
            if (!bluetoothManager.sendMessage("a")) {
                Toast.makeText(this@MainActivity, "Impossible d'interroger l'ESP32", Toast.LENGTH_LONG).show()
                return@launch
            }
            val x = bluetoothManager.receiveMessage()
            // la connexion a pu être perdue pendant l'attente (autre écran affiché)
            if (findViewById<TextView>(R.id.heure) == null) return@launch
            if (x.isEmpty()) {
                Toast.makeText(this@MainActivity, "Aucune réponse de l'ESP32", Toast.LENGTH_LONG).show()
                return@launch
            }
            messageBT = x
            Log.d("-------  xxx --", x)
            //affichage du résultat
            processResponse(x)
        }
    }//fin initialize

    override fun onDestroy() {
        // fermer le socket et arrêter la lecture quand l'application se ferme
        if (::bluetoothManager.isInitialized) bluetoothManager.closeConnexion()
        super.onDestroy()
    }

    /** Envoie [message] et affiche une erreur si l'envoi échoue. */
    private suspend fun envoyer(message: String): Boolean {
        val ok = bluetoothManager.sendMessage(message)
        if (!ok) Toast.makeText(this, "Echec de l'envoi à l'ESP32", Toast.LENGTH_LONG).show()
        return ok
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {

            R.id.adjust -> {
// méthode pour régler la valeur d'ajustement saisonnier
                // "%" comme caractère initial
                // Use a coroutine to wait for the dialog response
                lifecycleScope.launch {
                    val deb = showInputDialog(this@MainActivity).trim()
                    Log.d("---------deb --", deb)
                    val valeur = deb.toIntOrNull()
                    if (deb.isEmpty() || valeur == 0) {
                        Toast.makeText(this@MainActivity, "Annulation", Toast.LENGTH_SHORT).show()
                    } else if (valeur == null || valeur !in AJUSTEMENT_MIN..AJUSTEMENT_MAX) {
                        // rien n'est envoyé à l'ESP32
                        Toast.makeText(
                            this@MainActivity,
                            "ANNULATION... L'ajustement doit être entre $AJUSTEMENT_MIN et $AJUSTEMENT_MAX %",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val adjustedValue = "%$deb"
                        Log.d("---------deb --", adjustedValue)
                        // on ne ferme l'application que si l'envoi a réussi
                        if (envoyer(adjustedValue)) finishAndRemoveTask()
                    }
                }
                true
            }

            R.id.manuel -> {
                lifecycleScope.launch {
                    val (quelleZone, saisie) = showSelectDialog(this@MainActivity)
                    val duree = saisie.trim()
                    val minutes = duree.toIntOrNull()

                    if (quelleZone.isEmpty() || duree.isEmpty()) {
                        Toast.makeText(this@MainActivity, "ANNULATION...Choisir une zone et une durée", Toast.LENGTH_LONG).show()
                    } else if (minutes == null || minutes !in MANUEL_MIN..MANUEL_MAX) {
                        // rien n'est envoyé à l'ESP32
                        Toast.makeText(
                            this@MainActivity,
                            "ANNULATION... La durée doit être entre $MANUEL_MIN et $MANUEL_MAX mn",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val adjustedValue = "m$quelleZone$duree"
                        Log.d("--------- quelle Zone --", adjustedValue)

                        // Envoyer la commande via Bluetooth
                        if (envoyer(adjustedValue)) {
                            Toast.makeText(this@MainActivity, "Arrosage manuel lancé", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                return true
            }

            R.id.trash -> {
                // méthode pour mettre 0 an EEPROM
                // "z" comme caractère initial
                //raz des zones
                // action irréversible : on demande confirmation
                Builder(this)
                    .setTitle("Effacer les programmations")
                    .setMessage("Effacer toutes les programmations des 4 zones dans l'ESP32 ?")
                    .setPositiveButton("Effacer") { _, _ ->
                        lifecycleScope.launch {
                            Log.d("---------trash-", "trash = lettre z")
                            if (envoyer("z")) {
                                "".also { resultat1.text = it }
                                "".also { resultat2.text = it }
                                "".also { resultat3.text = it }
                                "".also { resultat4.text = it }
                                finishAndRemoveTask()
                            }
                        }
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
                return true
            }

            android.R.id.home -> {
                bluetoothManager.closeConnexion()
                // Fermer application lorsque la flèche de retour est cliquée
                finishAndRemoveTask()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    fun onBluetoothDisconnected() {
        Toast.makeText(this, "Connexion perdue", Toast.LENGTH_SHORT).show()
        // Optionally, navigate back to a specific activity or show a UI element to inform the user
        // affiche l'écran avec les boutons de reconnexion HC-05 / ESP32
        onBluetoothConnectionFailed("Connexion perdue")
    }


    private fun openProgrammingActivity(zone: Int) {
        val intent = Intent(this, ProgrammingActivity::class.java)
        intent.putExtra("ZONE_ID", zone)
        programmingActivityResultLauncher.launch(intent)
    }


    private fun effacerValeurEnMemoire(zone: Int) {
        //si on sélectionne une zone à reprogrammer
        // l'ancienne programmation doit être effacée
        var messageAvant = messageBT
        var result: String
        val endIndex = messageAvant.indexOf("$")

        // supprimer  heure debut en millis
        result = messageAvant.substring(endIndex + 1)

        // ajouter "&"  au cas où le bloc cherché est en premier
        var nouveau = "&"
        nouveau += result

        // Définir l'expression régulière pour trouver les blocs à supprimer
        val regex = "&${zone}/.*?/&".toRegex()

        //parcours de toute la commande
        while (regex.containsMatchIn(nouveau)) {
            // Remplacer les blocs correspondants par une chaîne vide
            result = nouveau.replace(regex, "&")
            nouveau = result
        }
        //on retire le "&" rajouté au début
        messageAvant = nouveau.substring(1)
        messageBT = messageAvant
        Log.d("-------messageBT++", messageBT)
    }


    private val programmingActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val zone = data?.getStringExtra("ZONE")
            var message: String? = data?.getStringExtra("MESSAGE")
            val enclair: String? = data?.getStringExtra("ENCLAIR")
            //mettre à jour le compteur dans ESP32
            Log.d("------", "nouveau programme reçu ----  $message")
            Log.d("------", " messageBT inicial EEPROM --- $messageBT")
            // la reprogrammation est validée : on efface maintenant l'ancienne
            // programmation de cette zone et on floute son image
            zone?.toIntOrNull()?.let { numZone ->
                effacerValeurEnMemoire(numZone)
                when (numZone) {
                    1 -> findViewById<ImageView>(R.id.imageView1).setImageResource(R.drawable.potagerfiltre)
                    2 -> findViewById<ImageView>(R.id.imageView2).setImageResource(R.drawable.parkingfiltre)
                    3 -> findViewById<ImageView>(R.id.imageView3).setImageResource(R.drawable.portillonfiltre)
                    4 -> findViewById<ImageView>(R.id.imageView4).setImageResource(R.drawable.grandtourfiltre)
                }
            }
            // trim() indispensable pour concaténer correctement les 2 strings
            message = messageBT.trim() + (message ?: "").trim()
            // messageBT  += message.toString() //ajout des autres  programmations
            Log.d("------messaBt  après TRIM() -", message)
            messageBT = message
            Routines.modifierLeFond(this, zone, resultat1, resultat2, resultat3, resultat4)

            when (zone) {
                "1" -> "${tableNomZones[0]} : $enclair".also { resultat1.text = it }
                "2" -> "${tableNomZones[1]} : $enclair".also { resultat2.text = it }
                "3" -> "${tableNomZones[2]} : $enclair".also { resultat3.text = it }
                "4" -> "${tableNomZones[3]} : $enclair".also { resultat4.text = it }
            }

            //mise à jour de l'heure
            val depuisMinuit = Routines.secondsSinceMidnight().toString() + "$"
            //mise à jour de l'heure dans ESP32
            if (messageBT.indexOf("$") == -1) {
                messageBT = depuisMinuit + messageBT
            }
            Log.d("---------------------", "  envoyé  >>> $messageBT")
            //    enregistrer le programme dans  HC-05
            val aEnvoyer = messageBT
            lifecycleScope.launch {
                if (envoyer(aEnvoyer)) {
                    Toast.makeText(this@MainActivity, "Programme enregistré", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun processResponse(response: String) {
        // une réponse incomplète ou mal formée ne doit pas faire planter l'application
        try {
            analyserReponse(response)
        } catch (e: RuntimeException) {
            // StringIndexOutOfBoundsException, NumberFormatException...
            Log.e("processResponse", "Réponse ESP32 illisible : $response", e)
            Toast.makeText(
                this,
                "Réponse de l'ESP32 incomplète, programmes non affichés",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun analyserReponse(response: String) {
        var debut: String
        var duree: String
        var txt = " "
        var sameRelay = ""
        var startIndex = 0
        val heureLue= findViewById<TextView>(R.id.heure)
        Log.d("--------- ", "réponse     $response")


 //récupération heure  le signe # sépare le  texte de l'heure

        var endIndex = response.indexOf("#")
        if (endIndex != -1) {
             heureLue.text= buildString {
                 append("ESP32 heure : ")
                 append(response.substring(startIndex, endIndex))
             }
            // start index sera donc endIndex + 1
             startIndex = endIndex + 1
        }

//récupération ajustement saisonnier
         endIndex = response.indexOf("%")
        if (endIndex != -1) {
            val ajusterValeur = response.substring(startIndex , endIndex)
            ajusterValeur.trim().toFloatOrNull()?.let {
                ajustement = (it * 100).toInt().toString()
            }
            messageBT = response.substring(endIndex + 1)
        }

        startIndex = response.indexOf("$")
        val finIndex = response.indexOf("&")

// sauter la valeur du compteur
        if (startIndex != -1 && finIndex != -1) {
            var resp = response.substring(startIndex + 1)
//examiner  les relais un à un
            while (resp.indexOf("&") != -1) {

                var stopIndex = resp.indexOf("/")
                val relay = resp.substring(0, stopIndex)

                if (relay != sameRelay) {
                    txt = " arrosage à "
                    sameRelay = relay
                }

                resp = resp.substring(relay.length + 1)
                stopIndex = resp.indexOf("/")
                debut = resp.substring(0, stopIndex)
                //calcul heure et minutes
                val heure = debut.toInt() / 3600
                val mn = (debut.toInt() % 3600) / 60
                var lesmn = ""
                if (mn > 9) {
                    lesmn = mn.toString()
                } else {
                    lesmn += "0$mn"
                }

                resp = resp.substring(debut.length + 1)
                stopIndex = resp.indexOf("/")
                //calcul durée
                duree = resp.substring(0, stopIndex)
                val duration = duree.toInt() / 60

                resp = resp.substring(duree.length + 1)
                stopIndex = resp.indexOf("/")
                // nombre de fois
                val fois: String = resp.substring(0, stopIndex)

                txt += heure.toString() + "h" + lesmn + " pour " + duration + "mn "

                when (relay) {
                    "1" -> {
                        if (fois != "0") resultat1.height = 200
                        resultat1.text = buildString {
                            append(tableNomZones[relay.toInt() - 1])
                            append(txt)
                        }
                    }

                    "2" -> {
                        if (fois != "0") resultat2.height = 200
                        resultat2.text = buildString {
                            append(tableNomZones[relay.toInt() - 1])
                            append(txt)
                        }
                    }

                    "3" -> {
                        if (fois != "0") resultat3.height = 200
                        resultat3.text = buildString {
                            append(tableNomZones[relay.toInt() - 1])
                            append(txt)
                        }
                    }

                    "4" -> {
                        if (fois != "0") resultat4.height = 200
                        resultat4.text = buildString {
                            append(tableNomZones[relay.toInt() - 1])
                            append(txt)
                        }
                    }

                }

                txt += "et à "
                Log.d("----------------", "510  relay $relay $txt")
                resp = resp.substring(fois.length + 2)
            }
        }
    }


    private suspend fun showInputDialog(context: Context): String =
        suspendCancellableCoroutine { continuation ->
            // Inflate the custom layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_layout, null)
            val input = view.findViewById<EditText>(R.id.editTextInput)

            val dialog = Builder(context)
                .setTitle("Exemple de valeur \npar ex : 25 pour 25%\n 200 pour 200%")
                .setMessage("Actuellement  programmé $ajustement%")
                .setView(view)
                .setPositiveButton("OK") { _, _ ->
                    continuation.resume(input.text.toString())
                }
                .setNegativeButton("Cancel") { _, _ ->
                    continuation.resume("")
                }
                .create()

            dialog.setOnCancelListener {
                continuation.resume("")
            }
            dialog.show()
        }


    private suspend fun showSelectDialog(context: Context): Pair<String, String> =
        suspendCancellableCoroutine { continuation ->
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.select_layout, null)

            val rdgroupe: RadioGroup = view.findViewById(R.id.rdgroup)
            val mnForcees: EditText = view.findViewById(R.id.mn_forcées) // Trouver le EditText ici

            var selectedOption = ""
            rdgroupe.setOnCheckedChangeListener { _, checkedId ->
                selectedOption = when (checkedId) {
                    R.id.rd1 -> "0"
                    R.id.rd2 -> "1"
                    R.id.rd3 -> "2"
                    R.id.rd4 -> "3"
                    else -> ""
                }
            }

            val dialog = Builder(context)
                .setTitle("Tester quelle zone ?")
                .setMessage("choisir la zone et la durée ")
                .setView(view)
                .setPositiveButton("VALIDER") { _, _ ->
                    // Récupérer la valeur des minutes ici
                    val duree = mnForcees.text.toString()
                    continuation.resume(Pair(selectedOption, duree))
                }
                .setNegativeButton("Annuler") { _, _ ->
                    continuation.resume(Pair("", ""))
                }
                .create()

            mnForcees.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Cacher le clavier
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    
                    // Donner le focus au bouton VALIDER du dialogue
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).requestFocus()
                    true
                } else {
                    false
                }
            }

            dialog.setOnCancelListener {
                continuation.resume(Pair("", ""))
            }
            dialog.show()
        }

}
