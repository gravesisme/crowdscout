package com.dpg.crowdscout.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.dpg.crowdscout.R;
import com.google.common.base.Preconditions;

import java.util.Arrays;

public class TitleView extends TextView {
    private final Rect m_clipBounds = new Rect();
    private final Paint m_strikeThroughPaint = new Paint();
    private int m_strikeThroughColor = 0x00000000;

    private static char[] s_typechars =
            new char[]{'%', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'n', 'o', 's', 't', 'x'};

    public TitleView(Context context) {
        super(context);
    }

    public TitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO: Implement
        /*
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.TitleView);
        setStrikeThroughColor(array.getColor(R.styleable.TitleView_strikethroughColor, 0x0));
        setStrikeThroughStrokeWidth(array.getDimensionPixelSize(R.styleable.TitleView_strikethroughStrokeWidth, 1));
        array.recycle();
        */
    }

    /**
     * Sets the stroke color. If 0, the strikethrough line is not drawn.
     */
    public void setStrikeThroughColor(int color) {
        m_strikeThroughColor = color;
        if (color != 0) {
            m_strikeThroughPaint.setColor(color);
        }
    }

    /**
     * Sets the stroke width, in pixels. Defaults to 1.
     */
    public void setStrikeThroughStrokeWidth(int width) {
        m_strikeThroughPaint.setStrokeWidth(width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (m_strikeThroughColor != 0 && getText() != null) {
            canvas.getClipBounds(m_clipBounds);
            int y = m_clipBounds.height() / 2;

            float textWidth = getPaint().measureText(getText().toString());

            int width = m_clipBounds.width();
            int textCenter = width / 2 - (getPaddingRight() - getPaddingLeft()) / 2;

            canvas.drawLine(getPaddingLeft(), y,
                    textCenter - textWidth / 2 - getCompoundDrawablePadding(), y, m_strikeThroughPaint);
            canvas.drawLine(textCenter + textWidth / 2 + getCompoundDrawablePadding(), y,
                    m_clipBounds.width() - getPaddingRight(), y, m_strikeThroughPaint);
        }
    }

    /**
     * Formats a string with the given format string and a single argument, making
     * the formatted argument bold using the HERE Bold typeface, instead of faux bold.
     * @param formatString the format string, e.g. "%1$s places"
     * @param formatArgument the argument to replace the format arguments with.
     */
    public Spannable formatText(@NonNull String formatString, @NonNull Object formatArgument) {
        Preconditions.checkNotNull(formatArgument);

        int tokenStart = formatString.indexOf('%');
        Preconditions.checkState(tokenStart != -1);

        int tokenEnd = tokenStart + 1;

        for (int i = tokenStart + 1; i < formatString.length(); ++i) {
            char c = Character.toLowerCase(formatString.charAt(i));
            if (Arrays.binarySearch(s_typechars, c) >= 0) {
                tokenEnd = i;
                break;
            }
        }

        CharSequence token = formatString.subSequence(tokenStart, tokenEnd + 1);
        String formattedString = String.format(formatString, formatArgument);

        SpannableStringBuilder builder = new SpannableStringBuilder(formattedString);
        Object span = new TypefaceSpan("sans-serif");

        int delta = formattedString.length() - formatString.length();
        int formattedTokenLength = token.length() + delta;

        builder.setSpan(span, tokenStart, tokenStart + formattedTokenLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    /**
     * A convenience method to set text on this view using {@link #formatText(String, Object)}.
     * @param formatString the format string with a single formatting field.
     * @param formatArgument the format argument.
     */
    public void setFormattedText(@NonNull String formatString, @NonNull Object formatArgument) {
        setText(formatText(formatString, formatArgument));
    }
}
