package icenerd.chako;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;

import icenerd.chako.adapter.ColorAdapter;
import icenerd.chako.data.ColorModel;
import icenerd.chako.data.ColorORM;
import icenerd.chako.system.DataFragment;
import icenerd.chako.system.MobileConfig;

public class GalleryFragment extends DataFragment implements ColorAdapter.ColorAdapterListener {

    private View mFragmentView;

    private RecyclerView mRecyclerView;
    private ColorAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public View onCreateView ( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        mFragmentView = inflater.inflate( R.layout.fragment_gallery, container, false );

        mRecyclerView = (RecyclerView) mFragmentView.findViewById(R.id.list_colors);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        return mFragmentView;
    }

    @Override
    public void onDataReceived( DataMap config ) {
        if( BuildConfig.DEBUG ) Log.d( "GalleryFragment", config.toString() );
        final ArrayList<DataMap> raColorData = config.getDataMapArrayList(MobileConfig.KEY_COLOR_DATA);
        mAdapter = new ColorAdapter( getActivity(), raColorData );
        mAdapter.setColorAdapterListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void setPrimaryColor( int color ) {
        Intent intent = getActivity().getIntent();
        intent.putExtra( MobileConfig.KEY_PRIMARY_COLOR, color );
        intent.putExtra( "finish", true );
        startActivity(intent);
    }

    @Override
    public void updateColorDataMap( ArrayList<ColorModel> raModels ) {
        ArrayList<DataMap> colorData = new ArrayList<>();
        for( ColorModel model : raModels ) {
            colorData.add(ColorORM.toDataMap(model));
        }
        DataMap dmWrapper = new DataMap();
        dmWrapper.putDataMapArrayList(MobileConfig.KEY_COLOR_DATA, colorData );

        Intent intent = getActivity().getIntent();
        intent.putExtra( MobileConfig.KEY_COLOR_DATA, dmWrapper.toBundle() );
        startActivity(intent);
    }
}
