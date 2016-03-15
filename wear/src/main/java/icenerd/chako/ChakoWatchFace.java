package icenerd.chako;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import java.util.Calendar;

import icenerd.chako.system.ChronusWatchFaceService;

public class ChakoWatchFace extends ChronusWatchFaceService {
    public ChakoWatchFace() { super(); }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends ChronusEngine {

        private Path mpathMinute;
        private Path mpathHour;

        private float mArcWidth;

        @Override
        public void onCreate( SurfaceHolder holder ) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(ChakoWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onSurfaceChanged( SurfaceHolder holder, int format, int width, int height ) {
            super.onSurfaceChanged(holder, format, width, height);

            int centerX = width/2;
            int centerY = height/2;
            float halfWidth = width/24;

            mpathMinute = new Path();
            mpathMinute.moveTo(centerX, centerY);
            mpathMinute.lineTo(centerX+halfWidth, centerY-(height/6));
            mpathMinute.lineTo(centerX - halfWidth, centerY - (height / 6));
            mpathMinute.lineTo(centerX, centerY);
            mpathMinute.close();
            mpathHour = new Path();
            mpathHour.moveTo(centerX, centerY);
            mpathHour.lineTo(centerX+halfWidth, centerY+(height/6));
            mpathHour.lineTo(centerX - halfWidth, centerY + (height / 6));
            mpathHour.lineTo(centerX, centerY);
            mpathHour.close();
            mArcWidth = centerX/6;
        }

        @Override
        public void onDraw( Canvas canvas, Rect bounds ) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            //mCalendar.set( 2015, 7, 22, 4, 20, 0 );
            int width = bounds.width();
            int height = bounds.height();
            float centerX = width / 2f;
            float centerY = height / 2f;

            canvas.drawColor(mColorBackground);

            float innerTickRadius = centerX - centerX/12;
            float outerTickRadius = centerX;
            for( int tickIndex = 1; tickIndex < 12; tickIndex++ ) { // intentionally skip the first
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mPaintSecondary);
            }

            float triangle_height = width/6;
            float baseRadius = centerX/6;

            float millis = mCalendar.get(Calendar.MILLISECOND);
            float seconds = mCalendar.get(Calendar.SECOND) + millis/1000f;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float hours = mCalendar.get(Calendar.HOUR) + (minutes / 60f);

            float hrRot = hours / 12f * TWO_PI;
            float minRot = minutes / 60f * TWO_PI;
            float hrDeg = hours/12f*360;
            float minDeg = minutes / 60f * 360;

            float dMinuteX = (float)Math.sin(minRot);
            float dMinuteY = (float)-Math.cos(minRot);
            float start_minute_X = dMinuteX * 2*baseRadius;
            float start_minute_Y = dMinuteY * 2*baseRadius;

            float dHourX = (float)Math.sin(hrRot);
            float dHourY = (float)-Math.cos(hrRot);
            float start_hour_X = dHourX * (triangle_height+ 3*baseRadius);
            float start_hour_Y = dHourY * (triangle_height+ 3*baseRadius);

            mPaintPrimary.setStyle(Paint.Style.FILL);
            canvas.save();
            canvas.translate(start_minute_X, start_minute_Y);
            canvas.rotate(minutes / 60f * 360, centerX, centerY);
            canvas.drawPath(mpathMinute, mPaintPrimary);
            canvas.restore();

            canvas.save();
            canvas.translate(start_hour_X, start_hour_Y);
            canvas.rotate(hours / 12f * 360, centerX, centerY);
            canvas.drawPath(mpathHour, mPaintPrimary);
            canvas.restore();

            mPaintPrimary.setStyle(Paint.Style.STROKE);
            mPaintPrimary.setStrokeCap(Paint.Cap.BUTT);
            mPaintPrimary.setStrokeWidth(mArcWidth);
            if( !isOdd((int)hours) ) {
                if( hrDeg > minDeg ) {
                    canvas.drawArc(
                            centerX - 3.5f * baseRadius,
                            centerY - 3.5f * baseRadius,
                            centerX + 3.5f * baseRadius,
                            centerY + 3.5f * baseRadius,
                            hrDeg-90,
                            360-(hrDeg-minDeg),
                            false, mPaintPrimary);
                } else {
                    canvas.drawArc(
                            centerX - 3.5f * baseRadius,
                            centerY - 3.5f * baseRadius,
                            centerX + 3.5f * baseRadius,
                            centerY + 3.5f * baseRadius,
                            minDeg-90,
                            360-(minDeg-hrDeg),
                            false, mPaintPrimary);
                }
            } else {
                if( hrDeg > minDeg ) {
                    canvas.drawArc(
                            centerX - 3.5f * baseRadius,
                            centerY - 3.5f * baseRadius,
                            centerX + 3.5f * baseRadius,
                            centerY + 3.5f * baseRadius,
                            minDeg-90,
                            hrDeg-minDeg,
                            false, mPaintPrimary);
                } else {
                    canvas.drawArc(
                            centerX - 3.5f * baseRadius,
                            centerY - 3.5f * baseRadius,
                            centerX + 3.5f * baseRadius,
                            centerY + 3.5f * baseRadius,
                            hrDeg-90,
                            minDeg-hrDeg,
                            false, mPaintPrimary);
                }
            }

            if( isVisible() && !isInAmbientMode() ) {

                outerTickRadius = baseRadius*2;
                canvas.save();
                canvas.rotate(minutes / 60f * 360, centerX, centerY);
                mPaintSecondary.setStyle(Paint.Style.FILL);
                if( isOdd((int)minutes) ) {
                    for (int tickIndex = 0; tickIndex < seconds; tickIndex++) {
                        float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                        float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                        float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                        canvas.drawCircle(centerX + outerX, centerY + outerY, baseRadius/12, mPaintSecondary);
                    }
                } else {
                    for( int tickIndex = 59; tickIndex > seconds; tickIndex--) {
                        float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                        float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                        float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                        canvas.drawCircle(centerX + outerX, centerY + outerY, baseRadius / 12, mPaintSecondary);
                    }
                }
                canvas.restore();
            }
        }

        private boolean isOdd( int val ) { return (val & 0x01) != 0; }
    }
}