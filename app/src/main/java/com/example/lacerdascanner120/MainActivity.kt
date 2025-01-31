package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {
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

            // Verifica se a permissão de localização foi concedida
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Obtém a localização atual
                getCurrentLocation(this) { location ->
                    // Adiciona a localização ao QR Code
                    val qrCodeWithLocation = "$formattedDate, $formattedTime - ${result.contents} - Localização: $location"
                    qrCodeHistory.add(qrCodeWithLocation)
                }
            } else {
                // Se a permissão de localização não for concedida, adiciona apenas o QR Code
                val qrCodeWithDate = "$formattedDate, $formattedTime - ${result.contents}"
                qrCodeHistory.add(qrCodeWithDate)
            }
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
    override fun onCreate(savedInstanceState: Bundle?) { //Declaração das variáveis globais
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                qrCodeHistory = qrCodeHistory,
                onScanQrCode = { showCamera() },
                onClearHistory = { qrCodeHistory.clear() }
            )
        }
    }

    //----------Função para atribuir localização ao qr code-------//
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, callback: (String) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    callback("${location.latitude}, ${location.longitude}")
                } ?: callback("Localização não disponível")
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
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
        val csvContent = qrCodeHistory.joinToString("\n") { it }

        try {
            // Abre o OutputStream para o arquivo escolhido pelo usuário
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("Data, Hora, NomeFuncionario, Matricula\n")           //Aqui separaça as informações entre "," e colunas na planilha
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



    //Barra inferior de opções
    // Barra inferior de opções
    @Composable
    fun MainScreen(
        qrCodeHistory: List<String>,
        onScanQrCode: () -> Unit,
        onClearHistory: () -> Unit
    ) {
        val context = LocalContext.current  // Acessa o contexto local

        Scaffold(
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (qrCodeHistory.isEmpty()) {
                        // Mensagem de histórico vazio
                        item {
                            Text(
                                text = "Nenhum QR Code escaneado ainda.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    } else {
                        // Exibe os QR Codes lidos
                        items(qrCodeHistory) { qrCodeWithDate ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                // Usando Column para exibir o conteúdo do QR e a data/hora em linhas separadas
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Exibe o QR Code
                                    Text(
                                        text = qrCodeWithDate.split(" - ")[1], // Exibe o conteúdo do QR Code (parte após " - ")
                                        modifier = Modifier.padding(bottom = 4.dp) // Espaçamento abaixo do QR Code
                                    )

                                    // Exibe a data e hora
                                    Text(
                                        text = qrCodeWithDate.split(" - ")[0], // Exibe a data e hora (parte antes de " - ")
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


