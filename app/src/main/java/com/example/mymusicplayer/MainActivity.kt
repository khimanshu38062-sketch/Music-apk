
package com.example.mymusicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class Song(
    val title: String,
    val artist: String,
    val uri: Uri
)

class MainActivity : ComponentActivity() {

    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicPlayerScreen()
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun loadSongs(): List<Song> {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            val titleColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            val artistColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                val uri = Uri.withAppendedPath(
                    collection,
                    id.toString()
                )

                songs.add(
                    Song(
                        cursor.getString(titleColumn) ?: "Unknown",
                        cursor.getString(artistColumn) ?: "Unknown Artist",
                        uri
                    )
                )
            }
        }

        return songs
    }

    private fun playSong(uri: Uri) {
        player?.release()

        player = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            setOnPreparedListener {
                it.start()
            }
            prepareAsync()
        }
    }

    @Composable
    private fun MusicPlayerScreen() {

        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        var currentIndex by remember {
            mutableIntStateOf(-1)
        }

        var isPlaying by remember {
            mutableStateOf(false)
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        fun refreshSongs() {
            songs = loadSongs()
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) refreshSongs()
            }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                refreshSongs()
            } else {
                permissionLauncher.launch(permission)
            }
        }

        fun playAt(index: Int) {
            if (index !in songs.indices) return

            currentIndex = index
            isPlaying = true
            playSong(songs[index].uri)
        }

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    Text(
                        "My Music Player",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("${songs.size} Songs")

                    Spacer(Modifier.height(20.dp))

                    if (songs.isEmpty()) {
                        Text("No music found. Allow audio permission and add songs to your phone.")
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(songs) { index, song ->

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playAt(index)
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                if (currentIndex == index) {
                                    Text("Now Playing")
                                }
                            }

                            HorizontalDivider()
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        if (currentIndex >= 0)
                            songs[currentIndex].title
                        else
                            "Select a song"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Button(
                            onClick = {
                                if (currentIndex > 0) {
                                    playAt(currentIndex - 1)
                                }
                            }
                        ) {
                            Text("Previous")
                        }

                        Button(
                            onClick = {
                                if (isPlaying) {
                                    player?.pause()
                                    isPlaying = false
                                } else if (currentIndex >= 0) {
                                    player?.start()
                                    isPlaying = true
                                } else if (songs.isNotEmpty()) {
                                    playAt(0)
                                }
                            }
                        ) {
                            Text(if (isPlaying) "Pause" else "Play")
                        }

                        Button(
                            onClick = {
                                if (currentIndex < songs.size - 1) {
                                    playAt(currentIndex + 1)
                                }
                            }
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}
