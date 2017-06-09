package derp.goforandroid;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.Gravity;

/**
 * Created by User on 05/06/2017.
 */
public class ACButton extends AppCompatButton {
    public ACButton(Context context) {
        super(context);
        initS();
    }

    public ACButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initS();
    }

    private void initS() {
        setTextColor(Color.WHITE);
        setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.nopad_editbutton, null));
        setGravity(Gravity.LEFT);
        setAllCaps(false);
        setPadding(5, 0, 5, 0);
        setHeight(getMinimumHeight()/2);
        setMinimumHeight(0);
    }
}
