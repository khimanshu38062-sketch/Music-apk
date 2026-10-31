package com.example.mymusicplayer

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

data class Song(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var currentIndex by mutableIntStateOf(-1)
    private var playing by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadSongs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }

        setContent {
            MaterialTheme {
                MusicPlayerScreen(
                    songs = songs,
                    currentIndex = currentIndex,
                    playing = playing,
                    onSelect = { playSong(it) },
                    onPlayPause = { togglePlayback() },
                    onNext = { playAt((currentIndex + 1).coerceAtMost(songs.lastIndex)) },
                    onPrevious = { playAt((currentIndex - 1).coerceAtLeast(0)) }
                )
            }
        }
    }

    private fun loadSongs() {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown title"
                val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown artist"
                result += Song(id, title, artist, ContentUris.withAppendedId(collection, id))
            }
        }
        songs = result
    }

    private fun playSong(index: Int) {
        if (index !in songs.indices) return
        currentIndex = index
        player.setMediaItem(MediaItem.fromUri(songs[index].uri))
        player.prepare()
        player.play()
        playing = true
    }

    private fun playAt(index: Int) = playSong(index)

    private fun togglePlayback() {
        if (currentIndex == -1 && songs.isNotEmpty()) {
            playSong(0)
        } else if (player.isPlaying) {
            player.pause()
            playing = false
        } else {
            player.play()
            playing = true
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
fun MusicPlayerScreen(
    songs: List<Song>,
    currentIndex: Int,
    playing: Boolean,
    onSelect: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val current = songs.getOrNull(currentIndex)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("My Music", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Your offline music library", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text("ALL SONGS  •  ${songs.size}", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (songs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No music found")
                        Text("Add audio files to your phone and reopen the app.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(songs) { index, song ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(index) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                Text(song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (index == currentIndex && playing) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Playing")
                            }
                        }
                    }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(current?.title ?: "Choose a song", fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(current?.artist ?: "Nothing playing", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous") }
                        IconButton(onClick = onPlayPause) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or pause", modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
                    }
                }
            }
        }
    }
}
