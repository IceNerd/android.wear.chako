package icenerd.chako.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.DateFormat;
import java.util.ArrayList;

import icenerd.chako.ColorPickFragment;
import icenerd.chako.R;
import icenerd.chako.data.ColorModel;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> implements View.OnClickListener {
    private final ArrayList<ColorModel> mraModels;

    private PopupWindow mPopupWindow;

    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;

    public interface ColorAdapterListener {
        void setPrimaryColor( int Color );
        void updateColorDataMap( ArrayList<ColorModel> raModels );
    }
    private ColorAdapterListener mListener;
    public void setColorAdapterListener( final ColorAdapterListener listener ) {
        mListener = listener;
    }

    public ColorAdapter( Context ctx, ArrayList<DataMap> raColorData ) {
        mraModels = new ArrayList<>();
        if( raColorData != null ) {
            for (DataMap dmap : raColorData) {
                mraModels.add(new ColorModel(dmap));
            }
        }
        mDateFormat = android.text.format.DateFormat.getDateFormat(ctx);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
    }

    @Override
    public int getItemCount() { return mraModels.size(); }

    @Override
    public void onClick( View view ) {
        int viewId = view.getId();
        if( viewId == R.id.img_more ) {
            LayoutInflater layoutInflater = LayoutInflater.from(view.getContext());
            View popupView = layoutInflater.inflate(R.layout.color_popup, null, false);
            popupView.findViewById(R.id.txt_set_title).setOnClickListener(this);
            popupView.findViewById(R.id.txt_remove).setOnClickListener(this);
            popupView.findViewById(R.id.txt_set_title).setTag(view.getTag());
            popupView.findViewById(R.id.txt_remove).setTag(view.getTag());
            if( mPopupWindow != null ) mPopupWindow.dismiss();
            mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.showAsDropDown(view, -view.getWidth(), 0);
        } else if( viewId == R.id.img_color || viewId == R.id.txt_color_code ) {
            int color = ((ColorDrawable)((ViewHolder)view.getTag()).mImageView.getBackground()).getColor();
            if( mListener != null ) mListener.setPrimaryColor(color);
        } else if( viewId == R.id.txt_set_title ) {
            if( mPopupWindow != null ) mPopupWindow.dismiss();
            final int position = ((ViewHolder)view.getTag()).mPosition;
            final EditText input = new EditText(view.getContext());

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.popup_set_title);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                        final String strTitle = input.getText().toString();
                        mraModels.get(position).title = strTitle;
                        notifyItemChanged(position);
                        if( mListener != null ) mListener.updateColorDataMap( mraModels );
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        } else if( viewId == R.id.txt_remove ) {
            if( mPopupWindow != null ) mPopupWindow.dismiss();
            final int position = ((ViewHolder)view.getTag()).mPosition;

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.popup_remove_color);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mraModels.remove(position);
                    notifyItemRemoved(position);
                    if( mListener != null ) mListener.updateColorDataMap( mraModels );
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public TextView mTextCodeView;
        public ImageView mImageView;
        public ImageView mImageMore;
        public int mPosition = -1;

        public ViewHolder( View v ) {
            super(v);
            mTextView = (TextView)v.findViewById(R.id.txt_color);
            mTextCodeView = (TextView)v.findViewById(R.id.txt_color_code);
            mImageView = (ImageView)v.findViewById(R.id.img_color);
            mImageMore = (ImageView)v.findViewById(R.id.img_more);
        }
    }

    @Override
    public ColorAdapter.ViewHolder onCreateViewHolder( ViewGroup parent, int viewType ) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ColorModel model = mraModels.get(position);
        holder.mPosition = position;
        holder.mTextCodeView.setTag(holder);
        holder.mImageView.setTag(holder);
        holder.mImageMore.setTag(holder);

        if( model.title != null && !model.title.isEmpty() ) {
            holder.mTextView.setText( model.title );
        } else {
            final String strCreatedDate = mDateFormat.format(model.created_at);
            final String strCreatedTime = mTimeFormat.format(model.created_at);
            final String noTitle = String.format("%s %s", strCreatedDate, strCreatedTime);
            holder.mTextView.setText(noTitle);
        }
        holder.mTextCodeView.setText(String.format("#%06X", 0xFFFFFF & model.color));
        holder.mTextCodeView.setTextColor(ColorPickFragment.blackORwhite(model.color));
        holder.mImageView.setBackgroundColor(model.color);

        holder.mTextCodeView.setOnClickListener(this);
        holder.mImageView.setOnClickListener(this);
        holder.mImageMore.setOnClickListener(this);
    }
}
