package com.example.lacerdascanner120

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.example.lacerdascanner120.ui.theme.LacerdaScanner120Theme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    //----------Lógica do QRCode---------//
        private var textResult = mutableStateOf("")

        private var barCodeLauncher = registerForActivityResult(ScanContract()){
            result ->
            if  (result.contents ==null)    {
                Toast.makeText(this@MainActivity, "Cancelado. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
            else{
                qrCodeHistory.add(result.contents)  // Adiciona o QR Code à lista
            }
        }

        private fun showCamera(){
            val options= ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Aponte para um QR Code válido")
            options.setCameraId(0)
            options.setBeepEnabled(false)
            options.setOrientationLocked(false)

            barCodeLauncher.launch(options)
        }

        private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){
            isGrantted ->
            if (isGrantted){
                showCamera()
            }
        }

        private fun checkCameraPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED){
            showCamera()
        }
        else if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
            Toast.makeText(this@MainActivity, "Cancelado. Tente novamente.", Toast.LENGTH_SHORT).show()
        }
        else{
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
}

    //Barra inferior de opções

private fun exportToCSV(qrCodeHistory: List<String>, context: Context) {
    // Cria o conteúdo CSV
    val csvContent = qrCodeHistory.joinToString("\n") { it }

    // Verifica se o diretório de documentos existe, caso contrário, cria
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (directory != null) {
        val file = File(directory, "qr_codes.csv")

        try {
            // Cria o arquivo e escreve no conteúdo
            val outputStream = FileOutputStream(file)
            val writer = OutputStreamWriter(outputStream)
            writer.write(csvContent)
            writer.close()

            // Exibe uma mensagem de sucesso
            Toast.makeText(context, "QR Codes exportados para ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao exportar para CSV", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Falha ao acessar o diretório de documentos", Toast.LENGTH_SHORT).show()
    }
}
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
                        Button(
                            onClick = onClearHistory,
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) {
                            Text("Limpar Histórico")
                        }
                        Button(
                            onClick = onScanQrCode,
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) {
                            Text("Escanear QR Code")
                        }
                        Button(
                            onClick = { exportToCSV(qrCodeHistory, context) },
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) {
                            Text("Exportar para CSV")
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
                        items(qrCodeHistory) { qrCode ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),

                            ) {
                                Text(
                                    text = qrCode,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }



