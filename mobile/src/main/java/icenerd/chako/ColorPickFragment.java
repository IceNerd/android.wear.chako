package icenerd.chako;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.wearable.DataMap;

import org.json.JSONException;
import org.json.JSONObject;

import icenerd.chako.system.DataFragment;
import icenerd.chako.system.MobileConfig;
import icenerd.chako.view.RatioImage;
import icenerd.chako.view.CameraPreview;

public class ColorPickFragment extends DataFragment implements View.OnClickListener {

    private View mFragmentView;

    private boolean mCameraFront = false;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private RelativeLayout mCameraPreview;
    private RatioImage mPreviewWindow;

    private ImageView mCameraShutter;
    private ImageView mCameraSwitch;
    private View mColorize;
    private ImageView mIconColorize;

    @Override
    public View onCreateView ( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        mFragmentView = inflater.inflate( R.layout.fragment_colorpick, container, false );

        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[] { android.R.attr.colorPrimaryDark };
        int indexOfAttrTextSize = 0;
        TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
        int primaryColorDark = a.getColor(indexOfAttrTextSize, -1);
        a.recycle();

        mCameraPreview = (RelativeLayout) mFragmentView.findViewById(R.id.camera_preview);

        mPreviewWindow = (RatioImage) mFragmentView.findViewById(R.id.preview_window);
        mPreviewWindow.setAspectRatio(1, 1); // force square

        mCameraShutter = (ImageView) mFragmentView.findViewById(R.id.img_takepicture);
        mCameraShutter.setColorFilter(primaryColorDark);

        mCameraSwitch = (ImageView) mFragmentView.findViewById(R.id.img_choosecamera);
        mCameraSwitch.setColorFilter(primaryColorDark);

        mColorize = mFragmentView.findViewById(R.id.palette_colorize);
        mIconColorize = (ImageView)mFragmentView.findViewById(R.id.icon_colorize);

        mColorize.setBackgroundColor(primaryColorDark);
        mIconColorize.setColorFilter(blackORwhite(primaryColorDark));

        ((ImageView)mFragmentView.findViewById(R.id.img_clear)).setColorFilter(primaryColorDark);
        ((ImageView)mFragmentView.findViewById(R.id.img_done)).setColorFilter(primaryColorDark);

        mFragmentView.findViewById(R.id.action_choosecamera).setOnClickListener(this);
        mFragmentView.findViewById(R.id.action_takepicture).setOnClickListener(this);
        mFragmentView.findViewById(R.id.action_clear).setOnClickListener(this);
        mFragmentView.findViewById(R.id.action_done).setOnClickListener(this);

        chooseCamera();

        return mFragmentView;
    }

    public static int blackORwhite( int color ) {
        final int red = Color.red(color);
        final int green = Color.green(color);
        final int blue = Color.blue(color);

        // weighted distance in 3d RGB space
        // source: http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
        final int val = (int) Math.sqrt(
                red * red * .299
                + green * green * .587
                + blue 	* blue * .114
        );

        return ( val < 130 ? Color.WHITE : Color.BLACK );
    }

