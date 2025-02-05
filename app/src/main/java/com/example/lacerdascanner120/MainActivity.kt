package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {

    //Variável Global pro campo de texto
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

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

    // Declaração do barcode launcher fora de qualquer função ou Composable
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var barCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this@MainActivity, "Cancelado. Tente novamente.", Toast.LENGTH_SHORT).show()
        } else {
            // Antes de processar o QR Code, garante que a localização foi obtida
            getLastKnownLocation()

            val currentDateTime = Calendar.getInstance().time // Pegando hora atual
            val formattedDate = SimpleDateFormat("dd/MM/yyyy").format(currentDateTime) // Organizando data
            val formattedTime = SimpleDateFormat("HH:mm:ss").format(currentDateTime) // Organizando hora

            // Verifica se a localização foi obtida
            val latitude = currentLocation?.latitude?.toString() ?: "N/A"
            val longitude = currentLocation?.longitude?.toString() ?: "N/A"

            val qrCodeWithDetails = "${result.contents}, - $formattedDate, $formattedTime, Lat: $latitude, Long: $longitude"
            qrCodeHistory.add(qrCodeWithDetails)
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager // É quem obtem a Localização do sistema

        // Verifica e solicita permissão de localização
        checkLocationPermissionAndRequest() // Possibilita rastreamento em tempo real, devido a atualizações constantes

        // Inicializa o FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

    // Solicita permissão para acessar a localização
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestPermissionLauncherLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // A permissão foi concedida, você pode começar a acessar a localização
            requestLocationUpdates()
        } else {
            // A permissão foi negada
            Toast.makeText(this, "Permissão de localização negada. A aplicação não será precisa.", Toast.LENGTH_SHORT).show()
        }
    }

    // Verifica se a permissão já foi concedida, caso contrário, solicita
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkLocationPermissionAndRequest() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Se a permissão já foi concedida, solicita a localização
            requestLocationUpdates()
        } else {
            // Caso contrário, solicita a permissão ao usuário
            requestPermissionLauncherLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Função para obter a última localização conhecida
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("MissingPermission") // Permite acessar a localização sem a necessidade de checar novamente
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = location
                // Aqui você pode fazer algo com a localização, como exibir no UI ou armazenar
                Toast.makeText(
                    this@MainActivity,
                    "Última localização: Lat: ${location.latitude}, Long: ${location.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para solicitar atualizações de localização
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L, // Tempo mínimo entre atualizações (1 segundo)
            10f,   // Distância mínima entre atualizações (10 metros)
            this   // Implementação do LocationListener na própria Activity
        )
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
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("Evento, Funcionario, Matricula, Data, Hora, Latitude, Longitude\n") // Cabeçalho atualizado
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
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val nomeDoEvento = sharedPref.getString("NOME_EVENTO", null)

        if (nomeDoEvento.isNullOrEmpty()) {
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
