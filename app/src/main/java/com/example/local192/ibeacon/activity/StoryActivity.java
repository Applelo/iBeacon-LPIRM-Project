package com.example.local192.ibeacon.activity;

import android.Manifest;

import android.app.Activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import com.github.clans.fab.FloatingActionButton;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ListView;
import android.widget.TextView;


import com.example.local192.ibeacon.R;
import com.example.local192.ibeacon.adapter.SallesAdapter;
import com.example.local192.ibeacon.model.Salle;
import com.plattysoft.leonids.ParticleSystem;

import java.util.ArrayList;

import java.util.List;


import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class StoryActivity extends Activity {

    private static final String LOG_TAG = "StoryActivity";
    private static final List<Salle> salles = new ArrayList<Salle>();
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 5000;
    private boolean isScanning = false;
    final int RC_LOCATION = 1;
    private int actualMajor;
    private int actualMinor;
    private int score;

    // ------------------------------------------------------------------------
// Inner classes
// ------------------------------------------------------------------------

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5)
            {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15)
                { //Identifies correct data length
                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound)
            {
                //Convert to hex String
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //UUID detection
                String uuid =  hexString.substring(0,8) + "-" +
                        hexString.substring(8,12) + "-" +
                        hexString.substring(12,16) + "-" +
                        hexString.substring(16,20) + "-" +
                        hexString.substring(20,32);

                // major
                final int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

                // minor
                final int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);

                Log.i(LOG_TAG,"UUID: " +uuid + "\nmajor: " +major +"\nminor: " +minor + "\nrssi: " + rssi + "\nmeters: " + calculateDistance(rssi));

                if (rssi > -65 && (actualMinor != minor || actualMajor != major) ) {

                    actualMajor = major;
                    actualMinor = minor;
                    updateStory();

                    /*String text;
                    text = "UUID: " + uuid;
                    tUuid.setText(text);
                    text = "Major: " + major;
                    tMajor.setText(text);
                    text = "Minor: " + minor;
                    tMinor.setText(text);
                    text = "Rssi: " + rssi;
                    tRssi.setText(text);
                    text = "Meters: " + calculateDistance(rssi);
                    tMeters.setText(text);*/
                }

            }

        }
    };

    private void updateStory() {
        TextView textStory = (TextView) findViewById(R.id.textStory);
        for (Salle salle : salles) {
            if (salle.getMajor() == actualMajor && salle.getMinor() == actualMinor) {
                textStory.setText(salle.getName() + " : " + salle.getText());
                textStory.setCompoundDrawablesWithIntrinsicBounds(0, salle.getDrawable(), 0, 0);
                if (salle.isVisited() == false) {
                    score++;
                    if (score == salles.size()) {
                        new ParticleSystem(this, 40, R.drawable.confeti2, 10000)
                                .setSpeedModuleAndAngleRange(0f, 0.3f, 180, 180)
                                .setRotationSpeed(144)
                                .setAcceleration(0.00005f, 90)
                                .emit(findViewById(R.id.emiter_top_right), 2);

                        new ParticleSystem(this, 40, R.drawable.confeti3, 10000)
                                .setSpeedModuleAndAngleRange(0f, 0.3f, 0, 0)
                                .setRotationSpeed(144)
                                .setAcceleration(0.00005f, 90)
                                .emit(findViewById(R.id.emiter_top_left), 2);
                    }
                    salle.setVisited(true);
                }
                sallesAdapter.notifyDataSetChanged();
                createNotification(salle);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_LOCATION)
    private void methodPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "wesh",
                    RC_LOCATION, perms);
        }
    }

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // ------------------------------------------------------------------------
// public usage
// ------------------------------------------------------------------------

    private Runnable scanRunnable = new Runnable()
    {
        @Override
        public void run() {

            if (isScanning)
            {
                if (btAdapter != null)
                {
                    btAdapter.stopLeScan(leScanCallback);
                }
            }
            else
            {
                if (btAdapter != null)
                {
                    btAdapter.startLeScan(leScanCallback);
                }
            }

            isScanning = !isScanning;

            scanHandler.postDelayed(this, scan_interval_ms);
        }
    };

    BottomSheetBehavior bts;
    FloatingActionButton fab;
    int btsState = BottomSheetBehavior.STATE_COLLAPSED;
    ListView listSalles;
    SallesAdapter sallesAdapter;
    Animation animation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);
        bts = BottomSheetBehavior.from(findViewById(R.id.bts));
        fab = (FloatingActionButton) findViewById(R.id.fabBts);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bts.setState(btsState == BottomSheetBehavior.STATE_COLLAPSED?BottomSheetBehavior.STATE_EXPANDED: BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        bts.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (btsState != newState){
                    switch (newState){
                        case BottomSheetBehavior.STATE_COLLAPSED:
                            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_upward_white_18dp));
                            fab.clearAnimation();
                            animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.arrow_rotation);
                            fab.setAnimation(animation);
                            fab.animate();
                            btsState = newState;
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_downward_white_18dp));
                            fab.clearAnimation();
                            animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.arrow_rotation);
                            fab.setAnimation(animation);
                            fab.animate();
                            btsState = newState;
                            break;
                        default:
                            Log.e("BottomSheetState", "Not know");
                            break;
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        listSalles = (ListView) findViewById(R.id.listSalles);
        salles.clear();
        salles.add(new Salle(1, 1, "C101", R.drawable.ic_launcher_background, "La C101 est la salle priviligié pour créer et développer des programmes innovants."));
        salles.add(new Salle(1, 2, "C105", R.drawable.ic_launcher_background, "Elle est habité par l'esprit de la DGSE, keep warning"));
        salles.add(new Salle(1, 3, "Secrétariat R&T", R.drawable.ic_launcher_background, "C'est beau"));
        salles.add(new Salle(2, 1, "Salle Café Prof", R.drawable.ic_launcher_background, "Si vous êtes professeur et que vous avez envie de faire une pause avec vos confrères, vous êtes au bonne endroit."));
        salles.add(new Salle(2, 2, "Bureau M.Faucher", R.drawable.ic_launcher_background, "Ah"));
        salles.add(new Salle(2, 3, "Secrétariat Info", R.drawable.ic_launcher_background, "Un problème, c'est ici qu'on trouve la solution"));

        sallesAdapter = new SallesAdapter(this, salles);
        listSalles.setAdapter(sallesAdapter);
        methodPermission();

        // init BLE
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        scanHandler.post(scanRunnable);
    }

    @Override
    public void onBackPressed() {
        if (btsState == BottomSheetBehavior.STATE_EXPANDED){
            bts.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }else {
            super.onBackPressed();
        }
    }

    private double calculateDistance(int rssi) {

        int txPower = -59; //hard coded power value. Usually ranges between -59 to -65

        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance;
        }
    }

    private final void createNotification(Salle salle){
        Intent intent = new Intent(this, StoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(this)
                .setContentIntent(pendingIntent)
                .setContentTitle("iBeacons")
                .setContentText("Tu es proche de la salle : " + salle.getName())
                .setSmallIcon(R.drawable.ic_nfc_black_48dp)
                .setVibrate(new long[]{100, 250, 100, 250})
                .build();
        notificationManager.notify(1, notification);
    }


}
