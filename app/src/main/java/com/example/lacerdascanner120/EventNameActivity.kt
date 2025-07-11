package com.example.lacerdascanner120
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
            EventNameScreen { eventName, supervisorName ->
                // Navegar para MainActivity
                val intent = Intent(this@EventNameActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EventNameScreen(onEventNameEntered: (String, String) -> Unit) {
        var eventName by remember { mutableStateOf(TextFieldValue("")) }
        var supervisorName by remember { mutableStateOf(TextFieldValue("")) }
        val context = LocalContext.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isVisible = true
        }

        val scale by animateFloatAsState(targetValue = if (isVisible) 1f else 0.8f)
        val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f)

        val regex = remember { Regex("[^a-zA-Z0-9 ]") } // Pré-compila a expressão regular

        fun filtrarCaracteres(texto: String): Pair<String, Boolean> {
            val textoFiltrado = regex.replace(texto, "")
            return Pair(textoFiltrado, textoFiltrado != texto)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bsbappiconstart),
                        contentDescription = "Ícone de casa",
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally)
                            .scale(scale)
                            .alpha(alpha)
                    )
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Novo Evento",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .alpha(alpha)
                    )
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Insira os detalhes do evento para continuar.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .alpha(alpha)
                    )
                }

                OutlinedTextField(
                    value = eventName,
                    onValueChange = { newValue ->
                        val (filteredText, modified) = filtrarCaracteres(newValue.text)
                        eventName = newValue.copy(text = filteredText)
                        if (modified) {
                            Toast.makeText(context, "Caracteres inválidos removidos!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome do Evento") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { /* Mover para o próximo campo */ }),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = supervisorName,
                    onValueChange = { newValue ->
                        val (filteredText, modified) = filtrarCaracteres(newValue.text)
                        supervisorName = newValue.copy(text = filteredText)
                        if (modified) {
                            Toast.makeText(context, "Caracteres inválidos removidos!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome do Supervisor") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        if (eventName.text.isNotEmpty() && supervisorName.text.isNotEmpty()) {
                            val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("NOME_EVENTO", eventName.text)
                                putString("NOME_SUPERVISOR", supervisorName.text)
                                apply()
                            }
                            onEventNameEntered(eventName.text, supervisorName.text)
                        } else {
                            Toast.makeText(context, "Por favor, preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        }
                    }),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (eventName.text.isNotEmpty() && supervisorName.text.isNotEmpty()) {
                            val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("NOME_EVENTO", eventName.text)
                                putString("NOME_SUPERVISOR", supervisorName.text)
                                apply()
                            }
                            onEventNameEntered(eventName.text, supervisorName.text)
                        } else {
                            Toast.makeText(context, "Por favor, preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF048cd4)) // Cor do botão modificada
                ) {
                    Text("Continuar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}







