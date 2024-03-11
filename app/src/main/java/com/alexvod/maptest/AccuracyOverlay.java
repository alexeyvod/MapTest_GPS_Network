package com.alexvod.maptest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class AccuracyOverlay extends Overlay {

    private Paint paint = new Paint();
    private Paint accuracyPaint = new Paint();
    private GeoPoint location;
    private final Point screenCoords = new Point();
    private float accuracy = 0;
    private int _BgColor;

    public AccuracyOverlay(GeoPoint location, float accuracyInMeters, int BgColor) {
        super();

        this.location = location;
        this.accuracy = accuracyInMeters;
        this.accuracyPaint.setStrokeWidth(2);
        this.accuracyPaint.setColor(Color.BLUE);
        this.accuracyPaint.setAntiAlias(true);
        this._BgColor = BgColor;
    }

    @Override
    public void onDetach(MapView view){
        paint = null;
        accuracyPaint = null;
    }

    @Override
    public void draw(final Canvas c, final MapView map, final boolean shadow) {

        if (shadow) {
            return;
        }

        if (location != null) {
            final Projection pj = map.getProjection();
            pj.toPixels(location, screenCoords);

            if (accuracy > 0) {  //Set this to a minimum pixel size to hide if accuracy high enough
                final float accuracyRadius = pj.metersToEquatorPixels(accuracy) * 2;

                /* Draw the inner shadow. */
                accuracyPaint.setAntiAlias(false);
                accuracyPaint.setStyle(Paint.Style.FILL);
                accuracyPaint.setColor(_BgColor);
                accuracyPaint.setAlpha(20);
                c.drawCircle(screenCoords.x, screenCoords.y, accuracyRadius, accuracyPaint);

                /* Draw the edge. */
                accuracyPaint.setAntiAlias(true);
                accuracyPaint.setStyle(Paint.Style.STROKE);
                accuracyPaint.setColor(_BgColor);
                accuracyPaint.setAlpha(150);
                c.drawCircle(screenCoords.x, screenCoords.y, accuracyRadius, accuracyPaint);
            }
        }
    }
}