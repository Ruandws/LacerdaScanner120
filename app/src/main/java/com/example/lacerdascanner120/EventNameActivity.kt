package com.example.lacerdascanner120

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class EventNameActivity : ComponentActivity() {
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
        val keyboardController = LocalSoftwareKeyboardController.current

        Box(
            modifier = Modifier.fillMaxSize() // Usa o Box para ocupar a tela inteira
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ícone no topo
                Image(
                    painter = painterResource(id = R.drawable.bsbappiconstart),
                    contentDescription = "Ícone de casa",
                    modifier = Modifier.size(48.dp) // Tamanho responsivo
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Título
                Text(
                    text = "Antes de Prosseguirmos...",
                    fontSize = 36.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Subtítulo
                Text(
                    text = "Por favor, insira o nome do evento a ser realizado",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
            TextField(
                value = eventName,
                onValueChange = { eventName = it },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .border(2.dp, Color(0xFF048CD4))
                    .height(47.dp),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send // Ou ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onSend = { // Ou onDone
                        // Lógica para enviar o texto
                        println("Nome do evento: $eventName")
                        keyboardController?.hide() //
                        // Lógica para salvar nome do evento
                        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("NOME_EVENTO", eventName.toString())
                        editor.apply() // Ou editor.commit()
                    }
                )
            )
            // Botão no canto inferior direito (agora dentro do BoxScope)
            IconButton(
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
                modifier = Modifier
                    .size(80.dp) // Tamanho do botão
                    .background(Color(0xFF048CD4)) // Cor de fundo
                    .align(Alignment.BottomEnd) // Posiciona no canto inferior direito (agora funciona)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos, // Ícone da seta
                    contentDescription = "Prosseguir",
                    tint = Color.Black // Cor preta para o ícone
                )
            }
        }
    }
}
