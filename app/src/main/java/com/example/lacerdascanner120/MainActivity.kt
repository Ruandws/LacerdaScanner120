package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    //------Leitura do QRcode e Ativação do Popup-----//
    private var qrContent by mutableStateOf("")
    private var showDialog by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var barCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let {
            qrContent = it
            showDialog = true // Exibe o popup para inserir dados extras
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

    //------Leitura do QRcode e Ativação do Popup-----//

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun salvarQrCode(
        qrContent: String?,
        codigo: String,
        nomePosto: String
    ) { // qrContent como nullable
        if (qrContent == null) {
            // Tratar o caso em que qrContent é nulo
            Log.e("salvarQrCode", "qrContent is null")
            return // Sai da função
        }

        val currentDateTime = Calendar.getInstance().time
        val formattedDate =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDateTime)
        val formattedTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDateTime)

        obterLocalizacaoComTimeout { location ->
            val latitude = location?.latitude?.toString() ?: "N/A"
            val longitude = location?.longitude?.toString() ?: "N/A"

            val qrCodeData = QrCodeData( // Crie um objeto QrCodeData
                codigo = codigo,
                nomePosto = nomePosto,
                qrContent = qrContent, // Usa qrContent
                data = formattedDate,
                hora = formattedTime,
                latitude = latitude,
                longitude = longitude
            )

            qrCodeHistory.add(qrCodeData) // Adicione o objeto QrCodeData à lista
        }
    }


    //-----------Lógica do Histórico--------//
    private var qrCodeHistory =
        mutableStateListOf<QrCodeData>().toMutableStateList()// Lista reativa. Garante que a UI seja atualizada automaticamente

    data class QrCodeData(
        val codigo: String,
        val nomePosto: String,
        val qrContent: String?,
        val data: String,
        val hora: String,
        val latitude: String,
        val longitude: String
    )

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
            MainScreenMenuInferior(
                qrCodeHistory = qrCodeHistory,
                onScanQrCode = { showCamera() },
                onClearHistory = { qrCodeHistory.clear() }
            )
        }
    }

    //-------------Lógica da Localização ------------//
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

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
            Toast.makeText(
                this,
                "Permissão de localização negada. O aplicativo não poderá acessar a localização.",
                Toast.LENGTH_SHORT
            ).show()
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
            priority = PRIORITY_HIGH_ACCURACY
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

    //----------Lógica Popup--------//
    //Lógica da inserção de dados do popup
    @Composable
    fun PopupInserirDados(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        qrContent: String?,
        onSave: (String?, String, String) -> Unit
    ) {
        var codigo by remember { mutableStateOf("") }
        var nomePosto by remember { mutableStateOf("") }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Insira o código e o Nome do Posto") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = filtrarCaracteres(it) },
                            label = { Text("Código") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nomePosto,
                            onValueChange = { nomePosto = filtrarCaracteres(it) },
                            label = { Text("Nome do Posto") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onSave(qrContent, codigo, nomePosto) // Passa os dados para serem salvos
                        onDismiss()
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancelar")
                    }
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
        val nomeDoEvento = getNomeDoEvento()
        if (nomeDoEvento.isEmpty()) return

        val csvContent = qrCodeHistory.joinToString("\n") { qrCodeData ->
            "${nomeDoEvento}, ${qrCodeData.codigo}, ${qrCodeData.nomePosto}, ${qrCodeData.qrContent ?: "N/A"}, ${qrCodeData.data}, ${qrCodeData.hora}, ${qrCodeData.latitude}, ${qrCodeData.longitude}"
        }

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("EVENTO, CODIGO, NOME DO POSTO, COLABORADOR, MATRICULA, DATA, HORA, LATITUDE, LONGITUDE\n")
                    writer.write(csvContent)
                    qrCodeHistory.clear()
                    val intent = Intent(this, EventNameActivity::class.java)
                    startActivity(intent)
                    finish() // Fecha a MainActivity
                }
            }
            Toast.makeText(this, "QR Codes exportados para ${nomeDoEvento}.csv", Toast.LENGTH_SHORT)
                .show()
        } catch (e: IOException) {
            Log.e("exportToCSV", "Erro ao exportar CSV: ${e.message}")
            Toast.makeText(
                this,
                "Erro ao exportar CSV. Verifique o log para mais detalhes.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: SecurityException) {
            Log.e("exportToCSV", "Erro de permissão ao exportar CSV: ${e.message}")
            Toast.makeText(this, "Erro de permissão ao exportar CSV.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("exportToCSV", "Erro inesperado ao exportar CSV: ${e.message}")
            Toast.makeText(this, "Erro inesperado ao exportar CSV.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptSaveCsv() {
        //Impedir que o usuário exporte sem ter lido nada
        if (qrCodeHistory.isEmpty()) {
            Toast.makeText(this, "Não há dados para exportar!", Toast.LENGTH_SHORT).show()
            return
        }

        val nomeDoEvento = getNomeDoEvento()
        if (nomeDoEvento.isEmpty()) return
        createFileLauncher.launch("${nomeDoEvento}.csv")
    }

    private fun getNomeDoEvento(): String {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val nomeDoEvento = sharedPref.getString("NOME_EVENTO", "") ?: ""
        if (nomeDoEvento.isEmpty()) {
            Toast.makeText(
                this,
                "Por favor, insira o nome do evento antes de exportar.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return nomeDoEvento
    }

    //---------------Barra Inferior de Opções-----------//
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Composable
    fun MainScreenMenuInferior(
        qrCodeHistory: List<QrCodeData>,
        onScanQrCode: () -> Unit,
        onClearHistory: () -> Unit
    ) { //Alerta de confirmação ao tentar apagar histórico.
        val context = LocalContext.current
        var showClearDialog by remember { mutableStateOf(false) } // Estado para controlar a exibição do diálogo

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Limpar Histórico") },
                text = { Text("Tem certeza que deseja limpar o histórico?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sim")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showClearDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Não")
                    }
                }
            )
        }
        //Funcionalidades do Menu
        Scaffold(
            topBar = { CustomTopBar() },
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { showClearDialog = true }) { // Exibe o diálogo ao clicar
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Limpar Histórico",
                                tint = Color(0xFF048cd4),
                                modifier = Modifier.size(48.dp)

                            )
                        }
                        IconButton(onClick = onScanQrCode) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Escanear QR Code",
                                tint = Color(0xFF048cd4),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = { promptSaveCsv() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Exportar para CSV",
                                tint = Color(0xFF048cd4),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            },
            content = { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (qrCodeHistory.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                verticalArrangement = Arrangement.Center, // Centraliza verticalmente
                                horizontalAlignment = Alignment.CenterHorizontally // Centraliza horizontalmente
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.emptybox),
                                    contentDescription = "Nenhum QR Code",
                                    modifier = Modifier.size(120.dp)
                                )
                                Text(
                                    text = "Nenhum QR Code escaneado ainda",
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(qrCodeHistory) { qrCodeData ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${qrCodeData.qrContent ?: "N/A"}",
                                        fontWeight = FontWeight.Bold //
                                    )
                                    Row {
                                        Text(
                                            text = "Código: ${qrCodeData.codigo}",
                                            modifier = Modifier.alpha(0.5f) // Define a opacidade para 50%

                                        )
                                        Spacer(modifier = Modifier.width(8.dp)) // Espaço entre os componentes
                                        Text(
                                            text = "Posto: ${qrCodeData.nomePosto}",
                                            modifier = Modifier.alpha(0.5f) // Define a opacidade para 50%
                                        )
                                    }
                                    Row {
                                        Text(
                                            text = "Data: ${qrCodeData.data}",
                                            modifier = Modifier.alpha(0.5f) // Define a opacidade para 50%
                                        )
                                        Spacer(modifier = Modifier.width(8.dp)) // Espaço entre os componentes
                                        Text(
                                            text = "Hora: ${qrCodeData.hora}",
                                            modifier = Modifier.alpha(0.5f) // Define a opacidade para 50%
                                        )
                                    }
                                    Text(
                                        text = "Localização: ${qrCodeData.latitude}, ${qrCodeData.longitude}",
                                        modifier = Modifier.alpha(0.5f) // Define a opacidade para 50%
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
        PopupInserirDados(
            showDialog = showDialog,
            onDismiss = { showDialog = false },
            qrContent = qrContent,
            onSave = { qrContent, codigo, nomePosto ->
                salvarQrCode(qrContent, codigo, nomePosto)
                showDialog = false
            }
        )
    }
                                                            //------Funcionalidades Extras--------//
    //Filtro de Caracteres Especiais
    fun filtrarCaracteres(texto: String): String {
        val regex = Regex("[^a-zA-Z0-9 ]") // Permite letras, números e espaços
        return regex.replace(texto, "")
    }
    //Extensão para converter Hexadecimal em cor
    private fun Color.Companion.fromHex(colorString: String): Color {
        return Color(android.graphics.Color.parseColor(colorString))
    }
}