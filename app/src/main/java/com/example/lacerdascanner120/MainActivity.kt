package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {

    //Variáveis
    val eventText = mutableStateOf("")
    private var location: Location? = null


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
            val currentDateTime = Calendar.getInstance().time // Pegando hora atual
            val formattedDate = SimpleDateFormat("dd/MM/yyyy").format(currentDateTime) // Organizando data
            val formattedTime = SimpleDateFormat("HH:mm:ss").format(currentDateTime) // Organizando hora

            val qrCodeWithDate = "$formattedDate, $formattedTime -  ${result.contents}"

            // Chama a função de adicionar QR Code com ou sem localização
            location?.let {
                addQRCodeWithLocation(it.latitude, it.longitude, qrCodeWithDate) // Adiciona o QR Code com localização
            } ?: run {
                addQRCodeWithLocation(0.0, 0.0, qrCodeWithDate) // Adiciona o QR Code sem localização, usando valores default
            }
        }
    }

    // Função para adicionar QR Code com localização
    private fun addQRCodeWithLocation(latitude: Double, longitude: Double, qrCodeContent: String) {
        val qrCodeWithLocation = "$qrCodeContent, Latitude: $latitude, Longitude: $longitude"
        qrCodeHistory.add(qrCodeWithLocation) // Adiciona o QR Code com as informações de localização no histórico
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

    // FusedLocationProviderClient - Classe reservada.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // Função de inicialização do FusedLocationProviderClient
    private fun initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    // Função para solicitar permissão de localização
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Se a permissão for concedida, obtemos a localização
            getLocation()
        } else {
            // Se a permissão for negada, mostramos uma mensagem de aviso
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para verificar se a permissão de localização foi concedida
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Se a permissão foi concedida, obtemos a localização
                getLocation()
            }
            else -> {
                // Caso a permissão não tenha sido concedida, solicitamos a permissão
                requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Função para obter a localização atual
    private fun getLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    location = it // Atualiza a variável de localização com as coordenadas obtidas
                } ?: run {
                    // Se a localização não foi obtida, você pode lidar com isso de outra forma
                    // Como adicionar as coordenadas "0.0, 0.0" ou uma mensagem de erro
                    location = Location("").apply {
                        latitude = 0.0
                        longitude = 0.0
                    }
                }
            }
        } catch (e: SecurityException) {
            // Caso haja um erro de permissão (por exemplo, o usuário negou), mostramos um aviso
            Toast.makeText(this, "Erro ao acessar a localização. Permissão negada.", Toast.LENGTH_SHORT).show()
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
                onClearHistory = { qrCodeHistory.clear()

                }
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

    private fun exportToCSV(uri: Uri) {
        // Caso o usuário esqueça de por o evento, ele não poderá exportar o arquivo.
        if (eventText.value.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome do evento.", Toast.LENGTH_SHORT).show()
            return  // Interrompe a execução se o nome do evento não for preenchido
        }

        // Inclui o texto do evento na linha do QR Code
        val csvContent = qrCodeHistory.joinToString("\n") { qrCode ->
            // Verifica se o local foi informado, se não, insere "Sem local"
            val localInfo = if (eventText.value.isEmpty()) "Sem local" else eventText.value

            // Inclui o QR Code, evento e local separados por vírgula
            "$qrCode, ${eventText.value}, $localInfo"
        }

        try {
            // Abre o OutputStream para o arquivo escolhido pelo usuário
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("Funcionário, Matrícula , LocalEvento, Latitude, Longitude,\n")  // Adiciona a coluna "Local" no cabeçalho
                    writer.write(csvContent)
                }
            }

            // Exibe uma mensagem de sucesso
            Toast.makeText(this, "QR Codes exportados para $uri", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao exportar para CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para iniciar o seletor de arquivos para salvar o CSV
    private fun promptSaveCsv() {
        // Nome do arquivo desejado, que será mostrado ao usuário
        val fileName = "qr_codes.csv"  // Nome do arquivo sugerido

        // Lança o seletor de arquivo para o usuário escolher onde salvar
        createFileLauncher.launch(fileName)
    }




    //---------Barra inferior de opções--------//
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
                                imageVector = Icons.Default.Send, // Ícone de "exportar"
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                // Exibe o conteúdo do QR Code com data/hora
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = qrCodeWithDate.split(" - ")[1], // Exibe o conteúdo do QR Code (parte após " - ")
                                        modifier = Modifier.padding(bottom = 4.dp) // Espaçamento abaixo do QR Code
                                    )

                                    Text(
                                        text = qrCodeWithDate.split(",")[0], // Exibe a data e hora (parte antes de " - ")
                                        modifier = Modifier.padding(top = 4.dp), // Espaçamento acima da data
                                        style = androidx.compose.ui.text.TextStyle(color = Color.Gray)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

}
