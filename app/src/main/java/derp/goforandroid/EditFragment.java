package derp.goforandroid;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EditFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditFragment extends Fragment {

    private static final String EXAMPLE = "example";

    private boolean isExample;
    private boolean firstTime = true;


    static String currentPath = "";

    static int fontSize = 18;

    MainActivity activity = null;
    Dirs mDirs;
    EditCode inView;
    static TabsDialog tabsDialog;
    static ArrayList<String> openDocuments = new ArrayList<>();
    static Runnable buildRunnable;
    static Thread buildThread;
    static boolean noTabs = true;

    private OnFragmentInteractionListener mListener;

    public EditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        //if(!firstTime) return;
        //firstTime=false;
        activity =(MainActivity) view.getContext(); //getActivity();
        mDirs = activity.mDirs;
        inView = (EditCode) activity.findViewById(R.id.inputView);
        //inView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inView.setTextSize(fontSize);

        if(!activity.lastFilePath.equals("") && new File(activity.lastFilePath).exists()) {
            try {
                loadFile(activity.lastFilePath, activity);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }

        ((Button)activity.findViewById(R.id.runButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //if (buildCode(mDirs.getPackageName(currentPath))) {
                    //    runCode(mDirs.getPackageName(currentPath));
                    //}
                    startBuild(mDirs.getPackageName(currentPath));
                }catch(Exception ex){
                    ex.printStackTrace();
                    ((MainActivity)activity).showError("Failed to run code",ex.toString(),false);
                }
                ((TabLayout)activity.findViewById(R.id.sliding_tabs)).getTabAt(2).select();
                //getConsoleFragment().scrollToBottom();
            }
        });
        setRetainInstance(true);
        //add special chars
        View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                CharSequence toAdd = ((Button)v).getText();
                if(toAdd.toString().equalsIgnoreCase("TAB"))
                    toAdd = "\t";
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor("#AAAAAA"));
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    EditCode ec = (EditCode) activity.findViewById(R.id.inputView);
                    if(ec.getSelectionEnd()-ec.getSelectionStart()>0){
                        //replace selected text with char
                        ec.getEditableText().replace(ec.getSelectionStart(),ec.getSelectionEnd(),toAdd);
                    }
                    else {
                        ec.getEditableText().insert(ec.getSelectionStart(), toAdd);
                    }
                    v.setBackgroundColor(Color.parseColor("#010101"));
                }
                else if(event.getAction() == MotionEvent.ACTION_CANCEL){
                    v.setBackgroundColor(Color.parseColor("#010101"));
                }
                return true;
            }
        };
        KeyButton tkb = new KeyButton(activity);
        tkb.setText("TAB");
        tkb.setOnTouchListener(listener);
        ((LinearLayout)activity.findViewById(R.id.keyButtonRow)).addView(tkb);
        char[] specialChars = ":={}[]()\"'+-<>*/%.,&!|_;".toCharArray();
        for(char c : specialChars){
            KeyButton kb = new KeyButton(activity);
            kb.setText(""+c);
            kb.setOnTouchListener(listener);
            ((LinearLayout)activity.findViewById(R.id.keyButtonRow)).addView(kb);
        }
        activity.findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentFile(true);
            }
        });

        LayoutInflater layoutInflater = activity.getLayoutInflater();
        final View popupView_AC = layoutInflater.inflate(R.layout.popup_edit, null);
        final PopupWindow popupWindow = new PopupWindow(
                popupView_AC,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        //popupWindow.setEnterTransition();
        final Button toolsButton = (Button) activity.findViewById(R.id.toolsButton);
        toolsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!popupWindow.isShowing())
                    popupWindow.showAsDropDown(activity.findViewById(R.id.toolsButton),0,0);
                else
                    popupWindow.dismiss();
            }
        });
        popupView_AC.findViewById(R.id.undoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inView.historyBack();
                popupWindow.dismiss();
            }
        });
        popupView_AC.findViewById(R.id.redoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inView.historyForward();
                popupWindow.dismiss();
            }
        });
        popupView_AC.findViewById(R.id.searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inView.searchDialog();
                popupWindow.dismiss();
            }
        });
        popupView_AC.findViewById(R.id.gotoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inView.gotoLineDialog();
                popupWindow.dismiss();
            }
        });
        inView.autocomplete = new Autocomplete(activity,this);

        final Button tabsButton = (Button) activity.findViewById(R.id.openTabButton);
        tabsDialog = new TabsDialog(activity,this);
        tabsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabsDialog.show();
            }
        });
    }

    public void startBuild(final String packageName){
        saveCurrentFile(false);
        ((TextView) activity.findViewById(R.id.outputView)).setText("Compiling...");
        buildRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if(buildCode(packageName))
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    runCode(mDirs.getBinNameFromPackageName(packageName));//switch back to ui before starting execution thread
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        buildThread = new Thread(buildRunnable,"builder");
        buildThread.start();
    }
    public boolean buildCode(String packageName) throws IOException{
        //Toast.makeText(activity,"Building code...",Toast.LENGTH_SHORT).show();
        String[] command = {
                mDirs.goExePath+"go",
                "install",
                packageName
        };
        HashMap<String,String> envVars = new HashMap<>();
        envVars.put("GOROOT",mDirs.GOROOT);
        envVars.put("GOPATH",mDirs.GOPATH);
        envVars.put("TMPDIR",mDirs.filesDir+"tmp");
        envVars.put("CGO_ENABLED","0");
        String wDir = mDirs.goExePath;
        final String out = Utils.executeCommand(command,envVars,wDir);

        //if(out.length()<8){
        if (out.length()==0 &&new File(mDirs.binDir + mDirs.getBinNameFromPackageName(packageName)).exists()) {
            return true;
        } else if (out.length()==0 && new File(mDirs.pkgDir + mDirs.getBinNameFromPackageName(packageName) + ".a").exists()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(activity,"Package is a library",Toast.LENGTH_SHORT).show();
                    ((TextView)activity.findViewById(R.id.outputView)).setText("Library compiled successfully");
                }
            });
            return false;
        }
        else{
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView)activity.findViewById(R.id.outputView)).setText(out);
                    //((MainActivity)activity).showError("Build Failed",out,false);
                }
            });
        }
        return false;
    }

    public void runCode(String packageName) throws IOException{
        String[] command = {
                mDirs.binDir+packageName
        };
        HashMap<String,String> envVars = new HashMap<>();
        envVars.put("TMPDIR",mDirs.filesDir+"tmp");
        String wDir = mDirs.binDir;
        //don't clutter user bin directory with example bins
        if(mDirs.getPackageName(currentPath).startsWith("examples")){
            if(!new File(mDirs.binDir+"examples").exists())
                new File(mDirs.binDir+"examples").mkdir();
            new File(mDirs.binDir+packageName).renameTo(new File(mDirs.binDir+"examples"+File.separator+packageName));//move bin to examples subdir
            command[0]=mDirs.binDir+"examples"+File.separator+packageName;
        }
        else{//test
            if(!new File(mDirs.binDir+"_"+packageName+File.separator).exists())//underscore?
                new File(mDirs.binDir+"_"+packageName+File.separator).mkdir();
            if(new File(mDirs.binDir+packageName).renameTo(new File(mDirs.binDir+"_"+packageName+File.separator+packageName)))
                command[0] = mDirs.binDir + "_" + packageName + File.separator + packageName;
        }
        /*
        if(mDirs.getPackageName(currentPath).startsWith("examples")){
            if(!new File(mDirs.binDir+"examples").exists())
                new File(mDirs.binDir+"examples").mkdir();
            if(!new File(mDirs.binDir+"examples"+File.separator+packageName).exists())
                new File(mDirs.binDir+"examples"+File.separator+packageName).mkdir();
            if(new File(mDirs.binDir+packageName).renameTo(new File(mDirs.binDir+"examples"+File.separator+packageName+File.separator+packageName)))//move bin to examples subdir
                command[0]=mDirs.binDir+"examples"+File.separator+packageName;
        }
        else{//test
            if(!new File(mDirs.binDir+"_"+packageName+File.separator).exists())//underscore?
                new File(mDirs.binDir+"_"+packageName+File.separator).mkdir();
            if(new File(mDirs.binDir+packageName).renameTo(new File(mDirs.binDir+"_"+packageName+File.separator+packageName)))
                command[0] = mDirs.binDir + "_" + packageName + File.separator + packageName;
        }f
        */
        ConsoleFragment con = getConsoleFragment();
        con.stopProcess();
        con.activity=activity;
        con.outputView = (TextView) activity.findViewById(R.id.outputView);
        con.outputView.setText("");
        con.consoleInputView = (EditText) activity.findViewById(R.id.consoleInput);
        con.consoleInputView.setText("");
        con.executeWithInput(command,envVars,wDir);
        //final String out = Utils.executeCommand(command,envVars,wDir);
        //return out;
    }
    ConsoleFragment getConsoleFragment(){
        return ((ConsoleFragment) activity.pageAdapter.getItem(2));
    }
    public void setTabSaved(){
        TabsDialog.unsavedTabs.remove(Integer.valueOf(TabsDialog.currentTabIndex));
        Button openTabButton = ((Button)activity.findViewById(R.id.openTabButton));
        if(openTabButton.getText().toString().endsWith("*"))
            openTabButton.setText(openTabButton.getText().toString().substring(0,openTabButton.getText().toString().length()-1));
    }
    public void goFmtCurrentFile(){
        String[] command = {
                mDirs.goExePath+"gofmt",
                "-w",
                currentPath
        };
        HashMap<String,String> envVars = new HashMap<>();
        envVars.put("GOROOT",mDirs.GOROOT);
        envVars.put("GOPATH",mDirs.GOPATH);
        String wDir = mDirs.goExePath;
        try {
            Utils.executeCommand(command, envVars, wDir);
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public void saveCurrentFile(boolean manual){
        setTabSaved();
        try {
            OutputStream output = new FileOutputStream(new File(currentPath));
            output.write(((EditText)activity.findViewById(R.id.inputView)).getText().toString().getBytes());
            goFmtCurrentFile();
            loadFile(currentPath,activity);
            if(manual)
                Toast.makeText(activity,"File saved",Toast.LENGTH_SHORT).show();
        }catch(Exception ex){
            ex.printStackTrace();
            activity.showError("File save failed",ex.toString(),false);
        }
    }
    public void copyStub(boolean isLibrary,String path, MainActivity activity,String name) throws IOException{
        String stubName = "stub.go";
        if(!isLibrary) {
            InputStream in = activity.getAssets().open(stubName);
            OutputStream out = new FileOutputStream(path);
            Utils.copyFile(in, out);
        }
        else{
            stubName="stub_lib.go";
            InputStream input = activity.getAssets().open(stubName);
            //prepend package name to file content
            final char[] buffer = new char[1024];
            final StringBuilder out = new StringBuilder();
            out.append("package "+name+"\n");
            Reader in = new InputStreamReader(input, "UTF-8");
            int rsz;
            while ((rsz = in.read(buffer, 0, buffer.length)) != -1) {
                out.append(buffer, 0, rsz);
            }
            OutputStream output = new FileOutputStream(new File(mDirs.srcDir + name + File.separator + name + ".go"));
            output.write(out.toString().getBytes());
        }
    }

    public void loadNewFile(final boolean isLibrary, final String path, final MainActivity activity, final String projName) throws Exception{
        loadFile(path,activity);
        Snackbar.make(activity.findViewById(R.id.editRoot), "Press Ok to add program stub", Snackbar.LENGTH_LONG)
                .setAction("Ok", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            copyStub(isLibrary,path,activity,projName);
                            loadFile(path,activity);
                        }
                        catch(Exception ex){

                        }
                    }
                })
                .setActionTextColor(Color.WHITE)
                .setDuration(BaseTransientBottomBar.LENGTH_LONG)
                .show();
    }

    public void loadFile(final String path, MainActivity activity)throws Exception{
        this.activity = activity;
        this.mDirs = activity.mDirs;
        currentPath = path;
        File file = new File(path);
        if(file.length()>1024*1024*2){
            throw new IOException("File size is too large (>2MB)");
        }
        FileInputStream inputStream = new FileInputStream(file);
        /*String code = "";
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            code=code.concat(new String(buffer).replace("\t","    "));
        }
        */
        final char[] buffer = new char[1024];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, "UTF-8");
        int rsz;
        while ((rsz = in.read(buffer, 0, buffer.length)) != -1) {
            out.append(buffer, 0, rsz);
        }
        inView = (EditCode) activity.findViewById(R.id.inputView);
        inView.removeTextChangedListener(inView.watcher);
        inView.setText(out.toString());
        inView.post(new Runnable() {
            @Override
            public void run() {
                inView.padLineNums();
                inView.highlighter.highlight(inView.getEditableText(), inView.getText().toString());
                if(!path.equals(currentPath))//reloading current file?
                    inView.historyClear();
                inView.addTextChangedListener(inView.watcher);
            }
        });
        Button tabsButton = (Button)activity.findViewById(R.id.openTabButton);
        final String tabText = mDirs.getRelativePath(currentPath);
        tabsButton.setText(tabText);
        activity.pager.setPagingEnabled(true);
        noTabs=false;
        inView.post(new Runnable() {
            @Override
            public void run() {
                if(tabsDialog.arrayAdapter.getPosition(tabText)==-1) {
                    tabsDialog.tabsList.add(tabText);
                    tabsDialog.arrayAdapter.notifyDataSetChanged();
                    tabsDialog.currentTabIndex=tabsDialog.tabsList.size()-1;
                    openDocuments.add(out.toString());
                }
            }
        });

    }

    public static EditFragment newInstance(boolean param2) {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();
        args.putBoolean(EXAMPLE, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isExample = getArguments().getBoolean(EXAMPLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit, container, false);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onStop(){
        super.onStop();
        activity.currentFilePath = currentPath;
    }
    @Override
    public void onResume(){
        super.onResume();
        applyPrefs();
    }
    void applyPrefs(){
        inView.setTextSize(fontSize);
        inView.lineNumPaint.setTextSize(Utils.spToPx(getResources(),fontSize));
        if(!EditCode.highlightLine)
            inView.curLinePaint.setColor(Color.TRANSPARENT);
        else
            inView.curLinePaint.setColor(Color.parseColor("#3E197AE9"));
        inView.padLineNums();
        inView.highlighter.highlight(inView.getEditableText(),inView.getText().toString());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}

class KeyButton extends AppCompatButton {
    public KeyButton(Context context, AttributeSet attrs){
        super(context,attrs);
        setBackgroundColor(Color.parseColor("#010101"));
        setTextColor(Color.parseColor("#ffffff"));
        setWidth(getMinimumWidth()/2);
        setMinimumWidth(0);
    }
    public KeyButton(Context context){
        super(context);
        setBackgroundColor(Color.parseColor("#010101"));
        setTextColor(Color.parseColor("#ffffff"));
        setWidth(getMinimumWidth()/2);
        setMinimumWidth(0);
    }
}