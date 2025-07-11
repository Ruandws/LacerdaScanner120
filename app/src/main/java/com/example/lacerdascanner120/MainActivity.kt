package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Variáveis de estado
    private var qrContent by mutableStateOf("")
    private var showDialog by mutableStateOf(false)
    private var qrCodeHistory = mutableStateListOf<QrCodeData>().toMutableStateList()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Top Bar
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
                containerColor = Color(0xFF048cd4),
                titleContentColor = Color.White
            ),
        )
    }

    // QR Code Scanner
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var barCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let {
            qrContent = it
            showDialog = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkCameraPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showCamera()
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(this@MainActivity, "Cancelado. Tente novamente.", Toast.LENGTH_SHORT).show()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

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

    // Salvar QR Code
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun salvarQrCode(
        qrContent: String?,
        codigo: String,
        nomePosto: String
    ) {
        if (qrContent == null) {
            Log.e("salvarQrCode", "qrContent is null")
            return
        }

        val currentDateTime = Calendar.getInstance().time
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDateTime)
        val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDateTime)

        obterLocalizacaoComTimeout { location ->
            val latitude = location?.latitude?.toString() ?: "N/A"
            val longitude = location?.longitude?.toString() ?: "N/A"

            val dadosQr = qrContent.split(",")
            val colaborador = dadosQr.getOrNull(0)?.trim() ?: "N/A"
            val matricula = dadosQr.getOrNull(1)?.trim() ?: "N/A"

            val qrCodeData = QrCodeData(
                codigo = codigo,
                nomePosto = nomePosto,
                colaborador = colaborador,
                matricula = matricula,
                data = formattedDate,
                hora = formattedTime,
                latitude = latitude,
                longitude = longitude
            )

            qrCodeHistory.add(qrCodeData)
        }
    }

    data class QrCodeData(
        val codigo: String,
        val nomePosto: String,
        val colaborador: String,
        val matricula: String,
        val data: String,
        val hora: String,
        val latitude: String,
        val longitude: String
    )

    // Localização
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestPermissionLauncherLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            obterLocalizacaoComTimeout {}
        } else {
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
                obterLocalizacaoComTimeout {}
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "É necessário permissão de localização para acessar sua localização.",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncherLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncherLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("MissingPermission")
    private fun obterLocalizacaoComTimeout(onLocationReceived: (Location?) -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = PRIORITY_HIGH_ACCURACY
            interval = 5000L
            fastestInterval = 1000L
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                onLocationReceived(location)
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // API Integration
    private fun exportToApi() {
        val nomeDoEvento = getNomeDoEvento()
        val supervisorName = getSupervisorName()
        if (nomeDoEvento.isEmpty()) return

        if (qrCodeHistory.isEmpty()) {
            Toast.makeText(this, "Não há dados para exportar!", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonData = createJsonData(nomeDoEvento, supervisorName)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonData.toString().toRequestBody(mediaType)

        // Substitua pela sua URL real
        val apiUrl = "https://sua-api.com/endpoint"

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        val loadingDialog = showLoadingDialog()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "Falha ao enviar dados: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    if (response.isSuccessful) {
                        handleSuccessfulExport()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Erro ao enviar dados: ${response.code} - ${response.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private fun handleSuccessfulExport() {
        Toast.makeText(
            this,
            "Dados enviados com sucesso!",
            Toast.LENGTH_SHORT
        ).show()
        qrCodeHistory.clear()
        val intent = Intent(this, EventNameActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createJsonData(nomeDoEvento: String, supervisorName: String): JSONObject {
        return JSONObject().apply {
            put("evento", nomeDoEvento)
            put("supervisor", supervisorName)
            put("data_hora_exportacao", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().time))

            val registrosArray = JSONArray()
            qrCodeHistory.forEach { qrCodeData ->
                registrosArray.put(JSONObject().apply {
                    put("codigo", qrCodeData.codigo)
                    put("nome_posto", qrCodeData.nomePosto)
                    put("colaborador", qrCodeData.colaborador)
                    put("matricula", qrCodeData.matricula)
                    put("data", qrCodeData.data)
                    put("hora", qrCodeData.hora)
                    put("latitude", qrCodeData.latitude)
                    put("longitude", qrCodeData.longitude)
                })
            }
            put("registros", registrosArray)
        }
    }

    private fun showLoadingDialog(): AlertDialog {
        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }

        return AlertDialog.Builder(this)
            .setView(progressBar)
            .setMessage("Enviando dados...")
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun promptSaveToApi() {
        if (qrCodeHistory.isEmpty()) {
            Toast.makeText(this, "Não há dados para exportar!", Toast.LENGTH_SHORT).show()
            return
        }

        val nomeDoEvento = getNomeDoEvento()
        if (nomeDoEvento.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Enviar dados")
            .setMessage("Deseja enviar ${qrCodeHistory.size} registros para o servidor?")
            .setPositiveButton("Enviar") { _, _ -> exportToApi() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun getNomeDoEvento(): String {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("NOME_EVENTO", "") ?: ""
    }

    private fun getSupervisorName(): String {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("NOME_SUPERVISOR", "") ?: ""
    }

    // UI Components
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
                    Button(
                        onClick = {
                            onSave(qrContent, codigo, nomePosto)
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
                            containerColor = Color(0xFF048cd4)
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Composable
    fun MainScreenMenuInferior(
        qrCodeHistory: List<QrCodeData>,
        onScanQrCode: () -> Unit,
        onClearHistory: () -> Unit
    ) {
        val context = LocalContext.current
        var showClearDialog by remember { mutableStateOf(false) }

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
                            containerColor = Color(0xFF048cd4), // Corrigido: removido o lambda
                            contentColor = Color.White
                        )
                    ) {
                        Text("Não")
                    }
                }
            )
        }

        Scaffold(
            topBar = { CustomTopBar() },
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { showClearDialog = true }) {
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
                        IconButton(onClick = { promptSaveToApi() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar para API",
                                tint = Color(0xFF048cd4),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            },
            content = { paddingValues ->
                QrCodeHistoryList(qrCodeHistory, paddingValues)
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

    @Composable
    private fun QrCodeHistoryList(
        qrCodeHistory: List<QrCodeData>,
        paddingValues: PaddingValues
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (qrCodeHistory.isEmpty()) {
                item {
                    EmptyStateView()
                }
            } else {
                items(qrCodeHistory) { qrCodeData ->
                    QrCodeItem(qrCodeData)
                }
            }
        }
    }

    @Composable
    private fun EmptyStateView() {
        Column(
            modifier = Modifier.fillMaxSize(), // Corrigido: substituído fillParentMaxSize por fillMaxSize
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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

    @Composable
    private fun QrCodeItem(qrCodeData: QrCodeData) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Colaborador: ${qrCodeData.colaborador}",
                    modifier = Modifier.alpha(0.5f)
                )
                Text(
                    text = "Matrícula: ${qrCodeData.matricula}",
                    modifier = Modifier.alpha(0.5f)
                )
                Row {
                    Text(
                        text = "Código: ${qrCodeData.codigo}",
                        modifier = Modifier.alpha(0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Posto: ${qrCodeData.nomePosto}",
                        modifier = Modifier.alpha(0.5f)
                    )
                }
                Row {
                    Text(
                        text = "Data: ${qrCodeData.data}",
                        modifier = Modifier.alpha(0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hora: ${qrCodeData.hora}",
                        modifier = Modifier.alpha(0.5f)
                    )
                }
                Text(
                    text = "Localização: ${qrCodeData.latitude}, ${qrCodeData.longitude}",
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermissionAndRequest()

        setContent {
            MainScreenMenuInferior(
                qrCodeHistory = qrCodeHistory,
                onScanQrCode = { showCamera() },
                onClearHistory = { qrCodeHistory.clear() }
            )
        }
    }

    // Helper functions
    fun filtrarCaracteres(texto: String): String {
        val regex = Regex("[^a-zA-Z0-9 ]")
        return regex.replace(texto, "")
    }

    private fun Color.Companion.fromHex(colorString: String): Color {
        return Color(android.graphics.Color.parseColor(colorString))
    }
}