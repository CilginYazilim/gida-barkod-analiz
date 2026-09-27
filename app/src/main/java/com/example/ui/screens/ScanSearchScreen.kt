package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationTab
import com.example.ui.ProductViewModel
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.ForestGreen

// CameraX & ML Kit imports
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSearchScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    var barcodeInput by remember { mutableStateOf("") }
    var torchOn by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.analyzeProductImageUri(uri)
    }

    // Tarama çizgisi animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tam ekran canlı kamera
        CameraScannerView(
            onBarcodeDetected = { barcode -> viewModel.analyzeProduct(barcode) },
            torchOn = torchOn,
            modifier = Modifier.fillMaxSize()
        )

        // Üst & alt karartma degradeleri (yazılar okunsun)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Üst satır: marka solda, Kıyasla kısayolu sağda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Akıllı Gıda", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { torchOn = !torchOn }) {
                        Icon(
                            imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Fener",
                            tint = if (torchOn) EcoGreen else Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.setTab(NavigationTab.COMPARE) }) {
                        Icon(Icons.Default.CompareArrows, contentDescription = "Kıyasla", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Köşe ayraçlı vizör + yumuşak tarama çizgisi
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1.1f)
                    .drawBehind {
                        val stroke = 4.dp.toPx()
                        val corner = size.minDimension * 0.22f
                        val r = 18.dp.toPx()
                        val c = EcoGreen
                        // Sol üst
                        drawLine(c, Offset(0f, corner), Offset(0f, r), stroke, cap = StrokeCap.Round)
                        drawArc(c, 180f, 90f, false, topLeft = Offset(0f, 0f), size = Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                        drawLine(c, Offset(r, 0f), Offset(corner, 0f), stroke, cap = StrokeCap.Round)
                        // Sağ üst
                        drawLine(c, Offset(size.width - corner, 0f), Offset(size.width - r, 0f), stroke, cap = StrokeCap.Round)
                        drawArc(c, 270f, 90f, false, topLeft = Offset(size.width - r * 2, 0f), size = Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                        drawLine(c, Offset(size.width, r), Offset(size.width, corner), stroke, cap = StrokeCap.Round)
                        // Sağ alt
                        drawLine(c, Offset(size.width, size.height - corner), Offset(size.width, size.height - r), stroke, cap = StrokeCap.Round)
                        drawArc(c, 0f, 90f, false, topLeft = Offset(size.width - r * 2, size.height - r * 2), size = Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                        drawLine(c, Offset(size.width - r, size.height), Offset(size.width - corner, size.height), stroke, cap = StrokeCap.Round)
                        // Sol alt
                        drawLine(c, Offset(corner, size.height), Offset(r, size.height), stroke, cap = StrokeCap.Round)
                        drawArc(c, 90f, 90f, false, topLeft = Offset(0f, size.height - r * 2), size = Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                        drawLine(c, Offset(0f, size.height - r), Offset(0f, size.height - corner), stroke, cap = StrokeCap.Round)
                        // Tarama çizgisi
                        val y = (size.height * 0.12f) + (size.height * 0.76f) * laserOffset
                        drawLine(
                            brush = Brush.horizontalGradient(listOf(Color.Transparent, EcoGreen, EcoGreen, Color.Transparent)),
                            start = Offset(size.width * 0.06f, y),
                            end = Offset(size.width * 0.94f, y),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
            )

            Spacer(Modifier.height(22.dp))
            Text("Barkod Tara", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Barkodu çerçevenin ortasına hizalayın",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // Barkod gir — yuvarlak koyu alan
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                placeholder = { Text("Barkod gir", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White.copy(alpha = 0.85f)) },
                trailingIcon = {
                    if (barcodeInput.isNotEmpty()) {
                        IconButton(onClick = {
                            if (barcodeInput.trim().isNotEmpty()) {
                                viewModel.analyzeProduct(barcodeInput); keyboardController?.hide()
                            }
                        }) { Icon(Icons.Default.Search, contentDescription = "Ara", tint = EcoGreen) }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (barcodeInput.trim().isNotEmpty()) {
                        viewModel.analyzeProduct(barcodeInput); keyboardController?.hide()
                    }
                }),
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("barcode_text_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.12f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = EcoGreen
                )
            )

            Spacer(Modifier.height(14.dp))

            // Alt aksiyonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (barcodeInput.trim().isNotEmpty()) {
                            viewModel.analyzeProduct(barcodeInput); keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("analyze_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Analiz Et", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("gallery_picker_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Galeriden Seç", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerView(
    onBarcodeDetected: (String) -> Unit,
    torchOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CameraXPreviewSurface(onBarcodeDetected, torchOn, modifier)
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1C1B1F)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kamera İzni Gerekli",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gıda barkodlarını kamerayla anında tarayabilmek için kamera erişimine izin vermelisiniz.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera İznini Onayla", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraXPreviewSurface(
    onBarcodeDetected: (String) -> Unit,
    torchOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var lastScannedBarcode by remember { mutableStateOf("") }
    var scanThrottleTime by remember { mutableLongStateOf(0L) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Fener durumunu kameraya uygula (donanım destekliyorsa).
    LaunchedEffect(camera, torchOn) {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(torchOn)
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val executor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Configure scanner for EAN 13 and other common gıda barkod formats
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_QR_CODE
                    )
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val barcode = barcodes.firstOrNull()?.rawValue
                                if (barcode != null && barcode.isNotBlank()) {
                                    val now = System.currentTimeMillis()
                                    // Throttle scans to prevent double calling in rapid succession
                                    if (barcode != lastScannedBarcode || now - scanThrottleTime > 2500L) {
                                        lastScannedBarcode = barcode
                                        scanThrottleTime = now
                                        onBarcodeDetected(barcode)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                // Scanning failed or no barcode found in frame
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
