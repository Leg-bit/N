package com.nexus.media.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MediaPermissions {
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun hasMedia(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33)
            granted(ctx, Manifest.permission.READ_MEDIA_AUDIO) && granted(ctx, Manifest.permission.READ_MEDIA_VIDEO)
        else granted(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
}
