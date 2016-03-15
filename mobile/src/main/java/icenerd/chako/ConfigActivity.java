package icenerd.chako;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import icenerd.chako.data.ColorModel;
import icenerd.chako.data.ColorORM;
import icenerd.chako.system.DataFragment;
import icenerd.chako.system.MobileConfig;
import icenerd.chako.view.SlidingTabLayout;

public class ConfigActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataApi.DataItemResult> {

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;
    private DataMap mDataMap;

    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chronus_config);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new ChronusPagerAdapter());
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if( BuildConfig.DEBUG ) Log.d( "ConfigActivity", "onConnected: " + connectionHint + " nodeId: " + mPeerId );
        if (mPeerId == null) displayNoConnectedDeviceDialog();
        else {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme("wear").path(MobileConfig.PATH_TO_WEAR).authority(mPeerId).build();
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
        }
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            mDataMap = dataMapItem.getDataMap();
            if( mDataMap == null ) mDataMap = new DataMap();

            DataFragment fragGallery = (DataFragment)getFragmentManager().findFragmentById(R.id.fragment_gallery);
            DataFragment fragColor = (DataFragment)getFragmentManager().findFragmentById(R.id.fragment_colorpick);
            fragGallery.onDataReceived( mDataMap );
            fragColor.onDataReceived( mDataMap );
        }
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if( BuildConfig.DEBUG ) Log.d("ConfigActivity", "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if( BuildConfig.DEBUG ) Log.d("ConfigActivity", "onConnectionFailed: " + result);
    }

    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.ndc_title);
        String okText = getResources().getString(R.string.dialog_ok);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    class ChronusPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject( View view, Object obj ) {
            return obj == view;
        }

        @Override
        public CharSequence getPageTitle( int position ) {
            return ( position == 0 ? "New Color" : "Gallery" );
        }

        @Override
        public Object instantiateItem( ViewGroup container, int position ) {
            View view;
            if( position == 0 ) {
                view = getLayoutInflater().inflate(R.layout.page_colorpicker, container, false);
            } else {
                view = getLayoutInflater().inflate(R.layout.page_gallery, container, false);
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem( ViewGroup container, int position, Object object ) {
            container.removeView((View) object);
        }

    }

    @Override
    protected void onNewIntent( Intent intent ) {
        boolean bSendConfigUpdate = false;
        if( intent.hasExtra(MobileConfig.KEY_PRIMARY_COLOR) ) {
            mDataMap.putInt(MobileConfig.KEY_PRIMARY_COLOR, intent.getIntExtra(MobileConfig.KEY_PRIMARY_COLOR, Color.WHITE));
            bSendConfigUpdate = true;
        }
        if( intent.hasExtra(MobileConfig.KEY_COLOR_DATA) ) {
            DataMap dmWrapper = DataMap.fromBundle(intent.getBundleExtra(MobileConfig.KEY_COLOR_DATA));
            if( dmWrapper.containsKey(MobileConfig.KEY_COLOR_DATA) ) {
                mDataMap.putDataMapArrayList(MobileConfig.KEY_COLOR_DATA, dmWrapper.getDataMapArrayList(MobileConfig.KEY_COLOR_DATA));
                bSendConfigUpdate = true;
            }
        }
        if( intent.hasExtra("new_color") ) {
            if( BuildConfig.DEBUG ) Log.d( "ConfigActivity", "Trying to save new color");
            ArrayList<DataMap> dmraColors;
            if( mDataMap.containsKey(MobileConfig.KEY_COLOR_DATA) ) {
                dmraColors = mDataMap.getDataMapArrayList(MobileConfig.KEY_COLOR_DATA);
            } else {
                dmraColors = new ArrayList<>();
            }
            ColorModel model = new ColorModel();
            model.color = intent.getIntExtra("new_color", 0);
            model.created_at = System.currentTimeMillis();
            model.modified_at = model.created_at;
            dmraColors.add(ColorORM.toDataMap(model));
            mDataMap.putDataMapArrayList(MobileConfig.KEY_COLOR_DATA, dmraColors);
            bSendConfigUpdate = true;
        }

        if( bSendConfigUpdate ) sendConfigUpdateMessage();
        if( intent.hasExtra("finish") ) { finish(); }
    }
    private void sendConfigUpdateMessage() {
        if( mPeerId != null ) {
            byte[] rawData = mDataMap.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, MobileConfig.PATH_TO_WEAR, rawData);
            if( BuildConfig.DEBUG ) Log.d( "ConfigActivity", "Sent new watch face config");
        } else {
            if( BuildConfig.DEBUG ) Log.d( "ConfigActivity", "Config message not sent");
        }
    }

}
