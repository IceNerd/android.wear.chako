package icenerd.chako.system;

import android.app.Fragment;

import com.google.android.gms.wearable.DataMap;

abstract public class DataFragment extends Fragment {
    abstract public void onDataReceived( DataMap config );
}
