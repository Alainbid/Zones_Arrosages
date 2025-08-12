package com.alain.zonesarrosage

// Routines.kt

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.time.LocalDateTime

class Routines {
    companion object {
        fun secondsSinceMidnight(): Int {
            val currentDateTime = LocalDateTime.now()
            val hr = currentDateTime.hour * 3600
            val mn = currentDateTime.minute * 60
            val ss = currentDateTime.second
            return (hr  + mn + ss)
        }


        fun modifierLeFond(context: Context,   zone: String?, resultat1 : TextView, resultat2 : TextView, resultat3 : TextView, resultat4: TextView) {
            //  pour flouter les images  quand on a effectué une modification sur une zone
            when (zone) {
                "1"-> {
                    resultat1.height = 200
                    resultat1.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_200))
                }
                "2"-> {
                    resultat2.height = 200
                    resultat2.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_200))
                }
                "3" -> {
                    resultat3.height = 200
                    resultat3.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_200))
                }
                "4" -> {
                    resultat4.height = 200
                    resultat4.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_200))
                }
            }
        }

        
    }
}
