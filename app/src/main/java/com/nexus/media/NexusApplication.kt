package com.nexus.media

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.nexus.media.di.AppContainer

class NexusApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
