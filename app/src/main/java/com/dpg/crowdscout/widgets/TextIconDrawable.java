package com.dpg.crowdscout.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;

import com.dpg.crowdscout.R;

public class TextIconDrawable extends Drawable {
    public static final int DEFAULT_BG_RESOURCE_ID = R.drawable.icon_placeholder;
    public static final int DEFAULT_TEXT_COLOR = Color.parseColor("#ffffff");
    public static final int DEFAULT_TEXT_RESOURCE_ID = R.string.no_photos;

    private final TextPaint m_textPaint;

    private Drawable m_backgroundDrawable;
    private String m_text;
    private int m_backgroundColor;
    private int m_textSize;
    private int m_borderPadding;
    private int m_textPadding;
    private int m_textColor = DEFAULT_TEXT_COLOR;

    public TextIconDrawable(final Context context) {
        // Get the background
        m_backgroundDrawable = context.getResources().getDrawable(DEFAULT_BG_RESOURCE_ID);

        // Get the text size
        m_textSize = context.getResources().getDimensionPixelSize(R.dimen.text_icon_drawable_text_size);

        // Get the padding between edge and border
        m_borderPadding = context.getResources().getDimensionPixelOffset(R.dimen.text_icon_drawable_border_padding);

        // Get the padding between border and text
        m_textPadding = context.getResources().getDimensionPixelOffset(R.dimen.text_icon_drawable_text_padding);

        // Set background color
        m_backgroundColor = context.getResources().getColor(R.color.grey3);

        // Set default text
        m_text = context.getResources().getString(DEFAULT_TEXT_RESOURCE_ID);

        // Initialize text paint
        m_textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        m_textPaint.setTextSize(m_textSize);
        m_textPaint.setTextAlign(Paint.Align.LEFT);
        m_textPaint.setColor(m_textColor);
        m_textPaint.setDither(true);
        m_textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        m_textPaint.density = context.getResources().getDisplayMetrics().density;
    }

    public void setBackgroundColor(int backgroundColor) {
        m_backgroundColor = backgroundColor;
        invalidateSelf();
    }

    public void setTextSize(int textSize) {
        m_textSize = textSize;
        m_textPaint.setTextSize(m_textSize);
        invalidateSelf();
    }

    public void setTextColor(int textColor) {
        m_textColor = textColor;
        m_textPaint.setColor(m_textColor);
        invalidateSelf();
    }

    public void setText(String text) {
        m_text = text;
        invalidateSelf();
    }

    public void setBackgroundDrawable(Drawable backgroundDrawable) {
        m_backgroundDrawable = backgroundDrawable;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        m_textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        m_textPaint.setColorFilter(cf);
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect canvasBounds = getBounds();

        canvas.drawColor(m_backgroundColor);

        final Rect drawableBounds = new Rect(
                canvasBounds.left + m_borderPadding,
                canvasBounds.top + m_borderPadding,
                canvasBounds.right - m_borderPadding,
                canvasBounds.bottom - m_borderPadding
        );

        m_backgroundDrawable.setBounds(drawableBounds);

        m_backgroundDrawable.draw(canvas);

        final float x = drawableBounds.left + m_textPadding;
        final float y = drawableBounds.bottom - m_textPadding;
        canvas.drawText(m_text, x, y, m_textPaint);
    }

    @Override
    public int getOpacity() {
        return 255;
    }

    public int getBackgroundColor() {
        return m_backgroundColor;
    }

    public String getText() {
        return m_text;
    }

    public int getTextSize() {
        return m_textSize;
    }

    public int getTextColor() {
        return m_textColor;
    }

    /**
     * Helper that will create an AddPhoto bitmap with a custom background drawable - and bg color -
     * using the specified text and dimensions
     */
    public static Bitmap createBitmap(@NonNull Context context, @Nullable Drawable backgroundDrawable, int backgroundColor, String text, int width, int height) {
        final TextIconDrawable textIconDrawable = new TextIconDrawable(context);
        textIconDrawable.setBackgroundDrawable(backgroundDrawable);
        textIconDrawable.setBackgroundColor(backgroundColor);
        textIconDrawable.setText(text);
        textIconDrawable.setBounds(0, 0, width, height);

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        final Canvas bitmapCanvas = new Canvas(bitmap);

        textIconDrawable.draw(bitmapCanvas);

        return bitmap;
    }

    /**
     * Helper that will create an AddPhoto bitmap with a custom background drawable
     * using the specified text and dimensions
     */
    public static Bitmap createBitmap(@NonNull Context context, @Nullable Drawable backgroundDrawable, String text, int width, int height) {
        int defaultBgColor = context.getResources().getColor(R.color.grey3);
        return createBitmap(context, backgroundDrawable, defaultBgColor, text, width, height);
    }

    /**
     * Helper that will create an AddPhoto bitmap using the specified text and dimensions
     */
    public static Bitmap createBitmap(@NonNull Context context, @NonNull String text, int width, int height) {
        final Drawable defaultBgDrawable = context.getResources().getDrawable(DEFAULT_BG_RESOURCE_ID);
        return createBitmap(context, defaultBgDrawable, text, width, height);
    }
}
