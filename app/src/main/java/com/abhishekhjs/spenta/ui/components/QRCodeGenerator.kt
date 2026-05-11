package com.abhishekhjs.spenta.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QRCodeGenerator(content: String, modifier: Modifier = Modifier) {
    val qrColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val bgColor = MaterialTheme.colorScheme.surface.toArgb()
    val bitmap = remember(content, qrColor, bgColor) {
        generateQRCode(content, qrColor, bgColor)
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = modifier.size(200.dp)
        )
    }
}

private fun generateQRCode(content: String, qrColor: Int, bgColor: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) qrColor else bgColor)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
