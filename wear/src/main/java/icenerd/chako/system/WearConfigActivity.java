package icenerd.chako.system;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;

import icenerd.chako.BuildConfig;
import icenerd.chako.R;

public class WearConfigActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, WearableListView.ClickListener, WearableListView.OnScrollListener, ResultCallback<DataApi.DataItemResult> {
    private static final String TAG = "WearConfigActivity";

    private DataMap mDataMap;

    private String mPeerId;
    private GoogleApiClient mGoogleApiClient;
    private TextView mHeader;
    private WearableListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_config);

        mHeader = (TextView) findViewById(R.id.header);
        mListView = (WearableListView) findViewById(R.id.color_picker);
        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        mListView.setHasFixedSize(true);
        mListView.setClickListener(this);
        mListView.addOnScrollListener(this);
        mListView.setAdapter(new ColorDataAdapter(null));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();
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

    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) viewHolder;
        if( viewHolder.getAdapterPosition() == 0 ) {
            sendMessageToOpenActivity();
        } else {
            DataMap datamap = new DataMap();
            datamap.putInt(WearConfig.KEY_PRIMARY_COLOR, colorItemViewHolder.mColorItem.getColor());
            WearConfig.overwriteKeysInConfigDataMap(mGoogleApiClient, datamap);
        }
        finish();
    }
    private void sendMessageToOpenActivity() {
        if (mPeerId != null) {
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(NodeApi.GetConnectedNodesResult result) {
                            for (Node node : result.getNodes()) {
                                Log.d(TAG, "Node " + node.getId() + " is connected");
                                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), WearConfig.PATH_TO_MOBILE, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(MessageApi.SendMessageResult result) {
                                        if (result.getStatus().isSuccess()) {
                                            if (BuildConfig.DEBUG)
                                                Log.d(TAG, "Sent message to connected node");
                                        } else {
                                            if (BuildConfig.DEBUG) Log.d(TAG, "Message not sent");
                                        }
                                    }
                                });
                            }
                        }
                    });
        } else {
            if( BuildConfig.DEBUG ) Log.d(TAG, "Message not sent");
        }
    }

    @Override // WearableListView.ClickListener
    public void onTopEmptyRegionClick() {
    }

    @Override // WearableListView.OnScrollListener
    public void onScroll(int scroll) {
    }

    @Override // WearableListView.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeader.setTranslationY(newTranslation);
    }

    @Override // WearableListView.OnScrollListener
    public void onScrollStateChanged(int scrollState) {
    }

    @Override // WearableListView.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {
    }

    @Override
    public void onResult( DataApi.DataItemResult dataItemResult ) {
        if( BuildConfig.DEBUG ) Log.d(TAG, "Received Result" + dataItemResult.getStatus().isSuccess() + " ::: " + dataItemResult.getDataItem());
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            mDataMap = dataMapItem.getDataMap();
            if( mDataMap != null ) Log.d(TAG, mDataMap.toString());
        }

        JSONArray raColors = new JSONArray();
        if( mDataMap != null && mDataMap.containsKey(WearConfig.KEY_COLOR_JSON) ) {
            try {
                raColors = new JSONArray(mDataMap.getString(WearConfig.KEY_COLOR_JSON));
            } catch (JSONException e) {
            }
        }
        mListView.setAdapter(new ColorDataAdapter(raColors));
    }

    @Override
    public void onConnected(Bundle bundle) {
        if( BuildConfig.DEBUG ) Log.d(TAG, "Connected");
        Wearable.NodeApi.getLocalNode(mGoogleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult result) {
                        mPeerId = result.getNode().getId();
                        fetchData();
                    }
                });
    }
    private void fetchData() {
        if( BuildConfig.DEBUG ) Log.d(TAG, "fetching data");
        Uri.Builder builder = new Uri.Builder();
        Uri uri = builder.scheme("wear").path(WearConfig.PATH_TO_WEAR).authority(mPeerId).build();
        Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    private class ColorDataAdapter extends WearableListView.Adapter {

        private JSONArray mColorData;

        public ColorDataAdapter(JSONArray colorData) {
            mColorData = colorData;
        }

        @Override
        public ColorItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ColorItemViewHolder(new ColorItem(parent.getContext()));
        }

        @Override
        public void onBindViewHolder( WearableListView.ViewHolder holder, int position ) {
            ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) holder;
            if( position == 0 ) {
                colorItemViewHolder.mColorItem.setIcon(getResources().getString(R.string.exit_to_app), R.drawable.ic_exit_to_app);
            } else {
                JSONObject jsonObj = null;
                try {
                    jsonObj = mColorData.getJSONObject( position - 1 );
                    String strTitle = "color";
                    Format format = android.text.format.DateFormat.getDateFormat(getApplicationContext());
                    if( jsonObj.has("created") ) strTitle = format.format( jsonObj.getLong("created") );
                    if( jsonObj.has("title") ) strTitle = jsonObj.getString("title");
                    colorItemViewHolder.mColorItem.setColor( strTitle, jsonObj.getInt("color") );
                } catch (JSONException err) { Log.e( TAG, "JSON", err );}
            }

            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
            int colorPickerItemMargin = (int) getResources().getDimension(R.dimen.digital_config_color_picker_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if( position == 0 ) {
                layoutParams.setMargins( 0, colorPickerItemMargin, 0, 0 );
            } else if (position == ( mColorData != null ? mColorData.length() : 0 ) ) {
                layoutParams.setMargins( 0, 0, 0, colorPickerItemMargin );
            } else {
                layoutParams.setMargins( 0, 0, 0, 0 );
            }
            colorItemViewHolder.itemView.setLayoutParams( layoutParams );
        }

        @Override
        public int getItemCount() {
            return (( mColorData != null ? mColorData.length() : 0 ) + 1);
        }
    }

    /**
     * The layout of a color item including image and label.
     */
    private static class ColorItem extends LinearLayout implements
            WearableListView.OnCenterProximityListener {
        /**
         * The duration of the expand/shrink animation.
         */
        private static final int ANIMATION_DURATION_MS = 150;
        /**
         * The ratio for the size of a circle in shrink state.
         */
        private static final float SHRINK_CIRCLE_RATIO = .75f;

        private static final float SHRINK_LABEL_ALPHA = .5f;
        private static final float EXPAND_LABEL_ALPHA = 1f;

        private final TextView mLabel;
        private final CircledImageView mColor;

        private final float mExpandCircleRadius;
        private final float mShrinkCircleRadius;

        private final ObjectAnimator mExpandCircleAnimator;
        private final ObjectAnimator mExpandLabelAnimator;
        private final AnimatorSet mExpandAnimator;

        private final ObjectAnimator mShrinkCircleAnimator;
        private final ObjectAnimator mShrinkLabelAnimator;
        private final AnimatorSet mShrinkAnimator;

        public ColorItem(Context context) {
            super(context);
            View.inflate(context, R.layout.color_picker_item, this);

            mLabel = (TextView) findViewById(R.id.label);
            mColor = (CircledImageView) findViewById(R.id.color);

            mExpandCircleRadius = mColor.getCircleRadius();
            mShrinkCircleRadius = mExpandCircleRadius * SHRINK_CIRCLE_RATIO;

            mShrinkCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius", mExpandCircleRadius, mShrinkCircleRadius);
            mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha", EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
            mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mShrinkAnimator.playTogether(mShrinkCircleAnimator, mShrinkLabelAnimator);

            mExpandCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius", mShrinkCircleRadius, mExpandCircleRadius);
            mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha", SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
            mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mExpandAnimator.playTogether(mExpandCircleAnimator, mExpandLabelAnimator);
        }

        @Override
        public void onCenterPosition(boolean animate) {
            if( animate ) {
                mShrinkAnimator.cancel();
                if (!mExpandAnimator.isRunning()) {
                    mExpandCircleAnimator.setFloatValues(mColor.getCircleRadius(), mExpandCircleRadius);
                    mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                    mExpandAnimator.start();
                }
            } else {
                mExpandAnimator.cancel();
                mColor.setCircleRadius(mExpandCircleRadius);
                mLabel.setAlpha(EXPAND_LABEL_ALPHA);
            }
        }

        @Override
        public void onNonCenterPosition(boolean animate) {
            if (animate) {
                mExpandAnimator.cancel();
                if (!mShrinkAnimator.isRunning()) {
                    mShrinkCircleAnimator.setFloatValues(mColor.getCircleRadius(), mShrinkCircleRadius);
                    mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                    mShrinkAnimator.start();
                }
            } else {
                mShrinkAnimator.cancel();
                mColor.setCircleRadius(mShrinkCircleRadius);
                mLabel.setAlpha(SHRINK_LABEL_ALPHA);
            }
        }

        private void setColor( String title, int color ) {
            mLabel.setText(title);
            mColor.setImageResource(0);
            mColor.setCircleColor( color );
        }

        private void setIcon( String name, int resId ) {
            mLabel.setText(name);
            mColor.setImageResource(resId);
            mColor.setCircleColor( Color.BLACK );
        }

        private int getColor() {
            return mColor.getDefaultCircleColor();
        }
    }

    private static class ColorItemViewHolder extends WearableListView.ViewHolder {
        private final ColorItem mColorItem;

        public ColorItemViewHolder(ColorItem colorItem) {
            super(colorItem);
            mColorItem = colorItem;
        }
    }
}