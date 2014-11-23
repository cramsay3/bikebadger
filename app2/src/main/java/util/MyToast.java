package util;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by cramsay on 9/23/2014.
 */
 public  class MyToast {
    static public  void Show(Context c, String msg, int color) {
    final TypedArray styledAttributes = c.getTheme().obtainStyledAttributes(
            new int[] { android.R.attr.actionBarSize });
    int actionBarSize = (int) styledAttributes.getDimension(0, 0);
    styledAttributes.recycle();
        Toast t = Toast.makeText(c, msg, Toast.LENGTH_LONG);
        t.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, actionBarSize + 20);
        t.show();
    }
}
