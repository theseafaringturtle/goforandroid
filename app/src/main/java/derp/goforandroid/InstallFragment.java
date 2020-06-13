package derp.goforandroid;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InstallFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InstallFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InstallFragment extends Fragment {

    private static final String ARG_PARAM1 = "buildSwitch";

    private boolean buildSwitch;
    private boolean firstTime = true;

    String GO = "Go";
    String SETTINGS = "GoSettings";


    Dirs mDirs;

    MainActivity activity = null;


    public InstallFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            buildSwitch = getArguments().getBoolean(ARG_PARAM1);
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        if(!firstTime) return;
        firstTime = false;
        activity = (MainActivity)getActivity();
        mDirs = activity.mDirs;
        SharedPreferences sh = getActivity().getSharedPreferences(SETTINGS,MODE_PRIVATE);
        boolean extracted = false;
        if(sh.contains("extracted"))
            extracted = true;
        if(!mDirs.checkFiles() || !extracted){
            Runnable r = new Runnable(){
                @Override
                public void run() {
                    extractFiles();
                }
            };
            new Thread(r,"extractor").start();
        }
        else if(buildSwitch){
            firstBuild();
        }
    }


    void stepProgress(int count){
        if(getView() != null) {
            ((ProgressBar) getView().findViewById(R.id.progressBar)).setProgress(((ProgressBar) getView().findViewById(R.id.progressBar)).getProgress() + count);
        }
    }
    public void extractionResult(boolean success){
        if(!success) {
            activity.showError("Error","File Extraction Failed",true);
        }
        else{
            SharedPreferences.Editor editor = activity.getSharedPreferences(SETTINGS, MODE_PRIVATE).edit();
            editor.putBoolean("extracted",true);
            editor.apply();
            ((TextView)getView().findViewById(R.id.progressText)).setText("Setting up...");
            ((ProgressBar)getView().findViewById(R.id.progressBar)).setVisibility(GONE);
            ((ProgressBar)getView().findViewById(R.id.progressBar2)).setVisibility(VISIBLE);
            Runnable r = new Runnable(){
                @Override
                public void run() {
                    firstBuild();
                }
            };
            new Thread(r,"builder").start();
        }
    }

    private void extractFiles(){
        String tarName = "go-linux-arm.zip";
        ((ProgressBar)getView().findViewById(R.id.progressBar)).setMax(116557824);//TODO get file size
        final boolean success = Decompress.unzipFromAssets(this,tarName,mDirs.filesDir) && mDirs.checkFiles();

        //Decompress.unzipFromAssets(this,"android-gcc-4.4.0.zip",mDirs.filesDir);
        getActivity().runOnUiThread( new  Runnable(){
            @Override
            public void run(){
                extractionResult(success);
            }
        });

    }
    /*void setGoPath(){
        SharedPreferences sharedPreferences = activity.getSharedPreferences(SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(!mDirs.externalStorageAvailable()){
            mDirs.GOPATH = mDirs.filesDir+"go"+File.separator;
            editor.putBoolean("externalStorage",false);
            editor.commit();
        }
        else{
            mDirs.GOPATH = Environment.getExternalStorageDirectory()+File.separator+"GoForAndroid"+File.separator;
            editor.putBoolean("externalStorage",true);
            editor.commit();
        }
        mDirs.srcDir = mDirs.GOPATH+"src"+File.separator;
        mDirs.pkgDir = mDirs.GOPATH+"pkg"+File.separator;
        mDirs.binDir = mDirs.GOPATH+"bin"+File.separator;
    }*/

    //and set permissions
    private void createOtherPaths() throws IOException {
        new File(mDirs.filesDir).setWritable(true, false);
        new File(mDirs.filesDir).setReadable(true, false);
            File g = new File(mDirs.GOPATH);
            if (!g.exists()) {
                g.mkdir();
                new File(mDirs.pkgDir).mkdir();
                if (new File(mDirs.srcDir + "welcome").mkdirs()) {
                    InputStream in = activity.getAssets().open("welcome.go");
                    OutputStream out = new FileOutputStream(new File(mDirs.srcDir + "welcome" + File.separator + "welcome.go"));
                    Utils.copyFile(in, out);
                }
                if (new File(mDirs.srcDir + "httpClient").mkdirs()) {
                    InputStream in = activity.getAssets().open("httpClient.go");
                    OutputStream out = new FileOutputStream(new File(mDirs.srcDir + "httpClient" + File.separator + "httpClient.go"));
                    Utils.copyFile(in, out);
                }
                if (new File(mDirs.binDir).mkdirs()) {
                    InputStream in = activity.getAssets().open("gocode");//autocompletion
                    OutputStream out = new FileOutputStream(new File(mDirs.binDir+"gocode"));
                    Utils.copyFile(in, out);
                    new File(mDirs.binDir+"gocode").setExecutable(true, false);
                }
            }
            new File(mDirs.goExePath + "go").setExecutable(true);
            new File(mDirs.goExePath + "go").setWritable(true, false);
            new File(mDirs.goExePath + "gofmt").setExecutable(true);
            new File(mDirs.goExePath + "gofmt").setWritable(true, false);

            new File(mDirs.goToolsDir + "compile").setExecutable(true, false);
            new File(mDirs.goToolsDir + "link").setExecutable(true, false);
            new File(mDirs.goToolsDir + "asm").setExecutable(true, false);
        //new File(mDirs.goToolsDir + "cgo").setExecutable(true, false);
            String tmpPath = mDirs.filesDir + "tmp";
            new File(tmpPath).mkdir();
            new File(tmpPath).setWritable(true, false);

            new File(mDirs.goToolsDir + "cgo").setExecutable(true, false);
    }
    private void firstBuild(){
        try{
            //setGoPath();
            createOtherPaths();
            setupExamples();
            //String command = "GOROOT="+GOROOT+" GOPATH="+GOPATH+" "+filesDir+compFolder+File.separator+"bin"+File.separator+"go build test";
            String[] command = {
                    mDirs.goExePath+"go",
                    "install",//"build", http://stackoverflow.com/a/25138487
                    "welcome"
            };
            HashMap<String,String> envVars = new HashMap<>();
            envVars.put("GOROOT",mDirs.GOROOT);
            envVars.put("GOPATH",mDirs.GOPATH);
            envVars.put("GOCACHE",mDirs.cacheDir);
            envVars.put("TMPDIR",mDirs.filesDir+"tmp");
            envVars.put("CGO_ENABLED","0");//todo android toolchain for cgo
            String wDir = mDirs.goExePath;
            final String out = Utils.executeCommand(command,envVars,wDir);
            if(out.length()<8){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeFragment();
                    }
                });
            }
            else{
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity)activity).showError("Build Failed, Exiting",out,true);
                    }
                });
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
            ((MainActivity)activity).showError("Build Failed, Exiting",ex.toString(),true);
        }
    }
    private void setupExamples(){//todo examples
        /* Not working:
        command-line-arguments, line-filters, reading-files, signals
        Review:
        panic,
        Remove html tags
         */

        final boolean success = Decompress.unzipFromAssets(this,"examples.zip",mDirs.srcDir) && mDirs.checkFiles();
        new File(mDirs.binDir+"examples").mkdir();
    }


    void changeFragment(){
        activity.installFinished();
    }



    public static InstallFragment newInstance(boolean param1) {
        InstallFragment fragment = new InstallFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_install, container, false);
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
