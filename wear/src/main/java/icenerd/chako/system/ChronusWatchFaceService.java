package icenerd.chako.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import icenerd.chako.BuildConfig;

abstract public class ChronusWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "ChronusWatchFaceService";

    private static final long NORMAL_UPDATE_RATE_MS = 500;
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    protected static final float TWO_PI = (float) Math.PI * 2f;
    protected static final int DEFAULT_R = 51;
    protected static final int DEFAULT_G = 153;
    protected static final int DEFAULT_B = 255;
    protected static final int ALPHA_SOLID = 255;
    protected static final int ALPHA_OPAQUE = 158;

    private int mColorPrimary = Color.WHITE;

    public ChronusWatchFaceService() {
        super();
        mColorPrimary = Color.argb( ALPHA_SOLID, DEFAULT_R, DEFAULT_G, DEFAULT_B );
    }

    public static int blackORwhite( int color ) {
        final int red = Color.red(color);
        final int green = Color.green( color );
        final int blue = Color.blue( color );

        // weighted distance in 3d RGB space
        // source: http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
        final int val = (int) Math.sqrt(
                red * red * .299
                        + green * green * .587
                        + blue 	* blue * .114
        );

        return ( val < 130 ? Color.WHITE : Color.BLACK );
    }

    abstract protected class ChronusEngine extends CanvasWatchFaceService.Engine implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        protected Calendar mCalendar;
        protected int mColorBackground;
        protected Paint mPaintPrimary;
        protected Paint mPaintSecondary;

        static final int MSG_UPDATE_TIME = 0;
        private long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;
        /** Handler to update the time periodically in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:

                            if( BuildConfig.DEBUG ) Log.v(TAG, "updating time");

                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(ChronusWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        protected boolean mLowBitAmbient;
        protected boolean mbMute;
        protected boolean mRegisteredTimeZoneReceiver = false;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onAmbientModeChanged( boolean inAmbientMode ) {
            super.onAmbientModeChanged(inAmbientMode);

            if( BuildConfig.DEBUG ) Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

            if( mLowBitAmbient ) {
                mPaintPrimary.setAntiAlias(!inAmbientMode);
                mPaintSecondary.setAntiAlias(!inAmbientMode);
            }
            if( mLowBitAmbient && inAmbientMode ) {
                setColor( Color.WHITE );
            } else {
                setColor(mColorPrimary);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onCreate( SurfaceHolder holder ) {
            super.onCreate(holder);
            mCalendar = Calendar.getInstance();
            setColor(mColorPrimary);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onInterruptionFilterChanged( int interruptionFilter ) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);
            if( mbMute != inMuteMode ) {
                mbMute = inMuteMode;
                mColorBackground = Color.argb( (mbMute ? ALPHA_OPAQUE : ALPHA_SOLID), Color.red(mColorBackground), Color.green(mColorBackground), Color.blue(mColorBackground));
                mPaintPrimary.setAlpha(mbMute ? ALPHA_OPAQUE : ALPHA_SOLID);
                mPaintSecondary.setAlpha(mbMute ? ALPHA_OPAQUE : ALPHA_SOLID);
                invalidate();
            }
        }
            private void setInteractiveUpdateRateMs(long updateRateMs) {
                if (updateRateMs == mInteractiveUpdateRateMs) {
                    return;
                }
                mInteractiveUpdateRateMs = updateRateMs;

                // Stop and restart the timer so the new update rate takes effect immediately.
                if (shouldTimerBeRunning()) {
                    updateTimer();
                }
            }

        @Override
        public void onPropertiesChanged( Bundle properties ) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            if( BuildConfig.DEBUG ) Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if( BuildConfig.DEBUG ) Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if( visible ) {
                mGoogleApiClient.connect();
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
            updateTimer();
        }

        private void registerReceiver() {
            if( mRegisteredTimeZoneReceiver ) { return; }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            ChronusWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if( !mRegisteredTimeZoneReceiver ) { return; }
            mRegisteredTimeZoneReceiver = false;
            ChronusWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if( BuildConfig.DEBUG ) Log.d(TAG, "updateTimer");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        protected void setColor( int colorPrimary ) {
            final int colorContrast = blackORwhite( colorPrimary );
            if( colorContrast == Color.WHITE ) { // the primary color should be backgrounded and the timepieces will be white
                mColorBackground = colorPrimary;
                mPaintPrimary = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintPrimary.setColor(Color.WHITE);
                mPaintSecondary = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintSecondary.setColor(Color.WHITE);
            } else {
                mColorBackground = Color.BLACK;
                mPaintPrimary = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintPrimary.setColor(colorPrimary);
                mPaintSecondary = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintSecondary.setColor(Color.WHITE);
            }
        }

        @Override // DataApi.DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                    continue;
                }

                DataItem dataItem = dataEvent.getDataItem();
                if(!dataItem.getUri().getPath().equals(WearConfig.PATH_TO_WEAR)) {
                    continue;
                }

                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                if( config.containsKey( WearConfig.KEY_PRIMARY_COLOR ) ) {
                    mColorPrimary = config.getInt(WearConfig.KEY_PRIMARY_COLOR);
                    setColor(mColorPrimary);
                } else {
                    setColor(mColorPrimary);
                }

                invalidate();
                if( BuildConfig.DEBUG ) Log.d(TAG, "Config DataItem updated:" + config);
            }
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected: " + connectionHint);
            Wearable.DataApi.addListener(mGoogleApiClient, ChronusEngine.this);
            //updateConfigDataItemAndUiOnStartup();
            WearConfig.fetchConfigDataMap(mGoogleApiClient,
                    new WearConfig.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            WearConfig.putConfigDataItem(mGoogleApiClient, startupConfig);
                            if (startupConfig.containsKey(WearConfig.KEY_PRIMARY_COLOR)) {
                                mColorPrimary = startupConfig.getInt(WearConfig.KEY_PRIMARY_COLOR);
                                setColor(mColorPrimary);
                                invalidate();
                            } else {
                                setColor(mColorPrimary);
                                invalidate();
                            }
                        }
                    }
            );
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if( BuildConfig.DEBUG ) Log.d(TAG, "onConnectionSuspended: " + cause);
        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if( BuildConfig.DEBUG ) Log.d(TAG, "onConnectionFailed: " + result);
        }
    }
}
