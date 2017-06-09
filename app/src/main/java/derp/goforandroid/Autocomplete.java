package derp.goforandroid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatButton;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by User on 15/04/2017.
 */

public class Autocomplete {

    MainActivity activity;
    EditFragment frag;
    Handler ACTimer = new Handler();
    Runnable ACRunnable;
    Dirs mDirs;
    int offset = 0;
    String outFormat = "json";//"nice"fn22
    PopupWindow popupWindow;
    View popupView;
    EditCode inView;

    public Autocomplete(MainActivity activity, final EditFragment frag) {
        this.activity = activity;
        this.frag = frag;
        this.mDirs = activity.mDirs;
        ACRunnable = new Runnable() {
            @Override
            public void run() {
                saveTempFile();
                goCodeCmd();
            }
        };
        inView = (EditCode) activity.findViewById(R.id.inputView);
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        popupView = layoutInflater.inflate(R.layout.popup_autocomplete, null);
        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
    }

    private void goCodeCmd() {
        try {
            String[] command = {
                    mDirs.binDir + "gocode",
                    "-f=" + outFormat,
                    "--in=" + mDirs.filesDir + "autocomplete.go",
                    "autocomplete",
                    "" + offset
            };
            HashMap<String, String> envVars = new HashMap<>();
            envVars.put("GOROOT", mDirs.GOROOT);
            envVars.put("TMPDIR", mDirs.filesDir + File.separator + "tmp");
            envVars.put("GOPATH", mDirs.GOPATH);
            String wDir = mDirs.binDir;
            final String out = Utils.executeCommand(command, envVars, wDir);
            parseOutput(out);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void parseOutput(String out) {
        //&& out.substring(0,5).equals("Found")
        //String[] lines = out.split("\n");
        //displayOutput(lines);
        try {
            LinearLayout layout = (LinearLayout) popupView.findViewById(R.id.floatingACMenu);
            layout.removeAllViews();

            JSONArray base = new JSONArray(out);
            if (base.length() > 1) {
                JSONArray arr = base.getJSONArray(1);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject ACEntry = (JSONObject) arr.get(i);
                    String shortHint = ACEntry.getString("name");
                    if(shortHint.equals("PANIC")) break;
                    String longHint = ACEntry.getString("class") + " " + ACEntry.getString("name") + " " + ACEntry.getString("type");
                    layout.addView(newHintButton(shortHint, longHint));
                }
                popupWindow.showAtLocation(inView, Gravity.TOP,(int)getCursorX(inView),(int)getCursorY(inView));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private ACButton newHintButton(String shortHint, final String longHint) {
        final ACButton b = new ACButton(activity);
        b.setText(shortHint);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toAdd =  b.getText().toString();
                if(longHint.startsWith("func"))
                    toAdd+="(";
                inView.getEditableText().insert(inView.getSelectionStart(),toAdd);
                popupWindow.dismiss();
            }
        });
        b.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity, longHint, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        return b;
    }
    /*void displayNiceOutput(String[] lines){
        LinearLayout layout = (LinearLayout)popupView.findViewById(R.id.floatingACMenu);
        final EditCode inView = (EditCode) activity.findViewById(R.id.inputView);
        layout.removeAllViews();
        for(int i = 1; i<lines.length; i++){
            final ACButton b = new ACButton(activity);
            b.setText(lines[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    inView.getEditableText().insert(inView.getSelectionStart(),b.getText());
                    popupWindow.dismiss();
                }
            });
            layout.addView(b);
            popupWindow.showAtLocation(inView, Gravity.TOP,(int)getCursorX(inView),(int)getCursorY(inView));
        }
    }*/

    public float getCursorY(EditCode ec) {
        if (ec.getCurrentCursorLine() != -1) {
            return ec.getLayout().getLineBottom(ec.getCurrentCursorLine());
        }
        return -1;
    }

    public float getCursorX(EditText editText) {//https://www.b4x.com/android/forum/threads/how-to-get-edittext-cursor-position-x-y.13811/
        float ret = -1;
        try {
            int pos = editText.getSelectionStart();
            Layout layout = editText.getLayout();
            float x = layout.getPrimaryHorizontal(pos);

            ret = x;

        } catch (Exception exception) {
            Log.d("getCursorPos", exception.toString());
        }
        return ret;
    }

    public void saveTempFile() {
        try {
            OutputStream output = new FileOutputStream(new File(mDirs.filesDir + "autocomplete.go"));
            output.write(((EditText) activity.findViewById(R.id.inputView)).getText().toString().getBytes());
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            activity.showError("Temp file creation failed", ex.toString(), false);
        }
    }
}

