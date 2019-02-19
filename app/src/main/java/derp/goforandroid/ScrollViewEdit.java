package derp.goforandroid;

/**
 * Created by User on 06/09/2018.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
public class ScrollViewEdit extends ScrollView {
    public static PrettifyHighlighter highlighter = null;

    public ScrollViewEdit(Context context, AttributeSet attra) {
        super(context, attra);
    }

    @Override
    protected void onScrollChanged(int x,int y,int oldx,int oldy){
        super.onScrollChanged(x,y,oldx,oldy);//todo fix dynamic highlighting or remove it
        /*if(highlighter!=null) {
            highlighter.highlight(((EditCode)((MainActivity)getContext()).findViewById(R.id.inputView)));//not enough casts
        }*/
    }
}
