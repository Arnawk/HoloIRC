package com.fusionx.lightirc.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.fusionx.lightirc.util.UIUtils;

import org.holoeverywhere.widget.TextView;

public class RobotoThinTextView extends TextView {
    public RobotoThinTextView(Context context) {
        super(context);
        UIUtils.setRobotoThin(getContext(), this);
    }

    public RobotoThinTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        UIUtils.setRobotoThin(getContext(), this);
    }

    public RobotoThinTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        UIUtils.setRobotoThin(getContext(), this);
    }
}