package com.carlosramos.misgastos.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Prepara una foto elegida en el teléfono para subirla: la endereza (EXIF), recorta el centro en cuadrado
 * y la deja en 512 × 512 JPEG (igual que el recortador de la web; liviana y nítida en los círculos).
 */
object Imagen {
    fun paraSubir(context: Context, uri: Uri, lado: Int = 512): ByteArray? = runCatching {
        val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val grados = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val derecha = if (grados == 0f) original else Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { postRotate(grados) }, true)
        val tam = min(derecha.width, derecha.height)
        val cuadrada = Bitmap.createBitmap(derecha, (derecha.width - tam) / 2, (derecha.height - tam) / 2, tam, tam)
        val final = Bitmap.createScaledBitmap(cuadrada, lado, lado, true)
        ByteArrayOutputStream().use { out -> final.compress(Bitmap.CompressFormat.JPEG, 88, out); out.toByteArray() }
    }.getOrNull()
}
