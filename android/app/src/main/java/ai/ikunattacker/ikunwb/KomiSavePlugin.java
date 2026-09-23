package ai.ikunattacker.ikunwb;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "KomiSave")
public class KomiSavePlugin extends Plugin {
    @PluginMethod
    public void save(PluginCall call) {
        JSObject d = call.getData();
        String base64 = d.optString("base64", "");
        String name = d.optString("name", "ikun_" + System.currentTimeMillis() + ".png");
        String mime = d.optString("mime", "image/png");
        if (base64.isEmpty()) { call.reject("empty data"); return; }
        byte[] data;
        try { data = Base64.decode(base64, Base64.DEFAULT); }
        catch (Exception e) { call.reject("bad base64: " + e.getMessage()); return; }
        try {
            String where;
            boolean isVideo = mime.startsWith("video");
            if (Build.VERSION.SDK_INT >= 29) {
                /* Android 10+：MediaStore 直插公共相册/下载，无需存储权限 */
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                String sub = isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES;
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, sub + "/ikun");
                cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
                Uri col = isVideo ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                  : MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri uri = getContext().getContentResolver().insert(col, cv);
                OutputStream os = getContext().getContentResolver().openOutputStream(uri);
                os.write(data); os.flush(); os.close();
                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContext().getContentResolver().update(uri, done, null, null);
                where = sub + "/ikun/" + name;
            } else {
                /* Android 9-：写入应用专属外部目录（免权限） */
                boolean isImg = mime.startsWith("image");
                File dir = getContext().getExternalFilesDir(isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES);
                File f = new File(dir, "ikun/" + name);
                f.getParentFile().mkdirs();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(data); fo.close();
                where = f.getAbsolutePath();
            }
            JSObject r = new JSObject();
            r.put("path", where);
            call.resolve(r);
        } catch (Exception e) { call.reject("save failed: " + e.getMessage()); }
    }
}
