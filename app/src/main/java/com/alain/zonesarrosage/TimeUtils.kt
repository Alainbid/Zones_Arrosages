package com.alain.zonesarrosage

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText

/** Conversions en secondes et contrôle des saisies d'heures et de minutes. */
object TimeUtils {

    private val REGEX_HEURES = Regex("^(?:[01]?[0-9]|2[0-3])?$") // 0..23 ou vide
    private val REGEX_MINUTES = Regex("^(?:[0-5]?[0-9])?$")      // 0..59 ou vide

    // Les valeurs sont validées avant l'appel (heure 0..23, minutes 0..59)
    // dans ProgrammingActivity.lireNombre() ; 0 h est une heure valide.
    fun heuresEnSecondes(heures: Int): Int = heures * 3600

    fun minutesEnSecondes(minutes: Int): Int = minutes * 60

    fun setupEditHeures(editText: EditText) =
        controlerSaisie(editText, REGEX_HEURES, "Entrez une valeur entre 0 et 23")

    fun setupEditMinutes(editText: EditText) =
        controlerSaisie(editText, REGEX_MINUTES, "Entrez une valeur entre 0 et 59")

    /**
     * Limite la saisie à 2 caractères et affiche [message] sur le champ
     * tant que le texte ne correspond pas à [regex]. Un champ vide est
     * accepté ici ; il est refusé à l'enregistrement par lireNombre().
     */
    private fun controlerSaisie(editText: EditText, regex: Regex, message: String) {
        editText.filters = arrayOf(InputFilter.LengthFilter(2))
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editText.error = if (regex.matches(s?.toString().orEmpty())) null else message
            }
        })
    }
}
