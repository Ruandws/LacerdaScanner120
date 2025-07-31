package com.example.lacerdascanner120

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.lacerdascanner120.ui.theme.BlueMainColor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ATENÇÃO: A cor BlueMainColor agora é declarada em CsvDataLoader.kt
// Remova qualquer declaração duplicada aqui!

class MainActivity : ComponentActivity() {

    private var contratoName: String = ""
    private var anelName: String = ""
    private var supervisorName: String = ""

    // Estados para a seleção de posto após o scan
    private var showPostoSelectionDialog by mutableStateOf(false)
    private var availablePostos: List<DataModels.Posto> = emptyList() // Postos filtrados pelo anel atual
    private var qrContentAfterScan by mutableStateOf<String?>(null) // Conteúdo do QR Code recém-escaneado
    private var selectedPostoFromDialog: DataModels.Posto? by mutableStateOf(null) // Posto selecionado na caixa de diálogo

    // Listas completas de todos os postos e anéis carregados do CSV (inicializadas em onCreate)
    private var allPostos: List<DataModels.Posto> = emptyList()
    private var allAneis: List<DataModels.Anel> = emptyList()

    // Estados para o diálogo de detalhes do cartão
    private var showCardDetailsDialog by mutableStateOf(false)
    private var selectedQrCodeDataForDetails: QrCodeData? by mutableStateOf(null)

    // Estados para o diálogo de confirmação de exclusão
    private var showDeleteConfirmationDialog by mutableStateOf(false)
    private var qrCodeDataToDelete: QrCodeData? by mutableStateOf(null)

    // A lista que armazena o histórico dos QR Codes escaneados
    private var qrCodeHistory = mutableStateListOf<QrCodeData>()

    // Classe de dados para representar um item de QR Code escaneado
    data class QrCodeData(
        val contrato: String,
        val anel: String,
        val posto: String,
        val colaborador: String,
        val matricula: String,
        val supervisor: String,
        val data: String,
        val hora: String,
        val latitude: String,
        val longitude: String
    )

