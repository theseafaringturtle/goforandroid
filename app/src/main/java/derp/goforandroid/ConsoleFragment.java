package derp.goforandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.util.TypedValue.COMPLEX_UNIT_PX;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ConsoleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConsoleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConsoleFragment extends Fragment {

    private static final String ARG_PARAM1 = "Terminal";
    static String GO = "go";

    private boolean isTerminal;

    /*static String[] command = null;
    static HashMap envVars = null;
    static String wDir = null;*/

    View rootView;
    TextView outputView;
    EditText consoleInputView;
    MainActivity activity;
    static Runnable myRunnable = null;
    static BufferedReader terminalIn;
    static BufferedWriter terminalOut;
    static Process process;
    static Thread runThread;
    static final CyclicBarrier barrier = new CyclicBarrier(2);

    private OnFragmentInteractionListener mListener;

    public ConsoleFragment() {
        // Required empty public constructor
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        rootView = view;
        activity = (MainActivity) getActivity();
        outputView = (TextView) activity.findViewById(R.id.outputView);
        consoleInputView = (EditText) activity.findViewById(R.id.consoleInput);

        outputView.post(new Runnable() {
            @Override
            public void run() {
                consoleInputView.setTextSize(COMPLEX_UNIT_PX,outputView.getTextSize());// get->px, set->sp by default
            }
        });
        outputView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] options = {"Copy all","Stop"};
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Action");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("GoConsole", outputView.getText().toString());
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(activity, "Console output copied to clipboard", Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                stopProcess();
                                break;
                        }
                    }
                });
                builder.show();
                return true;
            }
        });
        outputView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus && consoleInputView.isEnabled()) {
                    //v.clearFocus();//crashes when scrolling
                    consoleInputView.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(consoleInputView, InputMethodManager.SHOW_FORCED);
                }
            }
        });
        consoleInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_GO){
                    displayLine(v.getText().toString());
                    sendLine(v.getText().toString()+"\n"+'\u0004');//eot in case it's not scanln
                    consoleInputView.setEnabled(false);
                    consoleInputView.setText("");
                    return true;
                }
                return false;
            }
        });
        setRetainInstance(true);
    }

    public void executeWithInput(final String[] command, final HashMap envVars, final String workingDir){
        myRunnable = new Runnable(){//fragment leak? http://webcache.googleusercontent.com/a/3039718
            @Override
            public void run() {
                try {
                    ProcessBuilder builder = new ProcessBuilder(command);
                    builder.environment().putAll(envVars);
                    builder.directory(new File(workingDir));
                    builder.redirectErrorStream(true);
                    process = builder.start();
                    String line = "";
                    terminalIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    terminalOut = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    while ((line = terminalIn.readLine()) != null) {
                        if (line.equals("__!Waiting for input!__")) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    enableInput();
                                    scrollToBottom();
                                }
                            });
                            barrier.await();
                            continue;
                        }
                        final String toDisplay = line;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayLine(toDisplay);
                            }
                        });
                        Log.d(GO, line);
                    }
                    terminalIn.close();
                    terminalOut.close();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scrollToBottom();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();

                }
            }
        };
        runThread = new Thread(myRunnable,"terminal");
        runThread.start();


    }
    void enableInput(){
        consoleInputView.setEnabled(true);
    }
    void displayLine(String line){
        line+="\n";
        outputView.append(line);
    }
    void sendLine(String line){
        try {
            barrier.await();
            terminalOut.write(line);
            terminalOut.flush();
        }
        catch(Exception ex){
            ex.printStackTrace();//closed?
        }
    }
    void stopProcess(){
        if(process!=null){
            process.destroy();
        }
        try{
            if (terminalIn != null) {
                terminalIn.close();
                terminalOut.close();
            }
            barrier.reset();
        }
        catch(Exception ex){
            ex.printStackTrace();//closed?
        }
    }

    void scrollToBottom(){
        final ScrollView scrollLayout = (ScrollView)activity.findViewById(R.id.outputScroll);
        //RelativeLayout lastChild =(RelativeLayout) scrollLayout.getChildAt(scrollLayout.getChildCount() - 1);
        scrollLayout.post(new Runnable() { public void run() { scrollLayout.fullScroll(View.FOCUS_DOWN); } });
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment ConsoleFragment.
     */
    public static ConsoleFragment newInstance(boolean param1) {
        ConsoleFragment fragment = new ConsoleFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isTerminal = getArguments().getBoolean(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_console, container, false);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        // TODO: change or remove
        void onFragmentInteraction(Uri uri);
    }
}