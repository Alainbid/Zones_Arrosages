package com.alain.zonesarrosage

import org.junit.Test
import org.robolectric.annotation.Config
import java.time.LocalDateTime

//@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    manifest = "src/main/AndroidManifest.xml"
)  // Utilisez le bon chemin vers votre AndroidManifest.xml
class MainActivityTest {

    @Test
    fun secondsSinceMidnight() {
        val currentDateTime = LocalDateTime.now()
        val hr = currentDateTime.hour * 3600

        val mn = currentDateTime.minute * 60
        val sc = currentDateTime.second            // Ajouter les secondes actuelles


        // Calculer le nombre total de secondes depuis minuit
        val totalSeconds = hr + mn + sc

        val hh = totalSeconds / 3600
        val rsec = totalSeconds % 3600
        val mm = rsec / 60

        // Afficher le résultat pour débogage
        println("Seconds since midnight: $hh : $mm")
    }
}








