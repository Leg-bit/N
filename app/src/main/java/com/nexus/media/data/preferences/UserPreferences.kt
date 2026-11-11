package com.nexus.media.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val sp = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    private val _hidden = MutableStateFlow(sp.getStringSet("hidden", emptySet())!!.toSet())
    val hiddenFolders: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _custom = MutableStateFlow(sp.getStringSet("custom", emptySet())!!.toSet())
    val customFolders: StateFlow<Set<String>> = _custom.asStateFlow()

    fun setHidden(path: String, hide: Boolean) {
        val n = if (hide) _hidden.value + path else _hidden.value - path
        _hidden.value = n
        sp.edit().putStringSet("hidden", n).apply()
    }

    fun setCustom(uri: String, add: Boolean) {
        val n = if (add) _custom.value + uri else _custom.value - uri
        _custom.value = n
        sp.edit().putStringSet("custom", n).apply()
    }

    var playerMode: String
        get() = sp.getString("mode", "artwork")!!
        set(v) = sp.edit().putString("mode", v).apply()
    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(v) = sp.edit().putBoolean("shuffle", v).apply()
    var repeatMode: Int
        get() = sp.getInt("repeat", 0)
        set(v) = sp.edit().putInt("repeat", v).apply()
    var eqProfile: String
        get() = sp.getString("eq", "")!!
        set(v) = sp.edit().putString("eq", v).apply()
    var lastQueue: String
        get() = sp.getString("last_queue", "")!!
        set(v) = sp.edit().putString("last_queue", v).apply()
    var lastIndex: Int
        get() = sp.getInt("last_index", 0)
        set(v) = sp.edit().putInt("last_index", v).apply()
    var lastPosition: Long
        get() = sp.getLong("last_pos", 0L)
        set(v) = sp.edit().putLong("last_pos", v).apply()

    var videoBgAudio: Boolean
        get() = sp.getBoolean("video_bg_audio", false)
        set(v) = sp.edit().putBoolean("video_bg_audio", v).apply()

    fun lyricsUri(id: String): String? = sp.getString("lyr_$id", null)
    fun setLyricsUri(id: String, uri: String) = sp.edit().putString("lyr_$id", uri).apply()

    fun profiles(): Map<String, String> =
        (sp.getStringSet("eq_profiles", emptySet()) ?: emptySet()).mapNotNull {
            val i = it.indexOf('=')
            if (i < 0) null else it.substring(0, i) to it.substring(i + 1)
        }.toMap()

    fun saveProfile(name: String, data: String) = writeProfiles(profiles() + (name to data))
    fun deleteProfile(name: String) = writeProfiles(profiles() - name)
    private fun writeProfiles(m: Map<String, String>) {
        sp.edit().putStringSet("eq_profiles", m.map { "${it.key}=${it.value}" }.toSet()).apply()
    }
}
