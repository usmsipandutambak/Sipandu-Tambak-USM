package id.ac.usm.sipandutambak.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import id.ac.usm.sipandutambak.R
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onDeviceDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraInstance by remember { mutableStateOf< Camera? >(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Izin kamera diperlukan untuk memindai QR!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val qrVal = barcodes.firstOrNull()?.rawValue
                        if (!qrVal.isNullOrBlank()) {
                            onDeviceDetected(qrVal)
                        } else {
                            Toast.makeText(context, "QR code tidak terdeteksi pada gambar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal memproses gambar galeri.", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live Camera Preview View (CameraX)
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val barcodeScanner = BarcodeScanning.getClient()
                        val executor = Executors.newSingleThreadExecutor()

                        var isHandled = false
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isHandled) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val qrCode = barcodes.firstOrNull()?.rawValue
                                        if (!qrCode.isNullOrBlank() && !isHandled) {
                                            isHandled = true
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onDeviceDetected(qrCode)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraInstance = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Header SIPANDU TAMBAK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDCEBFE))
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .align(Alignment.TopCenter)
        ) {
            Column {
                Text(
                    text = "SIPANDU",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF453636),
                    letterSpacing = 0.5.sp,
                    lineHeight = 22.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.famicons_fish),
                        contentDescription = "Logo Ikan",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 36.dp, height = 24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TAMBAK",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF453636),
                        letterSpacing = 0.5.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // 3. Garis Penarget 4 Sudut
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            ScannerTargetOverlay(modifier = Modifier.size(280.dp))
        }

        // 4. Panel Bawah Transparan Gelap dengan Tombol Shutter Menonjol ke Atas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Panel Latar Gelap Lengkung
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0x99262626))
                    .padding(horizontal = 38.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Flashlight (Kiri)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                if (cameraInstance?.cameraInfo?.hasFlashUnit() == true) {
                                    isFlashOn = !isFlashOn
                                    cameraInstance?.cameraControl?.enableTorch(isFlashOn)
                                } else {
                                    Toast.makeText(context, "Flash tidak tersedia", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FlashlightIcon(isOn = isFlashOn)
                    }

                    // Spacer Penyeimbang untuk Tombol Shutter di Tengah
                    Spacer(modifier = Modifier.size(76.dp))

                    // Tombol Galeri (Kanan)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        GalleryPickerIcon()
                    }
                }
            }

            // Tombol Shutter Menonjol Lebih Tinggi ke Atas (Sesuai Tangkapan Layar Figma)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
                    .size(80.dp)
                    .shadow(elevation = 14.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(BorderStroke(3.dp, Color(0xFF1E293B)), CircleShape)
                    .clickable { onDeviceDetected("Alat A") },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(BorderStroke(1.5.dp, Color(0xFF94A3B8)), CircleShape)
                )
            }
        }
    }
}

@Composable
fun ScannerTargetOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bracketLen = w * 0.28f
        val strokeStyle = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val bracketColor = Color(0xFF6EC6F8)

        val topLeft = Path().apply {
            moveTo(0f, bracketLen); lineTo(0f, 0f); lineTo(bracketLen, 0f)
        }
        drawPath(topLeft, color = bracketColor, style = strokeStyle)

        val topRight = Path().apply {
            moveTo(w - bracketLen, 0f); lineTo(w, 0f); lineTo(w, bracketLen)
        }
        drawPath(topRight, color = bracketColor, style = strokeStyle)

        val bottomLeft = Path().apply {
            moveTo(0f, h - bracketLen); lineTo(0f, h); lineTo(bracketLen, h)
        }
        drawPath(bottomLeft, color = bracketColor, style = strokeStyle)

        val bottomRight = Path().apply {
            moveTo(w - bracketLen, h); lineTo(w, h); lineTo(w, h - bracketLen)
        }
        drawPath(bottomRight, color = bracketColor, style = strokeStyle)
    }
}

@Composable
fun FlashlightIcon(isOn: Boolean) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val iconColor = if (isOn) Color(0xFFEAB308) else Color(0xFF0F172A)

        val bolt = Path().apply {
            moveTo(w * 0.58f, 0f)
            lineTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.48f, h * 0.52f)
            lineTo(w * 0.38f, h)
            lineTo(w * 0.82f, h * 0.44f)
            lineTo(w * 0.52f, h * 0.44f)
            close()
        }
        drawPath(bolt, color = iconColor)
    }
}

@Composable
fun GalleryPickerIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val iconColor = Color(0xFF0F172A)

        drawRoundRect(
            color = iconColor,
            topLeft = Offset(0f, 0f),
            size = Size(w * 0.82f, h * 0.82f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            style = Stroke(width = 2.2.dp.toPx())
        )

        val mountain = Path().apply {
            moveTo(w * 0.12f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.55f, h * 0.55f)
            lineTo(w * 0.70f, h * 0.42f)
            lineTo(w * 0.76f, h * 0.65f)
            close()
        }
        drawPath(mountain, color = iconColor)

        drawLine(
            color = iconColor,
            start = Offset(w * 0.68f, h * 0.82f),
            end = Offset(w, h * 0.82f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = iconColor,
            start = Offset(w * 0.84f, h * 0.66f),
            end = Offset(w * 0.84f, h * 0.98f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}