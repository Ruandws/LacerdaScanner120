package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {

                                //------Action Bar----//
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

                            //-------Lógica do QrCode----//

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var barCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            // ...
        } else {
            val currentDateTime = Calendar.getInstance().time
            val formattedDate = SimpleDateFormat("dd/MM/yyyy").format(currentDateTime)
            val formattedTime = SimpleDateFormat("HH:mm:ss").format(currentDateTime)

            val qrCodeData = QrCodeData(result.contents, "$formattedDate, $formattedTime")

            // Obter a localização *e* processar o QR Code *depois* que a localização estiver disponível
            obterLocalizacaoComTimeout { location ->
                val latitude = location?.latitude?.toString() ?: "N/A"
                val longitude = location?.longitude?.toString() ?: "N/A"

                // Combinar os dados do QR Code com a localização *agora*
                val qrCodeWithDetails =
                    "${qrCodeData.content}, - ${qrCodeData.timestamp}, Lat: $latitude, Long: $longitude"
                qrCodeHistory.add(qrCodeWithDetails)
            }
        }
    }

    //Onde o App pede permissão pro sistema, pra usar a câmera.
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGrantted ->
        if (isGrantted) {
            showCamera()
        }
    }

    //Verifica se a permissão foi concedida. Se não, solicita permissão denovo ou exibe uma mensagem explicativa.
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    //Configurações da Câmera. Prompt de Exibição, Som etc.
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun showCamera() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Aponte para um QR Code válido")
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setOrientationLocked(false)

        barCodeLauncher.launch(options)
    }

             //-----------Lógica do Histórico--------//
    private var qrCodeHistory = mutableStateListOf<String>() // Lista reativa. Garante que a UI seja atualizada automaticamente
    data class QrCodeData(val content: String, val timestamp: String)// Classe para armazenar os dados do QR Code *temporariamente*

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Inicializa o FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Verifica e solicita permissão de localização
        checkLocationPermissionAndRequest()

        //Interface do APP
        setContent {
            //Responsável por setar a lista de componentes e suas respectivas ações. (da barra inferior.)
            MainScreen(
                qrCodeHistory = qrCodeHistory,
                onScanQrCode = { showCamera() },
                onClearHistory = { qrCodeHistory.clear() }
            )
        }
    }
                    //-------------Lógica da Localização ------------//

    //-------------Lógica da Localização ------------//
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    // Solicita permissão para acessar a localização
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestPermissionLauncherLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permissão concedida
            obterLocalizacaoComTimeout {}
        } else {
            // Permissão negada
            Toast.makeText(this, "Permissão de localização negada. O aplicativo não poderá acessar a localização.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkLocationPermissionAndRequest() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permissão já concedida
                obterLocalizacaoComTimeout {}
            }

            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Exibir diálogo explicando por que a permissão é necessária
                Toast.makeText(
                    this,
                    "É necessário permissão de localização para acessar sua localização.",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncherLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> {
                // Solicitar permissão
                requestPermissionLauncherLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Função para obter a última localização conhecida com timeout e tratamento de erros
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("MissingPermission")
    private fun obterLocalizacaoComTimeout(onLocationReceived: (Location?) -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000L // Tentar a cada 5 segundos
            fastestInterval = 1000L // Intervalo mínimo de 1 segundo
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                onLocationReceived(location)
                fusedLocationClient.removeLocationUpdates(this) // Parar de receber updates
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    // Função para iniciar a leitura do QR Code
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun iniciarLeituraQrCode() {
        barCodeLauncher.launch(null)
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

        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val nomeDoEvento = sharedPref.getString("NOME_EVENTO", "Evento Desconhecido") ?: "Evento Desconhecido"
        // Declarar fileName *antes* do bloco try
        val fileName = "${nomeDoEvento}.csv" // Nome do arquivo com extensão

        // Verifica se o nome do evento foi preenchido
        if (nomeDoEvento.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome do evento antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = qrCodeHistory.joinToString("\n") { qrCodeWithDate ->
            val parts = qrCodeWithDate.split(" - ", limit = 2)
            if (parts.size == 3) {
                "$nomeDoEvento, ${parts[0]}, ${parts[1]}, ${parts[2]}"
            } else {
                "$nomeDoEvento, $qrCodeWithDate"
            }
        }

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                // Não precisa mais criar fileName aqui, já foi criado antes

                // Escrever o cabeçalho e o conteúdo no arquivo
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("Evento, Funcionario, Matricula, Data, Hora, Latitude, Longitude\n")
                    writer.write(csvContent)
                }
            }
            // Usar fileName aqui, fora do bloco try
            Toast.makeText(this, "QR Codes exportados para $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            // ... (tratamento de erro igual)
        }
    }

    // Função para iniciar o seletor de arquivos para salvar o CSV (Obriga o usuário a dizer o Evento)
    private fun promptSaveCsv() {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val nomeDoEvento = sharedPref.getString("NOME_EVENTO", null)

        if (nomeDoEvento.isNullOrEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome do evento antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "${nomeDoEvento}.csv" // Nome do arquivo com extensão
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

private fun LocationManager.requestLocationUpdates(gpsProvider: String, l: Long, fl: Float, mainActivity: MainActivity) {

}
