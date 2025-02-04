package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {

    //Variável Global pro campo de texto
    val eventText = mutableStateOf("")

    //Configuração da ActionBar
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomTopBar() {
        TopAppBar(
            title = {
                Text(
                    text = "Lacerda Scanner",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = Color(0xFF048cd4), // Cor de fundo
                titleContentColor = Color.White // Cor do título
            ),
        )
    }

                            //----------Lógica do QRCode---------//
    @SuppressLint("SimpleDateFormat")
    private var barCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this@MainActivity, "Cancelado. Tente novamente.", Toast.LENGTH_SHORT)
                .show()
        } else {
            val currentDateTime = Calendar.getInstance().time //Pegando hora atual
            val formattedDate = SimpleDateFormat("dd/MM/yyyy").format(currentDateTime) // Organizando data
            val formattedTime = SimpleDateFormat("HH:mm:ss").format(currentDateTime) // Organizando hora

            val qrCodeWithDate = "${result.contents} - $formattedDate, $formattedTime"
            qrCodeHistory.add(qrCodeWithDate)
        }
    }

    private fun showCamera() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Aponte para um QR Code válido")
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setOrientationLocked(false)

        barCodeLauncher.launch(options)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGrantted ->
        if (isGrantted) {
            showCamera()
        }
    }

    private fun checkCameraPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showCamera()
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(this@MainActivity, "Cancelado. Tente novamente.", Toast.LENGTH_SHORT)
                .show()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
             //-----------Lógica do Histórico--------//
    private var qrCodeHistory = mutableStateListOf<String>() // Lista reativa para o histórico
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                qrCodeHistory = qrCodeHistory,
                onScanQrCode = { showCamera() },
                onClearHistory = { qrCodeHistory.clear() }
            )
        }
    }

                            //----------Lógica do Export CSV-----//
    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            exportToCSV(uri) // Passa a URI escolhida pelo usuário para a função de exportação
        }
    }
    // Export
    private fun exportToCSV(uri: Uri) {
        val nomeDoEvento = eventText.value.trim()

        // Verifica se o nome do evento foi preenchido
        if (nomeDoEvento.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome do evento antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = qrCodeHistory.joinToString("\n") { qrCodeWithDate ->
            val parts = qrCodeWithDate.split(" - ", limit = 2)
            if (parts.size == 2) {
                "$nomeDoEvento, ${parts[0]}, ${parts[1]}"  //
            } else {
                "$nomeDoEvento, $qrCodeWithDate"
            }
        }

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("Evento, Funcionario, Matricula, Data, Hora\n") // Cabeçalho atualizado
                    writer.write(csvContent)
                }
            }
            Toast.makeText(this, "QR Codes exportados para $uri", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao exportar para CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para iniciar o seletor de arquivos para salvar o CSV (Obriga o usuário a dizer o Evento)
    private fun promptSaveCsv() {
        if (eventText.value.trim().isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome do evento antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "qr_codes.csv"
        createFileLauncher.launch(fileName)
    }

                            //---------------Barra Inferior de Opções-----------//
    @Composable
    fun MainScreen(
        qrCodeHistory: List<String>,
        onScanQrCode: () -> Unit,
        onClearHistory: () -> Unit
    ) {
        val context = LocalContext.current  // Acessa o contexto local

        Scaffold(
            topBar = { CustomTopBar() }, //Adiciona a topbar
            bottomBar = {
                BottomAppBar {
                    Row( // Adiciona Row para suportar Modifier.weight()
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = onClearHistory,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, // Ícone de "limpar"
                                contentDescription = "Limpar Histórico",
                                modifier = Modifier.size(64.dp), // Tamanho do ícone
                                tint = Color(0xFF048cd4)
                            )
                        }
                        IconButton(
                            onClick = onScanQrCode,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera, // Ícone de "escanear"
                                contentDescription = "Escanear QR Code",
                                modifier = Modifier.size(64.dp), // Tamanho do ícone
                                tint = Color(0xFF048cd4)
                            )
                        }
                        IconButton(
                            onClick = { promptSaveCsv() },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send, // Ícone de "exportar"
                                contentDescription = "Exportar para CSV",
                                modifier = Modifier.size(64.dp), // Tamanho do ícone
                                tint = Color(0xFF048cd4)
                            )
                        }
                    }
                }
            },
            content = { paddingValues ->
                // LazyColumn com padding
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Campo de texto para inserção do nome do evento
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextField(
                                value = eventText.value,
                                onValueChange = { eventText.value = it },
                                label = { Text("Nome do evento") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }

                    // Verifica se o histórico está vazio
                    if (qrCodeHistory.isEmpty()) {
                        // Mensagem quando não há QR Codes no histórico
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.emptybox),
                                        contentDescription = "Nenhum QR Code",
                                        modifier = Modifier
                                            .size(120.dp) // Ajuste do tamanho da imagem
                                    )

                                    Text(
                                        text = "Nenhum QR Code escaneado ainda",
                                        textAlign = TextAlign.Center,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        // Exibe os QR Codes lidos
                        items(qrCodeHistory) { qrCodeWithDate ->
                            val parts = qrCodeWithDate.split(" - ", limit = 2)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    if (parts.size == 2) {
                                        // Exibir o QR Code primeiro
                                        Text(
                                            text = parts[0], // QR Code
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        // Exibir a data e hora depois
                                        Text(
                                            text = parts[1], // Data e hora
                                            modifier = Modifier.padding(top = 4.dp),
                                            style = androidx.compose.ui.text.TextStyle(color = Color.Gray)
                                        )
                                    } else {
                                        // Caso algum dado não esteja corretamente formatado
                                        Text(text = qrCodeWithDate)
                                    }
                                }
                            }
                        }

                    }
                }
            }
        )
    }

}
