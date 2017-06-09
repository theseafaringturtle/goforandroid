package derp.goforandroid;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * Created by User on 30/04/2017.
 */

public class PrefsActivity extends PreferenceActivity {
    @Override
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.headers_preference, target);
        PreferenceManager.setDefaultValues(this,R.xml.fragment_preference,true);
    }

    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return PrefsFragment.class.getName().equals(fragmentName);
    }
}
