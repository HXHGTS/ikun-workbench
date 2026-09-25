package ai.ikunattacker.komi

import android.Manifest
import android.app.Activity
import androidx.core.app.ActivityCompat
import app.tauri.annotation.Command
import app.tauri.annotation.Permission
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin

data class ShowArgs(val title: String = "ikun新媒体制作中心", val text: String = "生成中…", val priority: String = "default")
data class UpdateArgs(val text: String = "", val progress: Int = -1)
data class ResultArgs(val ok: Boolean = false, val text: String = "")
data class SaveArgs(val name: String = "", val mime: String = "image/png", val base64: String = "")

@TauriPlugin
class KomiPlugin(private val activity: Activity) : Plugin(activity) {
    @Command
    fun show(invoke: Invoke) {
        try {
            val a = invoke.parseArgs(ShowArgs::class.java)
            KomiService.start(activity, a.title, a.text, a.priority)
            invoke.resolve()
        } catch (e: Exception) {
            invoke.reject(e.message ?: "notification start failed")
        }
    }
    @Command
    fun update(invoke: Invoke) {
        try {
            val a = invoke.parseArgs(UpdateArgs::class.java)
            KomiService.notifyUpdate(activity, a.text, a.progress)
            invoke.resolve()
        } catch (e: Exception) {
            invoke.reject(e.message ?: "notification update failed")
        }
    }
    @Command
    fun hide(invoke: Invoke) {
        try {
            KomiService.stop(activity)
            invoke.resolve()
        } catch (e: Exception) {
            invoke.reject(e.message ?: "notification stop failed")
        }
    }
    @Command
    fun result(invoke: Invoke) {
        try {
            val a = invoke.parseArgs(ResultArgs::class.java)
            KomiService.notifyResult(activity, a.ok, a.text)
            invoke.resolve()
        } catch (e: Exception) {
            invoke.reject(e.message ?: "result notification failed")
        }
    }
    @Command
    fun save(invoke: Invoke) {
        val a = invoke.parseArgs(SaveArgs::class.java)
        try {
            val r = JSObject()
            r.put("path", KomiMedia.save(activity, a.name, a.mime, a.base64))
            invoke.resolve(r)
        } catch (e: Exception) {
            invoke.reject(e.message ?: "save failed")
        }
    }
}
