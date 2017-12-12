package com.example.local192.ibeacon.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.local192.ibeacon.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LoginActivity extends Activity {

    ImageView imageNfc;
    Handler handler = new Handler();
    IntentFilter[] filters;
    String[][] techs;
    PendingIntent pendingIntent;
    NfcAdapter nfcAdapter;
    long[] cards = new long[]{709826816, 308221696};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.login_text);
        imageNfc = (ImageView) findViewById(R.id.imageNfc);
        startAnim();
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters = new IntentFilter[]{intentFilter};
        techs = new String[][]{new String[]{NfcA.class.getName()}};
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAnim();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techs);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] id = tag.getId();
        ByteBuffer byteBuffer = ByteBuffer.wrap(id);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int sgnedInt = byteBuffer.getInt();
        long number = sgnedInt & 0xfffffff1;
        afficherInfo(number);
    }

    Snackbar snackbar;
    public void afficherInfo(long number) {
        boolean ouvrir = false;
        Log.e("Tag", number + "");
        for (int i = 0; i < cards.length; i++){
            if (cards[i] == number){
                ouvrir = true;
            }
        }
        if (ouvrir){
            startActivity(new Intent(this, StoryActivity.class));
        }else {
            snackbar = Snackbar.make(this.getCurrentFocus(), R.string.card_no_reconu, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.dissmis, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });
        }
    }

    public void startAnim(){
        imageNfc.clearAnimation();
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.image_anim);
        imageNfc.setAnimation(animation);
        imageNfc.animate();
    }
}
