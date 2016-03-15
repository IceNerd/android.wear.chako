package icenerd.chako.system;

import android.content.Intent;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import icenerd.chako.BuildConfig;
import icenerd.chako.ConfigActivity;

public class MobileListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if( BuildConfig.DEBUG ) Log.d( "MListener", "Message received" );
        if( !messageEvent.getPath().equals(MobileConfig.PATH_TO_MOBILE) ) return;
        if( BuildConfig.DEBUG ) Log.d( "MListener", "Message accepted" );

        //byte[] rawData = messageEvent.getData();
        String nodeId = messageEvent.getSourceNodeId();

        Intent intent = new Intent(this, ConfigActivity.class);
        intent.putExtra( WatchFaceCompanion.EXTRA_PEER_ID, nodeId );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
