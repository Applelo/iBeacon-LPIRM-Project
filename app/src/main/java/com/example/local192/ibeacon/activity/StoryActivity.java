package com.example.local192.ibeacon.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.local192.ibeacon.R;
import com.example.local192.ibeacon.model.Salle;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryActivity extends Activity {

    private static final String LOG_TAG = "StoryActivity";
    private static final List<Salle> salles = new ArrayList<Salle>();
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 5000;
    private boolean isScanning = false;
    final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;
    private int actualMajor;
    private int actualMinor;
    private int nearRssi;

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

                if (rssi > -50 && (actualMinor != minor || actualMajor != major) ) {

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
                textStory.setText(salle.getText());
                textStory.setCompoundDrawablesWithIntrinsicBounds(0, salle.getDrawable(), 0, 0);
                salle.setVisited(true);
            }
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
    boolean btsState = false;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        bts = BottomSheetBehavior.from(findViewById(R.id.bts));
        TextView btsTitle = (TextView) findViewById(R.id.btsTitle);
        btsTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btsState) {
                    bts.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                else {
                    bts.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
               btsState = !btsState;
            }
        });

        salles.add(new Salle(10, 3, "Salle1", R.drawable.ic_launcher_background, "Je suis une salle, je suis le beacon de Loïs"));
        salles.add(new Salle(7, 4, "Salle2", R.drawable.ic_launcher_background, "Je suis une salle, je suis le beacon de Adrian"));


        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            needPermission();
        }

        // init BLE
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        scanHandler.post(scanRunnable);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);


                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        ) {
                    // All Permissions Granted

                    // Permission Denied
                    //Toast.makeText(StoryActivity.this, "All Permission GRANTED !! Thank You :)", Toast.LENGTH_SHORT).show();


                } else {
                    // Permission Denied
                    //Toast.makeText(StoryActivity.this, "One or More Permissions are DENIED Exiting App :(", Toast.LENGTH_SHORT).show();

                    finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void needPermission() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("Connaitre la localisation");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {

                // Need Rationale
                String message = "L'application a besoin d'accéder à votre localisation pour fonctionner correctement.";

                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(StoryActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Annuler", null)
                .create()
                .show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }



}
