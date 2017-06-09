package derp.goforandroid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by User on 05/06/2017.
 */
public class ACScrollView extends ScrollView {
    public static int maxHeight = 100; // 100dp

    public ACScrollView(Context context, AttributeSet attra) {
        super(context, attra);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(Utils.dpToPx(getResources(), maxHeight), MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
