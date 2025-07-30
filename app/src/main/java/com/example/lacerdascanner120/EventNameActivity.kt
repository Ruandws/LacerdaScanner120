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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

// --- Modelagem de Dados Atualizada ---
data class Contrato(val id: Int, val nome: String)
data class Anel(val id: Int, val nome: String, val contratoId: Int)
data class Posto(val id: Int, val nome: String, val anelId: Int)

class EventNameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventNameScreen { contratoName, anelName, postoName, supervisorName -> // Adicionado postoName
                // Navegar para MainActivity
                val intent = Intent(this@EventNameActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EventNameScreen(onEventNameEntered: (String, String, String, String) -> Unit) { // Adicionado postoName
        val context = LocalContext.current

        // --- Carregar dados dos CSVs ---
        val contratos = remember {
            loadContratosFromCsv(context)
        }
        val aneis = remember {
            loadAneisFromCsv(context)
        }
        val postos = remember { // Novo carregamento para Postos
            loadPostosFromCsv(context)
        }
        // --- Fim do Carregamento de Dados ---

        var contratoText by remember { mutableStateOf(TextFieldValue("")) }
        var anelText by remember { mutableStateOf(TextFieldValue("")) }
        var postoText by remember { mutableStateOf(TextFieldValue("")) } // Novo estado para Posto
        var supervisorName by remember { mutableStateOf(TextFieldValue("")) }

        var selectedContrato by remember { mutableStateOf<Contrato?>(null) }
        var selectedAnel by remember { mutableStateOf<Anel?>(null) }
        var selectedPosto by remember { mutableStateOf<Posto?>(null) } // Novo estado para Posto

        var isContratoExpanded by remember { mutableStateOf(false) }
        var isAnelExpanded by remember { mutableStateOf(false) }
        var isPostoExpanded by remember { mutableStateOf(false) } // Novo estado para Posto

        val keyboardController = LocalSoftwareKeyboardController.current
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isVisible = true
        }

        val scale by animateFloatAsState(targetValue = if (isVisible) 1f else 0.8f, label = "scaleAnimation")
        val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, label = "alphaAnimation")

        val regex = remember { Regex("[^a-zA-Z0-9\\s.]") } // Permite espaços e pontos

        fun filtrarCaracteres(texto: String): Pair<String, Boolean> {
            val textoFiltrado = regex.replace(texto, "")
            return Pair(textoFiltrado, textoFiltrado != texto)
        }

        // Listas filtradas para os dropdowns
        val filteredContratos = remember(contratoText.text) {
            if (contratoText.text.isEmpty()) {
                contratos
            } else {
                contratos.filter {
                    it.nome.contains(contratoText.text, ignoreCase = true)
                }
            }
        }

        val filteredAneis = remember(anelText.text, selectedContrato) {
            if (selectedContrato == null) {
                emptyList() // Se nenhum contrato for selecionado, não há anéis para mostrar
            } else {
                aneis.filter {
                    it.contratoId == selectedContrato?.id &&
                            it.nome.contains(anelText.text, ignoreCase = true)
                }
            }
        }

