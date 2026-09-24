package ai.ikunattacker.komi

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

object KomiMedia {
    fun save(c: Context, name: String, mime: String, base64Data: String): String {
        val data = Base64.decode(base64Data, Base64.DEFAULT)
        val isVideo = mime.startsWith("video")
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                val sub = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$sub/ikun")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val col = if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                      else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = c.contentResolver.insert(col, cv)!!
            c.contentResolver.openOutputStream(uri)!!.use { os ->
                os.write(data); os.flush()
            }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            c.contentResolver.update(uri, done, null, null)
            val sub = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            return "$sub/ikun/$name"
        } else {
            val dir = c.getExternalFilesDir(if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)!!
            val f = File(dir, "ikun/$name")
            f.parentFile!!.mkdirs()
            FileOutputStream(f).use { it.write(data) }
            return f.absolutePath
        }
    }
}
