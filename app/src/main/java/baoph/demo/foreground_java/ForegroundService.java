package baoph.demo.foreground_java;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("ForegroundService", "Create foreground service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String dataIntent = intent.getStringExtra("key_data_intent");
        sendNotification(dataIntent);
        return START_NOT_STICKY;
    }

    private void sendNotification(String dataIntent) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
                .setContentTitle("Notification service")
                .setContentText(dataIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ForegroundService", "Destroy foreground service");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}