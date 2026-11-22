package com.alain.zonesarrosage

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText

class TimeUtils {

    fun convertToSeconds(time: Int, type: Int): Int {
        // type = 0 : conversion des minutes, type = 1 : conversion des heures
        // Les valeurs sont validées avant l'appel (heure 0..23, minutes 0..59)
        // dans ProgrammingActivity.lireNombre() ; 0h est une heure valide.
        return if (type == 1) time * 3600 else time * 60
    }


    fun setupEditHeures(editText: EditText    ) {
        // Limit the input length to 2 digits
     //   Log.d("------TimeUtils", "Adding TextWatcher to EditText with ID: ${editText.id}")
        editText.filters = arrayOf(InputFilter.LengthFilter(2))

        // Regex to match numbers between 0 and 59 (or adjust for 0-23)
        val regex = Regex("^(?:[01]?[0-9]|2[0-3])?$")

       editText.addTextChangedListener(object : TextWatcher {
           override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
           }

           override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
           }

           override fun afterTextChanged(s: Editable?) {

               s?.let {
                   // Validate input
                   if (!regex.matches(it.toString())) {
                     //  Log.d("--------TimeUtils", "Invalid input: ${it.toString()}")
                       editText.error = "Entrez une valeur entre 0 and 23"
                       editText.requestFocus() // Ensure focus remains on EditText
                   } else {
                    //   Log.d("---------TimeUtils", "Valid input: ${it.toString()}")
                       editText.error = null // Clear the error if input is valid
                   }
               }
           }
       })
    }

           fun setupEditMinutes(editText: EditText) {
               // Limit the input length to 2 digits
             //  Log.d("------TimeUtils", "Adding TextWatcher to EditText with ID: ${editText.id}")
               editText.filters = arrayOf(InputFilter.LengthFilter(2))

               // Regex to match numbers between 0 and 59 (or adjust for 0-23)

               val regex = Regex("^(?:[0-5]?[0-9])?$")

               editText.addTextChangedListener(object : TextWatcher {
                   override fun afterTextChanged(s: Editable?) {
                       s?.let {
                           // Validate input
                           if (!regex.matches(it.toString())) {
                          //     Log.d("--------TimeUtils", "Invalid input: ${it.toString()}")
                               editText.error = "Entrez une valeur entre 0 and 59"
                               editText.requestFocus() // Ensure focus remains on EditText
                           } else {
                         //      Log.d("---------TimeUtils", "Valid input: ${it.toString()}")
                               editText.error = null // Clear the error if input is valid
                           }
                       }
                   }


                   override fun beforeTextChanged(
                       s: CharSequence?,
                       start: Int,
                       count: Int,
                       after: Int
                   ) {
                   }

                   override fun onTextChanged(
                       s: CharSequence?,
                       start: Int,
                       before: Int,
                       count: Int
                   ) {
                   }
               })
           }
       }


