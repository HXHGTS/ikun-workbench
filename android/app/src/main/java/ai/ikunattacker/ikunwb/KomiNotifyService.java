package ai.ikunattacker.ikunwb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class KomiNotifyService extends Service {
    public static final int NOTIF_ID = 1002;
    private static PowerManager.WakeLock wl;
    private static String lastPri = "default", lastTitle = "ikun新媒体制作中心", lastText = "";
    private static int lastProgress = -1;

    public static void createChannels(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        String[] ids = {"ikun_gen_low", "ikun_gen_default", "ikun_gen_high"};
        String[] names = {"生成通知（低·静默常驻）", "生成通知（默认）", "生成通知（高·横幅提醒）"};
        int[] imps = {NotificationManager.IMPORTANCE_LOW, NotificationManager.IMPORTANCE_DEFAULT, NotificationManager.IMPORTANCE_HIGH};
        for (int i = 0; i < ids.length; i++) {
            if (nm.getNotificationChannel(ids[i]) == null) {
                NotificationChannel ch = new NotificationChannel(ids[i], names[i], imps[i]);
                ch.setDescription("ikun新媒体制作中心 生成任务通知");
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private static Notification build(Context c, String pri, String title, String text, int progress) {
        Intent launch = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, launch,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, "ikun_gen_" + pri)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title).setContentText(text)
                .setOnlyAlertOnce(true).setOngoing(true)
                .setContentIntent(pi);
        if (progress >= 0) b.setProgress(100, progress, false);
        else b.setProgress(0, 0, true);
        return b.build();
    }

    public static void start(Context c, String title, String text, String pri) {
        lastPri = pri; lastTitle = title; lastText = text; lastProgress = -1;
        createChannels(c);
        Intent i = new Intent(c, KomiNotifyService.class);
        i.putExtra("title", title); i.putExtra("text", text); i.putExtra("priority", pri);
        try { c.startForegroundService(i); } catch (Exception e) { }
    }

    public static void notifyUpdate(Context c, String text, int progress) {
        lastText = text != null ? text : lastText;
        lastProgress = progress;
        createChannels(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, build(c, lastPri, lastTitle, lastText, lastProgress));
    }

    public static void notifyResult(Context c, boolean ok, String text) {
        createChannels(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel("ikun_result") == null) {
            NotificationChannel ch = new NotificationChannel("ikun_result", "生成结果",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("生成成功/失败结果通知");
            nm.createNotificationChannel(ch);
        }
        Intent launch = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 1, launch,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, "ikun_result")
                .setSmallIcon(ok ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert)
                .setContentTitle(ok ? "✅ 生成成功" : "❌ 生成失败")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true).setContentIntent(pi);
        nm.notify(NOTIF_ID + 1, b.build());
    }

    public static void stop(Context c) {
        if (wl != null && wl.isHeld()) wl.release();
        try { c.stopService(new Intent(c, KomiNotifyService.class)); } catch (Exception e) { }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIF_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String pri = intent != null ? intent.getStringExtra("priority") : null;
        String title = intent != null ? intent.getStringExtra("title") : null;
        String text = intent != null ? intent.getStringExtra("text") : null;
        if (pri != null) lastPri = pri;
        if (title != null) lastTitle = title;
        if (text != null) lastText = text;
        Notification n = build(this, lastPri, lastTitle, lastText, -1);
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(NOTIF_ID, n);
        if (wl == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ikun:gen");
            wl.setReferenceCounted(false);
        }
        if (!wl.isHeld()) wl.acquire();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        if (wl != null && wl.isHeld()) wl.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