    @Override
    public void onPause() {
        super.onPause();
        //if( mPreview != null ) mPreview.stop();
        //if( mCameraPreview != null ) mCameraPreview.removeAllViews();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasCamera(getActivity())) {
            Toast toast = Toast.makeText(getActivity(), "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            getActivity().finish();
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                mCameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                mCameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void chooseCamera() {
        if (mCameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                mPicture = getPictureCallback();
                if( mPreview != null ) mPreview.stop();
                mCameraPreview.removeAllViews();
                mPreview = new CameraPreview( getActivity(), Camera.CameraInfo.CAMERA_FACING_BACK, CameraPreview.LayoutMode.FitToParent );
                mCameraPreview.addView(mPreview);
                mCameraSwitch.setImageResource(R.drawable.ic_camera_rear);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                mPicture = getPictureCallback();
                if( mPreview != null ) mPreview.stop();
                mCameraPreview.removeAllViews();
                mPreview = new CameraPreview( getActivity(), Camera.CameraInfo.CAMERA_FACING_FRONT, CameraPreview.LayoutMode.FitToParent );
                mCameraPreview.addView(mPreview);
                mCameraSwitch.setImageResource(R.drawable.ic_camera_front);
            }
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mPreview.stop();
                Bitmap bmp = BitmapFactory.decodeByteArray( data, 0, data.length );
                Matrix matrix = new Matrix();
                matrix.postRotate( 90 );
                if( mCameraFront ) {
                    matrix.preScale(1,-1);
                }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getHeight(), bmp.getHeight(), matrix, true);
                mPreviewWindow.setImageBitmap(bmp);
                mPreviewWindow.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int[] viewCoords = new int[2];
                        mPreviewWindow.getLocationOnScreen(viewCoords);
                        Matrix inverse = new Matrix();
                        mPreviewWindow.getImageMatrix().invert(inverse);
                        float[] touchPoint = new float[]{event.getX(), event.getY()};
                        inverse.mapPoints(touchPoint);
                        Bitmap bmp = ((BitmapDrawable) mPreviewWindow.getDrawable()).getBitmap();
                        int pixel = bmp.getPixel((int) touchPoint[0], (int) touchPoint[1]);
                        mColorize.setBackgroundColor(pixel);
                        mIconColorize.setColorFilter(blackORwhite(pixel));
                        Intent intent = getActivity().getIntent();
                        intent.putExtra( MobileConfig.KEY_PRIMARY_COLOR, pixel );
                        startActivity(intent);
                        return false;
                    }
                });
                Toast toast = Toast.makeText(getActivity(), "Now tap the image to pick a color.", Toast.LENGTH_LONG);
                toast.show();
            }
        };
        return picture;
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick( View view ) {
        int actionId = view.getId();

        if( actionId == R.id.action_takepicture ) {
            mPreview.takePicture(mPicture);
            mCameraPreview.setVisibility(View.GONE);
            // hide step one and reveal step 2
            mFragmentView.findViewById(R.id.step_one).setVisibility(View.GONE);
            mFragmentView.findViewById(R.id.step_two).setVisibility(View.VISIBLE);
        } else if( actionId == R.id.action_choosecamera ) {
            int camerasNumber = Camera.getNumberOfCameras();
            if (camerasNumber > 1) {
                chooseCamera();
            } else {
                Toast toast = Toast.makeText(getActivity(), "Sorry, this device has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }
        } else if( actionId == R.id.action_clear ) {
            mCameraPreview.setVisibility(View.VISIBLE);
            mPreviewWindow.setImageBitmap(null);
            mPreviewWindow.setOnTouchListener(null);
            mFragmentView.findViewById(R.id.step_one).setVisibility(View.VISIBLE);
            mFragmentView.findViewById(R.id.step_two).setVisibility(View.GONE);
            chooseCamera();
        } else if( actionId == R.id.action_done ) {
            int color = ((ColorDrawable)mColorize.getBackground()).getColor();
            Intent intent = getActivity().getIntent();
            intent.putExtra( "finish", true );

            JSONObject jsonColor = new JSONObject();
            try {
                jsonColor.put( "color", color );
                jsonColor.put( "created", System.currentTimeMillis());
                intent.putExtra( "new_color", jsonColor.toString() );
            } catch( JSONException err ) { Log.e( "ColorPickFragment", "Error in JSON" ); }

            startActivity(intent);
        }

    }

    @Override
    public void onDataReceived (DataMap config ) {
        if( config.containsKey(MobileConfig.KEY_PRIMARY_COLOR) ) {
            mColorize.setBackgroundColor(config.getInt(MobileConfig.KEY_PRIMARY_COLOR));
            mIconColorize.setColorFilter(blackORwhite(config.getInt(MobileConfig.KEY_PRIMARY_COLOR)));
        }
    }
}