    // Inicialização do FusedLocationProviderClient e LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

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
                containerColor = BlueMainColor, // Usando a cor centralizada
                titleContentColor = Color.White
            ),
        )
    }

    // Launcher para o scanner de código de barras
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var barCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let {
            qrContentAfterScan = it
            // Encontra o anel atual com base no nome do anel selecionado na EventNameActivity
            val currentAnel = allAneis.firstOrNull { it.nome.equals(anelName, ignoreCase = true) }
            if (currentAnel != null) {
                // Filtra os postos disponíveis com base no ID do anel atual
                availablePostos = allPostos.filter { it.anelId == currentAnel.id }
                if (availablePostos.isNotEmpty()) {
                    showPostoSelectionDialog = true // Mostra o diálogo de seleção de posto
                } else {
                    Toast.makeText(this, "Nenhum posto encontrado para o anel '$anelName'.", Toast.LENGTH_LONG).show()
                    qrContentAfterScan = null // Limpa o conteúdo se não houver postos
                }
            } else {
                Toast.makeText(this, "Anel '$anelName' não encontrado. Verifique os dados ou o CSV de anéis.", Toast.LENGTH_LONG).show()
                qrContentAfterScan = null
            }
        }
    }

    // Launcher para solicitação de permissão de câmera
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> // Nome da variável mudado para isGranted para clareza
        if (isGranted) { // Mudado para isGranted
            showCamera()
        } else {
            Toast.makeText(this@MainActivity, "Permissão de câmera negada. Não é possível escanear QR Codes.", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para verificar e solicitar permissão da câmera
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkCameraPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showCamera()
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(this@MainActivity, "Permissão de câmera necessária para escanear QR Codes.", Toast.LENGTH_SHORT).show()
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) // Tenta solicitar novamente
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) // Solicita a permissão
        }
    }

    // Função para iniciar o scanner da câmera
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun showCamera() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Aponte para um QR Code válido")
        options.setCameraId(0) // Câmera traseira
        options.setBeepEnabled(false) // Desabilita o beep
        options.setOrientationLocked(false) // Permite rotação

        barCodeLauncher.launch(options)
    }

    // Função para salvar os dados do QR Code no histórico
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun salvarQrCodeData(
        qrContent: String?,
        selectedPosto: DataModels.Posto
    ) {
        if (qrContent == null) {
            Log.e("salvarQrCodeData", "qrContent is null")
            return
        }

        val currentDateTime = Calendar.getInstance().time
        val formattedDate =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDateTime)
        val formattedTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDateTime)

        obterLocalizacaoComTimeout { location ->
            val latitude = location?.latitude?.toString() ?: "N/A"
            val longitude = location?.longitude?.toString() ?: "N/A"

            val dadosQr = qrContent.split(",")
            val colaborador = dadosQr.getOrNull(0)?.trim() ?: "N/A"
            val matricula = dadosQr.getOrNull(1)?.trim() ?: "N/A"

            val qrCodeData = QrCodeData(
                contrato = contratoName,
                anel = anelName,
                posto = selectedPosto.nome, // Usando o nome do posto selecionado no diálogo
                colaborador = colaborador,
                matricula = matricula,
                supervisor = supervisorName,
                data = formattedDate,
                hora = formattedTime,
                latitude = latitude,
                longitude = longitude
            )
            qrCodeHistory.add(qrCodeData)
            Toast.makeText(this, "QR Code salvo com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    // Método onCreate da Activity
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tentar obter os dados passados via Intent
        contratoName = intent.getStringExtra("EXTRA_CONTRATO_NAME") ?: ""
        anelName = intent.getStringExtra("EXTRA_ANEL_NAME") ?: ""
        supervisorName = intent.getStringExtra("EXTRA_SUPERVISOR_NAME") ?: ""

        // Se os dados não vierem da Intent (primeira inicialização após crash ou reinício)
        if (contratoName.isEmpty() || anelName.isEmpty() || supervisorName.isEmpty()) {
            val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            contratoName = sharedPref.getString("NOME_CONTRATO", "") ?: ""
            anelName = sharedPref.getString("NOME_ANEL", "") ?: ""
            supervisorName = sharedPref.getString("NOME_SUPERVISOR", "") ?: ""
        }

        // Lógica de verificação para redirecionar se os dados iniciais estiverem faltando
        // O posto não é mais um dado inicial obrigatório para navegar
        if (contratoName.isEmpty() || anelName.isEmpty() || supervisorName.isEmpty()) {
            Toast.makeText(this, "Dados do evento não encontrados. Reinicie o processo de criação do evento.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, EventNameActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Carregar todas as listas de dados do CSV usando CsvDataLoader
        // Estas listas são importantes para o filtro de postos baseado no anel
        allAneis = CsvDataLoader.loadAneisFromCsv(this)
        allPostos = CsvDataLoader.loadPostosFromCsv(this)


        // Inicialização do serviço de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermissionAndRequest() // Verifica e solicita permissão de localização

        setContent {
            MainScreenMenuInferior(
                qrCodeHistory = qrCodeHistory,
                onScanQrCode = { checkCameraPermission(this) },
                onClearHistory = { qrCodeHistory.clear() }
            )
        }
    }

    // Launcher para solicitação de permissão de localização
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val requestPermissionLauncherLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            obterLocalizacaoComTimeout {} // Tenta obter a localização se a permissão for concedida
        } else {
            Toast.makeText(
                this,
                "Permissão de localização negada. O aplicativo não poderá acessar a localização.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Função para verificar e solicitar permissão de localização
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

    // Função para obter a localização do dispositivo com um timeout implícito
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("MissingPermission") // Anotação para ignorar a verificação de permissão, pois ela é feita antes
    private fun obterLocalizacaoComTimeout(onLocationReceived: (Location?) -> Unit) {
        val locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 5000L) // Intervalo de 5 segundos
            .setWaitForAccurateLocation(false) // Não espera por uma localização super precisa
            .setMinUpdateIntervalMillis(1000L) // Intervalo mínimo de atualização de 1 segundo
            .setMaxUpdateDelayMillis(2000L) // Atraso máximo de 2 segundos para coalescer atualizações
            .build()


        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                onLocationReceived(location)
                fusedLocationClient.removeLocationUpdates(this) // Remove as atualizações após receber uma localização
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Composable para o diálogo de seleção de posto
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PostoSelectionDialog(
        showDialog: Boolean,
        postos: List<DataModels.Posto>, // Lista de postos a serem exibidos
        onPostoSelected: (DataModels.Posto) -> Unit, // Callback quando um posto é selecionado
        onDismiss: () -> Unit // Callback quando o diálogo é descartado
    ) {
        val context = LocalContext.current

        if (showDialog) {
            var selectedOptionText by remember { mutableStateOf(postos.firstOrNull()?.nome ?: "") }
            var expanded by remember { mutableStateOf(false) }

            // Atualiza o texto selecionado e o posto quando a lista de postos muda
            LaunchedEffect(postos) {
                if (postos.isNotEmpty()) {
                    selectedOptionText = postos.first().nome
                    selectedPostoFromDialog = postos.first() // Define o primeiro como padrão
                } else {
                    selectedOptionText = ""
                    selectedPostoFromDialog = null
                }
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Selecione o Posto") },
                text = {
                    Column {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedOptionText,
                                onValueChange = {}, // Campo somente leitura
                                readOnly = true,
                                label = { Text("Posto") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                postos.forEach { posto: DataModels.Posto -> // Tipagem explícita aqui
                                    DropdownMenuItem(
                                        text = { Text(posto.nome) },
                                        onClick = {
                                            selectedOptionText = posto.nome
                                            selectedPostoFromDialog = posto
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedPostoFromDialog?.let {
                                onPostoSelected(it)
                            } ?: run {
                                Toast.makeText(context, "Por favor, selecione um posto.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueMainColor)
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueMainColor)
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    // Composable para o diálogo de detalhes do cartão
    @Composable
    fun CardDetailsDialog(
        showDialog: Boolean,
        qrCodeData: QrCodeData?,
        onDismiss: () -> Unit
    ) {
        if (showDialog && qrCodeData != null) {
            AlertDialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false), // Permite controlar a largura
                modifier = Modifier
                    .width(IntrinsicSize.Min) // Largura mínima para o conteúdo
                    .height(IntrinsicSize.Min) // Altura mínima para o conteúdo
                    .padding(horizontal = 32.dp, vertical = 64.dp) // Padding ao redor
                ,
                title = { Text("Mais detalhes...") },
                text = {
                    Column {
                        Text("Posto: ", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(qrCodeData.posto)
                        Spacer(Modifier.height(4.dp))

                        Text("Latitude: ", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(qrCodeData.latitude)
                        Spacer(Modifier.height(4.dp))

                        Text("Longitude: ", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(qrCodeData.longitude)
                        Spacer(Modifier.height(4.dp))

                        Text("Data: ", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(qrCodeData.data)
                        Spacer(Modifier.height(4.dp))

                        Text("Hora: ", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal))
                        Text(qrCodeData.hora)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueMainColor)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }

    // Composable para o diálogo de confirmação de exclusão
    @Composable
    fun DeleteConfirmationDialog(
        showDialog: Boolean,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Confirmar Exclusão") },
                text = { Text("Tem certeza que deseja excluir este item?") },
                confirmButton = {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueMainColor)
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueMainColor)
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    // Função para exportar os dados do histórico para um arquivo Excel
    private fun exportToExcel(uri: Uri) {
        // Usa a combinação Contrato - Anel para o nome do arquivo, já que Posto não é mais inicial
        val eventoDetalhes = "$contratoName - $anelName"

        if (qrCodeHistory.isEmpty()) {
            Toast.makeText(this, "Não há dados para exportar!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Roteiro de Ronda")

                // Adiciona o logo
                val logo = BitmapFactory.decodeResource(resources, R.drawable.bsblogo)
                val byteArrayOutputStream = ByteArrayOutputStream()
                logo.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val bytes = byteArrayOutputStream.toByteArray()
                val pictureIdx = workbook.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_PNG)
                val helper = workbook.creationHelper
                val anchor = XSSFClientAnchor(0, 0, 0, 0, 0, 0, 2, 3) // Posição do logo
                sheet.createDrawingPatriarch().createPicture(anchor, pictureIdx)

                // Informações da empresa
                sheet.createRow(1).createCell(1).setCellValue("Brasília Segurança S/A")
                sheet.createRow(2).createCell(1).setCellValue("SIA SUL TRECHO 06 BLOCO A LOTES 5 15")
                sheet.createRow(3).createCell(1).setCellValue("Brasília - DF")
                sheet.createRow(1).createCell(2).setCellValue("CNPJ: 02.730.521/0001-20")
                sheet.createRow(2).createCell(2).setCellValue("CEP: 71205060")
                sheet.createRow(3).createCell(2).setCellValue("Tel: 61 3247 4777")

                // Título do relatório
                val titleRow = sheet.createRow(4)
                val titleCell = titleRow.createCell(0)
                titleCell.setCellValue("-----------------------\n ROTEIRO DE RONDA \n-------------------------")
                val titleStyle = workbook.createCellStyle()
                titleStyle.alignment = HorizontalAlignment.CENTER
                titleStyle.verticalAlignment = VerticalAlignment.CENTER
                titleCell.cellStyle = titleStyle

                // Detalhes do Evento (Contrato, Anel, Supervisor) - Posto removido daqui
                sheet.createRow(5).createCell(0).setCellValue("Contrato: $contratoName")
                sheet.createRow(5).createCell(1).setCellValue("Anel: $anelName")
                sheet.createRow(6).createCell(0).setCellValue("Supervisor: $supervisorName") // Ajustado para linha 6, coluna 0

                // Cabeçalhos da tabela de dados
                val headerRow = sheet.createRow(7)
                val headers = arrayOf("COLABORADOR", "MATRICULA", "POSTO", "DATA", "HORA", "LATITUDE", "LONGITUDE")
                headers.forEachIndexed { index, header ->
                    headerRow.createCell(index).setCellValue(header)
                }

                // Preenche os dados do histórico
                qrCodeHistory.forEachIndexed { rowIndex, qrCodeData ->
                    val dataRow = sheet.createRow(rowIndex + 8) // +8 para começar após cabeçalhos
                    dataRow.createCell(0).setCellValue(qrCodeData.colaborador)
                    dataRow.createCell(1).setCellValue(qrCodeData.matricula)
                    dataRow.createCell(2).setCellValue(qrCodeData.posto)
                    dataRow.createCell(3).setCellValue(qrCodeData.data)
                    dataRow.createCell(4).setCellValue(qrCodeData.hora)
                    dataRow.createCell(5).setCellValue(qrCodeData.latitude)
                    dataRow.createCell(6).setCellValue(qrCodeData.longitude)
                }

                workbook.write(outputStream) // Escreve o workbook no OutputStream
                Toast.makeText(this, "Ronda exportada para ${eventoDetalhes}.xlsx", Toast.LENGTH_SHORT).show()
                qrCodeHistory.clear() // Limpa o histórico após a exportação
                val intent = Intent(this, EventNameActivity::class.java)
                startActivity(intent) // Retorna para a tela de evento
                finish() // Finaliza a MainActivity
            }
        } catch (e: IOException) {
            Log.e("exportToExcel", "Erro ao exportar Excel: ${e.message}")
            Toast.makeText(this, "Erro ao exportar Excel. Verifique o log para mais detalhes.", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("exportToExcel", "Erro de permissão ao exportar Excel: ${e.message}")
            Toast.makeText(this, "Erro de permissão ao exportar Excel.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("exportToExcel", "Erro inesperado ao exportar Excel: ${e.message}")
            Toast.makeText(this, "Erro inesperado ao exportar Excel.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para criar o arquivo Excel
    private val createExcelFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            exportToExcel(uri)
        }
    }

    // Função para iniciar o processo de salvamento do Excel
    private fun promptSaveExcel() {
        if (qrCodeHistory.isEmpty()) {
            Toast.makeText(this, "Não há dados para exportar!", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "$contratoName - $anelName.xlsx" // Nome do arquivo atualizado
        createExcelFileLauncher.launch(fileName)
    }

    // Funções auxiliares para obter nomes (agora sem posto)
    private fun getNomeDoEvento(): String {
        return "$contratoName - $anelName"
    }
    private fun getSupervisorName(): String {
        return supervisorName
    }

    // Composable principal da tela
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Composable
    fun MainScreenMenuInferior(
        qrCodeHistory: List<QrCodeData>,
        onScanQrCode: () -> Unit,
        onClearHistory: () -> Unit
    ) {
        val context = LocalContext.current
        var showClearDialog by remember { mutableStateOf(false) }

        // Diálogo de confirmação para limpar histórico
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
                            containerColor = BlueMainColor,
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
                            containerColor = BlueMainColor,
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
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically // Centraliza verticalmente os ícones
                    ) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Limpar Histórico",
                                tint = BlueMainColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = onScanQrCode) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Escanear QR Code",
                                tint = BlueMainColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = { promptSaveExcel() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Exportar para Excel",
                                tint = BlueMainColor,
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
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .alpha(0.50f), // Opacidade reduzida para um visual mais sutil
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Exibe Contrato > Anel > Supervisor
                            Text(
                                text = "$contratoName > $anelName > $supervisorName",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center // Centraliza o texto
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    if (qrCodeHistory.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(), // Preenche o espaço disponível
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
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        itemsIndexed(qrCodeHistory) { index, qrCodeData ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { /* Adicionar funcionalidade de clique no cartão se necessário */ }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Ícone da BSB
                                    Image(
                                        painter = painterResource(id = R.drawable.bsbappiconstart),
                                        contentDescription = "Ícone Padrão",
                                        modifier = Modifier.size(36.dp)
                                    )

                                    // Coluna com Colaborador e Matrícula
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 16.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = qrCodeData.colaborador,
                                            style = MaterialTheme.typography.titleMedium, // Melhor para nomes
                                            modifier = Modifier.alpha(0.9f),
                                            textAlign = TextAlign.Start
                                        )
                                        Text(
                                            text = qrCodeData.matricula,
                                            style = MaterialTheme.typography.bodyMedium, // Melhor para matrícula
                                            modifier = Modifier.alpha(0.7f),
                                            textAlign = TextAlign.Start
                                        )
                                    }

                                    // Botões de Ação
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Botão "Saiba Mais"
                                        IconButton(onClick = {
                                            selectedQrCodeDataForDetails = qrCodeData
                                            showCardDetailsDialog = true
                                        }) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.detalhe),
                                                contentDescription = "Saiba Mais",
                                                tint = BlueMainColor,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Botão "Excluir Item"
                                        IconButton(onClick = {
                                            qrCodeDataToDelete = qrCodeData
                                            showDeleteConfirmationDialog = true
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Excluir Item",
                                                tint = BlueMainColor,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )

        // Diálogo de seleção de Posto (aparece após escanear um QR Code)
        PostoSelectionDialog(
            showDialog = showPostoSelectionDialog,
            // A lista de postos disponíveis já está filtrada pelo anel na lógica do scanner
            postos = availablePostos,
            onPostoSelected = { selectedPosto ->
                salvarQrCodeData(qrContentAfterScan, selectedPosto)
                showPostoSelectionDialog = false
                qrContentAfterScan = null // Limpa o conteúdo do QR Code
            },
            onDismiss = {
                showPostoSelectionDialog = false
                qrContentAfterScan = null // Limpa o conteúdo se o usuário cancelar
                Toast.makeText(context, "Seleção de posto cancelada.", Toast.LENGTH_SHORT).show()
            }
        )

        // Diálogo de detalhes do cartão
        CardDetailsDialog(
            showDialog = showCardDetailsDialog,
            qrCodeData = selectedQrCodeDataForDetails,
            onDismiss = { showCardDetailsDialog = false }
        )

        // Diálogo de confirmação de exclusão
        DeleteConfirmationDialog(
            showDialog = showDeleteConfirmationDialog,
            onConfirm = {
                qrCodeDataToDelete?.let { qrCodeToRemove ->
                    val wasRemoved = (qrCodeHistory as MutableList<QrCodeData>).remove(qrCodeToRemove)
                    if (wasRemoved) {
                        Toast.makeText(context, "Item excluído com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Erro ao excluir item. Item não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(context, "Nenhum item selecionado para exclusão.", Toast.LENGTH_SHORT).show()
                }
                showDeleteConfirmationDialog = false
                qrCodeDataToDelete = null // Limpa a referência após a remoção
            },
            onDismiss = {
                showDeleteConfirmationDialog = false
                qrCodeDataToDelete = null // Limpa a referência se o usuário cancelar
            }
        )
    }

    // Função auxiliar para filtrar caracteres (pode não ser mais necessária se os campos não são editáveis,
    // mas mantida por segurança se houver algum outro uso)
    fun filtrarCaracteres(texto: String): String {
        val regex = Regex("[^a-zA-Z0-9 ]")
        return regex.replace(texto, "")
    }

    // Essa extensão de Color.Companion.fromHex pode ser movida para um arquivo de utilitários
    // ou removida se não estiver sendo usada em nenhum outro lugar após as mudanças.
    // private fun Color.Companion.fromHex(colorString: String): Color {
    //     return Color(android.graphics.Color.parseColor(colorString))
    // }
}