package com.example.lacerdascanner120

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter

class EventNameActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventNameScreen { eventName ->
                // Salvar nome do evento e navegar para MainActivity
                val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("NOME_EVENTO", eventName)
                    apply()
                }

                val intent = Intent(this@EventNameActivity, MainActivity::class.java)
                startActivity(intent)
                finish() // Fecha a Activity para não voltar ao pressionar "back"
            }
        }
    }

    @Composable
    fun EventNameScreen(onEventNameEntered: (String) -> Unit) {
        var eventName by remember { mutableStateOf(TextFieldValue("")) }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bsbappiconstart),  // Escolha o ícone desejado
                contentDescription = "Ícone de casa",  // Descrição do ícone para acessibilidade
                modifier = Modifier.size(24.dp)  // Define o tamanho do ícone
            )
            Text(
                text = "Antes de Prosseguirmos...",
                fontSize = 36.sp,
                modifier = Modifier.padding(top = 50.dp, bottom = 32.dp)
            )
            Text(
                text = "Por favor, insira o nome do evento a ser realizado",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BasicTextField(
                value = eventName,
                onValueChange = { eventName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (eventName.text.isNotEmpty()) {
                        val sharedPref =
                            context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("NOME_EVENTO", eventName.text)
                            apply()
                        }
                        onEventNameEntered(eventName.text)
                    } else {
                        Toast.makeText(
                            context,
                            "Por favor, insira um nome!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Color(0xFF048CD4)), // Define a cor de fundo
            ) {
                Text(text = "Continuar")
            }
            // Adiciona o GIF abaixo do botão
            Image(
                painter = rememberImagePainter("file:///android_asset/Mobile Inbox"), // Caminho do GIF
                contentDescription = "Animação GIF",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            )
        }
    }
}
















