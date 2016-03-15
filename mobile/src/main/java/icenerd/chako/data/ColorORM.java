package icenerd.chako.data;

import com.google.android.gms.wearable.DataMap;

public class ColorORM {

    public static final String COL_COLOR = "color";
    public static final String COL_TITLE = "title";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_MODIFED_AT = "modified_at";

    public static DataMap toDataMap( ColorModel model ) {
        DataMap dmReturn = new DataMap();

        dmReturn.putInt(COL_COLOR, model.color);
        dmReturn.putString(COL_TITLE, model.title);
        dmReturn.putLong(COL_CREATED_AT, model.created_at);
        dmReturn.putLong(COL_MODIFED_AT, model.modified_at);

        return dmReturn;
    }

}
