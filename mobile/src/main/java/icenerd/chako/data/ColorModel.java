package icenerd.chako.data;

import com.google.android.gms.wearable.DataMap;

public class ColorModel {
    public int color;
    public String title;
    public long created_at;
    public long modified_at;

    public ColorModel() {}
    public ColorModel( DataMap dataMap ) {
        color = dataMap.getInt( ColorORM.COL_COLOR, 0 );
        title = dataMap.getString( ColorORM.COL_TITLE, null );
        created_at = dataMap.getLong( ColorORM.COL_CREATED_AT, 0 );
        modified_at = dataMap.getLong( ColorORM.COL_MODIFED_AT, 0 );
    }
}