        val filteredPostos = remember(postoText.text, selectedAnel) { // Nova lista filtrada para Postos
            if (selectedAnel == null) {
                emptyList() // Se nenhum anel for selecionado, não há postos para mostrar
            } else {
                postos.filter {
                    it.anelId == selectedAnel?.id &&
                            it.nome.contains(postoText.text, ignoreCase = true)
                }
            }
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
                        contentDescription = "Ícone do aplicativo",
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

                // --- Campo de Pesquisa para "Nome do Contrato" ---
                ExposedDropdownMenuBox(
                    expanded = isContratoExpanded,
                    onExpandedChange = {
                        isContratoExpanded = !isContratoExpanded
                        if (!isContratoExpanded) {
                            if (selectedContrato?.nome != contratoText.text) {
                                contratoText = TextFieldValue("")
                                selectedContrato = null
                                selectedAnel = null // Limpa anel e posto
                                anelText = TextFieldValue("")
                                selectedPosto = null
                                postoText = TextFieldValue("")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = contratoText,
                        onValueChange = { newValue ->
                            val (filteredText, modified) = filtrarCaracteres(newValue.text)
                            contratoText = newValue.copy(text = filteredText)
                            isContratoExpanded = true
                            selectedContrato = null
                            selectedAnel = null // Limpa anel e posto
                            anelText = TextFieldValue("")
                            selectedPosto = null
                            postoText = TextFieldValue("")
                            if (modified) {
                                Toast.makeText(context, "Caracteres inválidos removidos!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        readOnly = false,
                        label = { Text("Nome do Contrato") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isContratoExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    ExposedDropdownMenu(
                        expanded = isContratoExpanded,
                        onDismissRequest = { isContratoExpanded = false }
                    ) {
                        filteredContratos.forEach { contrato ->
                            DropdownMenuItem(
                                text = { Text(contrato.nome) },
                                onClick = {
                                    contratoText = TextFieldValue(contrato.nome)
                                    selectedContrato = contrato
                                    isContratoExpanded = false
                                    selectedAnel = null // Reseta anel e posto
                                    anelText = TextFieldValue("")
                                    selectedPosto = null
                                    postoText = TextFieldValue("")
                                }
                            )
                        }
                    }
                }

                // --- Campo de Pesquisa para "Nome do Anel" (dependente do Contrato) ---
                ExposedDropdownMenuBox(
                    expanded = isAnelExpanded,
                    onExpandedChange = {
                        isAnelExpanded = !isAnelExpanded
                        if (!isAnelExpanded) {
                            if (selectedAnel?.nome != anelText.text) {
                                anelText = TextFieldValue("")
                                selectedAnel = null
                                selectedPosto = null // Limpa o posto
                                postoText = TextFieldValue("")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = anelText,
                        onValueChange = { newValue ->
                            val (filteredText, modified) = filtrarCaracteres(newValue.text)
                            anelText = newValue.copy(text = filteredText)
                            isAnelExpanded = true
                            selectedAnel = null
                            selectedPosto = null // Limpa o posto
                            postoText = TextFieldValue("")
                            if (modified) {
                                Toast.makeText(context, "Caracteres inválidos removidos!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        readOnly = selectedContrato == null, // Só pode digitar se um contrato for selecionado
                        label = { Text("Nome do Anel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAnelExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    ExposedDropdownMenu(
                        expanded = isAnelExpanded,
                        onDismissRequest = { isAnelExpanded = false }
                    ) {
                        if (selectedContrato == null) {
                            DropdownMenuItem(
                                text = { Text("Selecione um Contrato primeiro") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else if (filteredAneis.isEmpty() && anelText.text.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhum anel encontrado") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else {
                            filteredAneis.forEach { anel ->
                                DropdownMenuItem(
                                    text = { Text(anel.nome) },
                                    onClick = {
                                        anelText = TextFieldValue(anel.nome)
                                        selectedAnel = anel
                                        isAnelExpanded = false
                                        selectedPosto = null // Reseta o posto ao selecionar um novo anel
                                        postoText = TextFieldValue("")
                                    }
                                )
                            }
                        }
                    }
                }

                // --- Campo de Pesquisa para "Nome do Posto" (dependente do Anel) ---
                ExposedDropdownMenuBox(
                    expanded = isPostoExpanded,
                    onExpandedChange = {
                        isPostoExpanded = !isPostoExpanded
                        if (!isPostoExpanded) {
                            if (selectedPosto?.nome != postoText.text) {
                                postoText = TextFieldValue("")
                                selectedPosto = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = postoText,
                        onValueChange = { newValue ->
                            val (filteredText, modified) = filtrarCaracteres(newValue.text)
                            postoText = newValue.copy(text = filteredText)
                            isPostoExpanded = true
                            selectedPosto = null
                            if (modified) {
                                Toast.makeText(context, "Caracteres inválidos removidos!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        readOnly = selectedAnel == null, // Só pode digitar se um anel for selecionado
                        label = { Text("Nome do Posto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPostoExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    ExposedDropdownMenu(
                        expanded = isPostoExpanded,
                        onDismissRequest = { isPostoExpanded = false }
                    ) {
                        if (selectedAnel == null) {
                            DropdownMenuItem(
                                text = { Text("Selecione um Anel primeiro") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else if (filteredPostos.isEmpty() && postoText.text.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhum posto encontrado") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else {
                            filteredPostos.forEach { posto ->
                                DropdownMenuItem(
                                    text = { Text(posto.nome) },
                                    onClick = {
                                        postoText = TextFieldValue(posto.nome)
                                        selectedPosto = posto
                                        isPostoExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

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
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (selectedContrato != null && selectedAnel != null && selectedPosto != null && supervisorName.text.isNotEmpty()) {
                            val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("NOME_CONTRATO", selectedContrato?.nome)
                                putString("NOME_ANEL", selectedAnel?.nome)
                                putString("NOME_POSTO", selectedPosto?.nome)
                                putString("NOME_SUPERVISOR", supervisorName.text)
                                apply()
                            }
                            onEventNameEntered(
                                selectedContrato!!.nome,
                                selectedAnel!!.nome,
                                selectedPosto!!.nome,
                                supervisorName.text
                            )
                        } else {
                            Toast.makeText(context, "Por favor, preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF048cd4))
                ) {
                    Text("Continuar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // --- Funções para carregar dados do CSV ---
    private fun loadContratosFromCsv(context: Context): List<Contrato> {
        val contratos = mutableListOf<Contrato>()
        try {
            val inputStream = context.resources.openRawResource(R.raw.contrato)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Pula a linha do cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size == 2) { // ID, Nome
                    contratos.add(Contrato(parts[0].trim().toInt(), parts[1].trim()))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao carregar contratos: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return contratos
    }

    private fun loadAneisFromCsv(context: Context): List<Anel> {
        val aneis = mutableListOf<Anel>()
        try {
            val inputStream = context.resources.openRawResource(R.raw.anel)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Pula a linha do cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size == 3) { // ID, Nome, Contrato_ID
                    aneis.add(Anel(parts[0].trim().toInt(), parts[1].trim(), parts[2].trim().toInt()))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao carregar anéis: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return aneis
    }

    private fun loadPostosFromCsv(context: Context): List<Posto> {
        val postos = mutableListOf<Posto>()
        try {
            val inputStream = context.resources.openRawResource(R.raw.postos)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Pula a linha do cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size == 3) { // ID, Nome, Anel_ID
                    postos.add(Posto(parts[0].trim().toInt(), parts[1].trim(), parts[2].trim().toInt()))
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