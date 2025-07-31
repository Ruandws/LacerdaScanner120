package com.example.lacerdascanner120

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.graphics.Color // Import necessário para Color
import java.io.BufferedReader
import java.io.InputStreamReader


// Objeto singleton para carregar dados CSV de forma centralizada
object CsvDataLoader {

    fun loadContratosFromCsv(context: Context): List<DataModels.Contrato> {
        val contratos = mutableListOf<DataModels.Contrato>()
        try {
            // Abre o recurso raw/contrato.csv
            val inputStream = context.resources.openRawResource(R.raw.contrato)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Pula a linha do cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size == 2) { // Espera formato: ID, Nome
                    contratos.add(DataModels.Contrato(parts[0].trim().toInt(), parts[1].trim()))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            // Em um aplicativo real, considere usar um logger mais robusto (ex: Timber)
            Toast.makeText(context, "Erro ao carregar contratos: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return contratos
    }

    fun loadAneisFromCsv(context: Context): List<DataModels.Anel> {
        val aneis = mutableListOf<DataModels.Anel>()
        try {
            // Abre o recurso raw/anel.csv
            val inputStream = context.resources.openRawResource(R.raw.anel)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Pula a linha do cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size == 3) { // Espera formato: ID, Nome, Contrato_ID
                    aneis.add(DataModels.Anel(parts[0].trim().toInt(), parts[1].trim(), parts[2].trim().toInt()))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao carregar anéis: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return aneis
    }

    fun loadPostosFromCsv(context: Context): List<DataModels.Posto> {
        val postos = mutableListOf<DataModels.Posto>()
        try {
            // Abre o recurso raw/postos.csv
            val inputStream = context.resources.openRawResource(R.raw.postos)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Pula a linha do cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size == 3) { // Espera formato: ID, Nome, Anel_ID
                    postos.add(DataModels.Posto(parts[0].trim().toInt(), parts[1].trim(), parts[2].trim().toInt()))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao carregar postos: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return postos
    }
}