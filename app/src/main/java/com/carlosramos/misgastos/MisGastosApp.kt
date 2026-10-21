package com.carlosramos.misgastos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class MisGastosApp : Application(), ImageLoaderFactory {

    /** El mismo cliente de la API: las fotos de contactos son privadas y necesitan el token. */
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(okHttpClient)
        .crossfade(true)
        .build()
}
