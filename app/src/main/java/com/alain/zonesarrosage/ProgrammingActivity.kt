package com.alain.zonesarrosage

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.alain.zonesarrosage.R.color


class ProgrammingActivity : AppCompatActivity() {
    private lateinit var rd1fois: RadioButton
    private lateinit var rd2fois: RadioButton
    private lateinit var rd3fois: RadioButton
    private lateinit var layout2: ConstraintLayout
    private lateinit var layout3: ConstraintLayout
    private lateinit var timeUtils: TimeUtils
    private var depart2mn: Int = 0
    private var duration2: Int = 0
    private var depart2s: Int = 0
    private var depart3mn: Int = 0
    private var duration3: Int = 0
    private var depart3s: Int = 0
    private var duree2: String = "0"
    private var duree3: String = "0"

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


        editTextDuration1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                programmationZone.text = getString(R.string.enregistrer)
                programmationZone.textSize= 24F
                programmationZone.width = 800
                programmationZone.setBackgroundColor(
                    ContextCompat.getColor(
                        applicationContext,
                        color.teal_700
                    )
                )
                programmationZone.setTextColor(
                    ContextCompat.getColor(
                        applicationContext,
                        color.white
                    )
                )
            }
        })

//----------------------------------------------------
programmationZone.setOnClickListener {
//si on clique sur le titre on valid toutes les valeurs

Log.d("---------", "debut1h.text   ${debut1h.text}")
            var depart3heure: String
            var depart2heure: String
            var debut3minute   : String
            var debut2minute: String
            var enclair  : String
            var message  : String

            var depart1heure : String=(debut1h.text.toString())
            if(depart1heure.length == 1) depart1heure = "0$depart1heure"
            var depart1s = timeUtils.convertToSeconds(depart1heure.toInt(), 1)

            var debut1minute =   (debut1mn  .text.toString())
            if(debut1minute.length ==1) debut1minute = "0$debut1minute"
             val depart1mn = timeUtils.convertToSeconds(debut1minute.toInt(), 0)

           // val duree1 = findViewById<TextView>(R.id.duration1).text.toString()
            val duration1 = editTextDuration1.text.toString().toInt() * 60 // duration1 en s
            depart1s += depart1mn
            enclair = "arrosage à ${depart1heure}h$debut1minute pour ${editTextDuration1.text}mn "
            message = "$zoneId/$depart1s/$duration1/0/&"

            if (nombreDeFois > 1) {

                depart2heure=   (debut2h  .text.toString())
                if(depart2heure.length == 1) depart2heure = "0$depart2heure"
                depart2s = timeUtils.convertToSeconds(depart2heure.toInt(), 1)

               debut2minute =   (debut2mn  .text.toString())
                if(debut2minute.length ==1) debut2minute = "0$debut2minute"
                depart2mn = timeUtils.convertToSeconds(debut2minute.toInt(), 0)
                depart2s += depart2mn
                duree2 = findViewById<TextView>(R.id.duration2).text.toString()
                duration2 = duree2.toInt() * 60

                enclair = "$enclair \net  à  ${depart2heure}h$debut2minute pour ${duree2}mn"
                message = "$zoneId/$depart1s/$duration1/0/&$zoneId/$depart2s/$duration2/1/&"
            }

            if (nombreDeFois == 3) {

                depart3heure=   (debut3h  .text.toString())
                if(depart3heure.length == 1) depart3heure = "0$depart3heure"
                depart3s = timeUtils.convertToSeconds(depart3heure.toInt(), 1)

                debut3minute =   (debut3mn  .text.toString())
                if(debut3minute.length ==1) debut3minute = "0$debut3minute"
                depart3mn = timeUtils.convertToSeconds(debut3minute.toInt(), 0)

                duree3 = findViewById<TextView>(R.id.duration3).text.toString()
                duration3 = duree3.toInt() * 60
                depart3s += depart3mn
                enclair = "$enclair  et à  ${depart3heure}h$debut3minute pour ${duree3}mn"
                message = "$zoneId/$depart1s/$duration1/0/&$zoneId/$depart2s/$duration2/1/&$zoneId/$depart3s/$duration3/2/&"
            }


            Log.d("----------------------", "enclair  $enclair")
            Log.d("--------------", "message $message")
                //  message   34570$1/34680/300/2/&2/36680/100/1/&
                // 34570 = nombre de secondes depuis minuit
                //  1,2,3,4 nombre de relais
                // par ex 34680 heure du départ en secondes depuis minuit
                // 300 durée arrosage en secondes
                // 1,2,3 nombre de fois par relais
                //& sépare les blocs de commandes pour chaque relai
                // Set result and finish when done
                val resultIntent = Intent().apply {
                    putExtra("ZONE", zoneId.toString())
                    putExtra("MESSAGE", message)
                    putExtra("ENCLAIR", enclair)
                }
                setResult(RESULT_OK, resultIntent)
               finish()
            }
        }

    }

