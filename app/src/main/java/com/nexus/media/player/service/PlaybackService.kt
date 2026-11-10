package com.nexus.media.player.service

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nexus.media.NexusApplication

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val pm = (application as NexusApplication).container.player
        session = MediaSession.Builder(this, pm.player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
