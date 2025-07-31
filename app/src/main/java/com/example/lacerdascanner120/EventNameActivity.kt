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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Importações para Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Importação da cor centralizada do seu módulo de UI/Tema
import com.example.lacerdascanner120.ui.theme.BlueMainColor

// Função auxiliar para filtrar caracteres, definida fora da Composable
// para evitar recriações desnecessárias.
private fun filterCharacters(text: String, regex: Regex): Pair<String, Boolean> {
    val filteredText = regex.replace(text, "")
    return Pair(filteredText, filteredText != text)
}

class EventNameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A função lambda agora recebe apenas contrato, anel e supervisor
            EventNameScreen { contratoName, anelName, supervisorName ->
                // Salvar dados no SharedPreferences
                val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("NOME_CONTRATO", contratoName)
                    putString("NOME_ANEL", anelName)
                    putString("NOME_SUPERVISOR", supervisorName)
                    apply()
                }

                // Navegar para MainActivity
                val intent = Intent(this@EventNameActivity, MainActivity::class.java).apply {
                    putExtra("EXTRA_CONTRATO_NAME", contratoName)
                    putExtra("EXTRA_ANEL_NAME", anelName)
                    putExtra("EXTRA_SUPERVISOR_NAME", supervisorName)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EventNameScreen(onEventNameEntered: (String, String, String) -> Unit) { // Assinatura da lambda atualizada
        val context = LocalContext.current
        val keyboardController = LocalSoftwareKeyboardController.current // Movido para o topo para melhor organização

        // --- Carregar dados dos CSVs usando CsvDataLoader de forma assíncrona ---
        // 1. Declare as listas como 'mutableStateOf' para que possam ser atualizadas
        //    após o carregamento em segundo plano.
        var contratos by remember { mutableStateOf(emptyList<DataModels.Contrato>()) }
        var aneis by remember { mutableStateOf(emptyList<DataModels.Anel>()) }

        // 2. Use LaunchedEffect para carregar os dados em um thread de I/O
        LaunchedEffect(Unit) { // 'Unit' garante que este bloco seja executado apenas uma vez
            launch(Dispatchers.IO) { // 'Dispatchers.IO' move a operação para um thread de I/O
                contratos = CsvDataLoader.loadContratosFromCsv(context)
                aneis = CsvDataLoader.loadAneisFromCsv(context)
            }
        }
        // --- Fim do Carregamento de Dados Assíncrono ---

        // Estados para os campos de texto e seleção
        var contratoText by remember { mutableStateOf(TextFieldValue("")) }
        var anelText by remember { mutableStateOf(TextFieldValue("")) }
        var supervisorName by remember { mutableStateOf(TextFieldValue("")) }

        var selectedContrato by remember { mutableStateOf<DataModels.Contrato?>(null) }
        var selectedAnel by remember { mutableStateOf<DataModels.Anel?>(null) }

        // Estados para controlar a expansão dos dropdowns
        var isContratoExpanded by remember { mutableStateOf(false) }
        var isAnelExpanded by remember { mutableStateOf(false) }

        var isVisible by remember { mutableStateOf(false) } // Mantém a declaração original

        // Animação de entrada da tela - Retornada à posição original para controle visual
        LaunchedEffect(Unit) {
            isVisible = true
        }

        val scale by animateFloatAsState(targetValue = if (isVisible) 1f else 0.8f, label = "scaleAnimation")
        val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, label = "alphaAnimation")

        // Regex para filtrar caracteres inválidos (apenas para o campo de supervisor, que é editável)
        val supervisorRegex = remember { Regex("[^a-zA-Z0-9\\s.]") }

        // Listas filtradas para os dropdowns (ainda aplicam filtro para a função de pesquisa visual)
        // A lógica de filtro está correta e será executada no thread da UI,
        // mas como as listas são pequenas (máx. 10 itens), isso é aceitável.
        val filteredContratos = remember(contratoText.text, contratos) {
            if (contratoText.text.isEmpty()) {
                contratos // Agora 'contratos' é um estado que será atualizado
            } else {
                contratos.filter {
                    it.nome.contains(contratoText.text, ignoreCase = true)
                }
            }
        }

        val filteredAneis = remember(anelText.text, selectedContrato, aneis) {
            if (selectedContrato == null) {
                emptyList() // Se nenhum contrato for selecionado, não há anéis para mostrar
            } else {
                aneis.filter {
                    it.contratoId == selectedContrato?.id &&
                            it.nome.contains(anelText.text, ignoreCase = true)
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Centraliza os elementos na horizontal
            ) {
                // Animação do Ícone
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

                // Animação do Título "Novo Evento"
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

                // Animação da Descrição
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

                // --- Campo de Pesquisa/Seleção para "Nome do Contrato" ---
                ExposedDropdownMenuBox(
                    expanded = isContratoExpanded,
                    onExpandedChange = {
                        isContratoExpanded = !isContratoExpanded
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = contratoText,
                        onValueChange = {
                            // Este campo é somente leitura, então a mudança ocorre apenas via seleção do DropdownMenuItem.
                            // No entanto, se quiser permitir que o usuário digite para FILTRAR, você pode adicionar a lógica aqui.
                            // Por ora, ele é estritamente de seleção.
                            // newValue -> { contratoText = newValue; isContratoExpanded = true }
                        },
                        readOnly = true, // Torna o campo somente leitura
                        label = { Text("Nome do Contrato") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isContratoExpanded) },
                        modifier = Modifier
                            .menuAnchor() // Necessário para o ExposedDropdownMenuBox
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    ExposedDropdownMenu(
                        expanded = isContratoExpanded,
                        onDismissRequest = { isContratoExpanded = false }
                    ) {
                        // Mensagem de carregamento se as listas ainda estiverem vazias
                        if (filteredContratos.isEmpty() && contratos.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Carregando contratos...") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else if (filteredContratos.isEmpty() && contratoText.text.isNotEmpty()) {
                            // Mensagem se não houver resultados para a pesquisa (se a digitação fosse permitida)
                            DropdownMenuItem(
                                text = { Text("Nenhum contrato encontrado") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else {
                            filteredContratos.forEach { contrato ->
                                DropdownMenuItem(
                                    text = { Text(contrato.nome) },
                                    onClick = {
                                        contratoText = TextFieldValue(contrato.nome) // Atualiza o texto do campo
                                        selectedContrato = contrato // Salva o objeto selecionado
                                        isContratoExpanded = false // Fecha o dropdown
                                        // Resetar Anel quando o Contrato muda para forçar nova seleção
                                        selectedAnel = null
                                        anelText = TextFieldValue("")
                                    }
                                )
                            }
                        }
                    }
                }

                // --- Campo de Pesquisa/Seleção para "Nome do Anel" (dependente do Contrato) ---
                ExposedDropdownMenuBox(
                    expanded = isAnelExpanded,
                    onExpandedChange = {
                        // Só expande se um contrato já estiver selecionado
                        isAnelExpanded = selectedContrato != null && !isAnelExpanded
                        if (selectedContrato == null) {
                            Toast.makeText(context, "Selecione um Contrato primeiro.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = anelText,
                        onValueChange = {
                            // Este campo é somente leitura
                        },
                        readOnly = true, // Torna o campo somente leitura
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
                        // Mensagem de carregamento se as listas ainda estiverem vazias
                        if (selectedContrato == null) {
                            DropdownMenuItem(
                                text = { Text("Selecione um Contrato primeiro") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else if (filteredAneis.isEmpty() && aneis.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Carregando anéis...") },
                                onClick = { /* Não faz nada */ },
                                enabled = false
                            )
                        } else if (filteredAneis.isEmpty() && anelText.text.isNotEmpty()) {
                            // Mensagem se não houver resultados para a pesquisa (se a digitação fosse permitida)
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
                                    }
                                )
                            }
                        }
                    }
                }

                // REMOVIDO: Campo de Pesquisa para "Nome do Posto"

                // Campo "Nome do Supervisor" - continua editável
                OutlinedTextField(
                    value = supervisorName,
                    onValueChange = { newValue ->
                        val (filteredText, modified) = filterCharacters(newValue.text, supervisorRegex)
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
                        // Apenas Contrato, Anel e Supervisor são obrigatórios agora
                        if (selectedContrato != null && selectedAnel != null && supervisorName.text.isNotEmpty()) {
                            onEventNameEntered(
                                selectedContrato!!.nome,
                                selectedAnel!!.nome,
                                supervisorName.text
                            )
                        } else {
                            Toast.makeText(context, "Por favor, preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueMainColor)
                ) {
                    Text("Continuar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}