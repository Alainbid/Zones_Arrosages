package com.alain.zonesarrosage

import android.content.Context
import android.util.Log
import android.widget.Toast
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText

class TimeUtils(private val context: Context) {

    fun convertToSeconds(time: Int, type: Int): Int {
        //if type = 0 conversion des mn  1 conversion des heures
        if (type == 1) {
// conversion des heures en secondes
            Log.d("-----------------", "Heures = $time")
            return if (time > 23 || time == 0) {
                Log.d(" ------------------  time  ","$time")
                Toast.makeText(context, "Heure ne peut dépasser 23h.", Toast.LENGTH_SHORT).show()
                1000
            } else {
                time * 3600
            } // heure en secondes

// conversion des mn en secondes
        } else {
            val mn: Int
            if (time > 59) {
                Toast.makeText(context, "La durée maximum est de 60mn.", Toast.LENGTH_SHORT).show()
                mn = 59 * 60
                Log.d("--------------", "mn  $mn")
            } else {
                mn = time * 60
                Log.d("-----------------", "durée = $mn")
            }
            return mn
        }
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


