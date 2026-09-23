package ai.ikunattacker.ikunwb;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "KomiNotify")
public class KomiNotifyPlugin extends Plugin {
    @PluginMethod
    public void show(PluginCall call) {
        JSObject d = call.getData();
        KomiNotifyService.start(getContext(),
            d.optString("title", "ikun新媒体制作中心"),
            d.optString("text", "生成中…"),
            d.optString("priority", "default"));
        call.resolve();
    }
    @PluginMethod
    public void update(PluginCall call) {
        JSObject d = call.getData();
        KomiNotifyService.notifyUpdate(getContext(), d.optString("text", ""), d.optInt("progress", -1));
        call.resolve();
    }
    @PluginMethod
    public void hide(PluginCall call) {
        KomiNotifyService.stop(getContext());
        call.resolve();
    }
    @PluginMethod
    public void result(PluginCall call) {
        JSObject d = call.getData();
        KomiNotifyService.notifyResult(getContext(), d.optBoolean("ok", false), d.optString("text", ""));
        call.resolve();
    }
}
