package com.alain.zonesarrosage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout


class ProgrammingActivity : AppCompatActivity() {
    private lateinit var rd1fois: RadioButton
    private lateinit var rd2fois: RadioButton
    private lateinit var rd3fois: RadioButton
    private lateinit var layout2: ConstraintLayout
    private lateinit var layout3: ConstraintLayout
    private lateinit var timeUtils: TimeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programming)
        val table = arrayOf("Potager", "Parking", "Portillon", "Grand Tour")
        timeUtils = TimeUtils(this)//class for converting in seconds

        val zoneId = intent.getIntExtra("ZONE_ID", 0)

        rd1fois = findViewById(R.id.rd_1fois)
        rd2fois = findViewById(R.id.rd_2fois)
        rd3fois = findViewById(R.id.rd_3fois)
        layout2 = findViewById(R.id.layout2)
        layout3 = findViewById(R.id.layout3)
       val editTextDuration1: EditText = findViewById(R.id.duration1)
        val programmationZone = findViewById<TextView>(R.id.programmationZone)
        layout2.visibility = View.GONE
        layout3.visibility = View.GONE
        """Programmation du ${table[zoneId - 1]}""".also { programmationZone.text = it }
        val debut1h = findViewById<EditText>(R.id.debut1heure)
        val debut2h = findViewById<EditText>(R.id.debut2heure)
        val debut3h = findViewById<EditText>(R.id.debut3heure)
        timeUtils.setupEditHeures(debut1h)
        timeUtils.setupEditHeures(debut2h)
        timeUtils.setupEditHeures(debut3h)
        val debut1mn = findViewById<EditText>(R.id.debut1mn)
        val debut2mn = findViewById<EditText>(R.id.debut2mn)
        val debut3mn = findViewById<EditText>(R.id.debut3mn)
        timeUtils.setupEditMinutes(debut1mn)
        timeUtils.setupEditMinutes(debut2mn)
        timeUtils.setupEditMinutes(debut3mn)
        var nombreDeFois = 1

        rd1fois.setOnClickListener {
            if (rd1fois.isChecked) {
                layout2.visibility = View.GONE
                layout3.visibility = View.GONE
                nombreDeFois = 1
            }
        }
        rd2fois.setOnClickListener {
            if (rd2fois.isChecked) layout2.visibility = View.VISIBLE
            layout3.visibility = View.GONE
            nombreDeFois = 2
        }
        rd3fois.setOnClickListener {
            if (rd3fois.isChecked) layout2.visibility = View.VISIBLE
            if (rd3fois.isChecked) layout3.visibility = View.VISIBLE
            nombreDeFois = 3
        }


//----------------------------------------------------
        // Le bouton ENREGISTRER est toujours visible : il valide toutes les valeurs
        // (le retour arrière du téléphone annule sans rien modifier)
        findViewById<Button>(R.id.btnEnregistrer).setOnClickListener {
            // Chaque champ est vérifié : en cas de valeur vide ou hors limites,
            // on affiche l'erreur et on reste sur l'écran (pas de plantage, rien n'est envoyé)
            val champs = listOf(
                Triple(debut1h, debut1mn, editTextDuration1),
                Triple(debut2h, debut2mn, findViewById<EditText>(R.id.duration2)),
                Triple(debut3h, debut3mn, findViewById<EditText>(R.id.duration3)),
            )

            var enclair = ""
            var message = ""
            for (i in 0 until nombreDeFois) {
                val (champH, champMn, champDuree) = champs[i]
                val heure = lireNombre(champH, 0, 23, "L'heure") ?: return@setOnClickListener
                val minute = lireNombre(champMn, 0, 59, "Les minutes") ?: return@setOnClickListener
                val duree = lireNombre(champDuree, 0, 999, "La durée") ?: return@setOnClickListener

                val departS = timeUtils.convertToSeconds(heure, 1) + timeUtils.convertToSeconds(minute, 0)
                val dureeS = duree * 60
                val hh = heure.toString().padStart(2, '0')
                val mm = minute.toString().padStart(2, '0')

                enclair = when (i) {
                    0 -> "arrosage à ${hh}h$mm pour ${duree}mn "
                    1 -> "$enclair \net  à  ${hh}h$mm pour ${duree}mn"
                    else -> "$enclair  et à  ${hh}h$mm pour ${duree}mn"
                }
                // i = 0, 1, 2 : numéro de l'arrosage dans la journée
                message += "$zoneId/$departS/$dureeS/$i/&"
            }

            Log.d("----------------------", "enclair  $enclair")
            Log.d("--------------", "message $message")
            //  message   34570$1/34680/300/2/&2/36680/100/1/&
            // 34570 = nombre de secondes depuis minuit
            //  1,2,3,4 nombre de relais
            // par ex 34680 heure du départ en secondes depuis minuit
            // 300 durée arrosage en secondes
            // 0,1,2 numéro de l'arrosage pour ce relais
            //& sépare les blocs de commandes pour chaque relai
            val resultIntent = Intent().apply {
                putExtra("ZONE", zoneId.toString())
                putExtra("MESSAGE", message)
                putExtra("ENCLAIR", enclair)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    /**
     * Lit un nombre entier dans [champ] et vérifie qu'il est entre [min] et [max].
     * Renvoie null (et signale l'erreur sur le champ) si la valeur est vide ou invalide.
     */
    private fun lireNombre(champ: EditText, min: Int, max: Int, libelle: String): Int? {
        val valeur = champ.text.toString().trim().toIntOrNull()
        if (valeur == null || valeur < min || valeur > max) {
            val erreur = "$libelle doit être entre $min et $max"
            champ.error = erreur
            champ.requestFocus()
            Toast.makeText(this, erreur, Toast.LENGTH_SHORT).show()
            return null
        }
        return valeur
    }

}
