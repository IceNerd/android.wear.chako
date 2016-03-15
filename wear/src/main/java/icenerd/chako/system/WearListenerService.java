package icenerd.chako.system;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

import icenerd.chako.BuildConfig;

public class WearListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {
        if( BuildConfig.DEBUG ) Log.d( "WListener", "Message received" );
        if( !messageEvent.getPath().equals(WearConfig.PATH_TO_WEAR) ) return;
        if( BuildConfig.DEBUG ) Log.d( "WListener", "Message accepted" );

        byte[] rawData = messageEvent.getData();
        DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);
        if( BuildConfig.DEBUG ) Log.d("WListener", "Received watch face config message: " + configKeysToOverwrite);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }
        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(100, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e("WListener", "Failed to connect to GoogleApiClient.");
                return;
            }
        }

        WearConfig.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if( BuildConfig.DEBUG ) Log.d("WListener", "onConnected: " + connectionHint);
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if( BuildConfig.DEBUG ) Log.d("WListener", "onConnectionSuspended: " + cause);
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if( BuildConfig.DEBUG ) Log.d("WListener", "onConnectionFailed: " + result);
    }
}
