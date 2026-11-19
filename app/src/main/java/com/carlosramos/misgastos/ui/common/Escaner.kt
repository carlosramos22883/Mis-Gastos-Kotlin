package com.carlosramos.misgastos.ui.common

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Escanea un código de barras de producto (EAN-13, EAN-8, UPC) con el escáner de Google Play Services.
 * [alLeer] recibe solo los dígitos; [alFallar] un mensaje (si se cancela no se llama ninguno).
 */
object Escaner {
    fun escanear(context: Context, alLeer: (String) -> Unit, alFallar: (String) -> Unit = {}) {
        val opciones = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, opciones).startScan()
            .addOnSuccessListener { codigo -> codigo.rawValue?.filter { it.isDigit() }?.takeIf { it.length in 8..14 }?.let(alLeer) ?: alFallar("Ese código no es de un producto.") }
            .addOnFailureListener { alFallar("No se pudo abrir el escáner. Revisa que Google Play Services esté al día.") }
    }
}
