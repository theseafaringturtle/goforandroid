package derp.goforandroid;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by User on 07/04/2017.
 */

public class EditCode extends AppCompatEditText {

    MainActivity activity;
    TextWatcher watcher;
    private Rect lineNumRect;
    Paint lineNumPaint;
    PrettifyHighlighter highlighter = new PrettifyHighlighter();
    Rect curLineRect;
    int curLine;
    Paint curLinePaint;
    ArrayList<ArrayList> history = new ArrayList<>();
    Handler changeTimer = new Handler();
    Runnable changeRunnable;//history
    ArrayList<TextChange> currentZipper = new ArrayList<>();
    int historyIndex = -1;
    String lastSearchTerm = "";
    Autocomplete autocomplete = null;
    static boolean drawLineNums = true;
    static boolean highlightLine = true;
    static boolean autoComplete = true;


    public EditCode(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(!isInEditMode())
            activity = (MainActivity)context;
        watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                historyAdd(s,start,count);
                changeTimer.removeCallbacks(changeRunnable);
                changeTimer.postDelayed(changeRunnable, 2000);
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentZipper.get(currentZipper.size()-1).newS=s.subSequence(start,start+count);
            }
            @Override
            public void afterTextChanged(Editable s) {
                String code = s.toString();
                highlighter.highlight(s,code);
                padLineNums();
                startAutoComplete(code);
                setTabUnsaved();
            }
        };
        /*this.post(new Runnable() {//don't fire on orientation change
            @Override
            public void run() {
                removeTextChangedListener(watcher);
                addTextChangedListener(watcher);
            }
        });*/

        changeRunnable = new Runnable()
        {
            public void run(){
                history.add(currentZipper);
                currentZipper = new ArrayList<TextChange>();
                historyIndex++;
            }
        };

        lineNumRect = new Rect();
        setPadding((int)getTextSize(),0,0,0);
        lineNumPaint = new Paint();
        lineNumPaint.setStyle(Paint.Style.FILL);
        lineNumPaint.setColor(Color.BLACK);
        lineNumPaint.setTextSize(this.getTextSize());
        curLineRect = new Rect(0,0,getWidth(),0);
        curLinePaint = new Paint();
        curLinePaint.setStyle(Paint.Style.FILL);
        curLinePaint.setColor(Color.parseColor("#3E197AE9"));
    }
    void startAutoComplete(String code){
        if(autocomplete!=null && autoComplete && getSelectionStart()>0){
            if(code.substring(getSelectionStart()-1,getSelectionStart()).equals(".")) {
                autocomplete.offset = getSelectionStart();
                autocomplete.ACTimer.removeCallbacks(autocomplete.ACRunnable);
                autocomplete.ACTimer.postDelayed(autocomplete.ACRunnable, 2000);
            }
            else{
                autocomplete.ACTimer.removeCallbacks(autocomplete.ACRunnable);
            }
        }
    }
    void setTabUnsaved(){
        if(!TabsDialog.unsavedTabs.contains(TabsDialog.currentTabIndex)) {
            TabsDialog.unsavedTabs.add(TabsDialog.currentTabIndex);
            ((Button) activity.findViewById(R.id.openTabButton)).append("*");
        }
    }

    void historyClear(){
        history.clear();
        currentZipper.clear();
        changeTimer.removeCallbacks(changeRunnable);
        historyIndex = -1;
    }
    void historyAdd(CharSequence s, int start, int count){
        //activity.findViewById(R.id.toolsButton).setEnabled(true);
        if(historyIndex < history.size() - 1){
            for(int i = history.size()-1; i>historyIndex; i--) {
                history.remove(i);
                //currentZipper.clear();
            }
        }
        currentZipper.add(new TextChange(start,s.subSequence(start,start+count),null));
    }
    void historyForward(){
        if(historyIndex+1==history.size()) return;
        historyIndex++;
        changeTimer.removeCallbacks(changeRunnable);
        ArrayList<TextChange> zip = history.get(historyIndex);
        for(int i = 0 ; i<zip.size(); i++) {
            TextChange change = zip.get(i);
            removeTextChangedListener(watcher);
            setText(getText().replace(change.start, change.start + change.oldS.length(), change.newS));
            addTextChangedListener(watcher);
            setSelection(change.start + change.newS.length());
            bringPointIntoView(change.start+change.oldS.length());
        }
        highlighter.highlight(getEditableText(),getText().toString());
        //currentZipper = new ArrayList<TextChange>();
    }
    void historyBack(){
        if(currentZipper.size()>0) {
            history.add(currentZipper);
            historyIndex++;
        }
        if(historyIndex==-1){
            return;
        }
        changeTimer.removeCallbacks(changeRunnable);
        ArrayList<TextChange> zip = history.get(historyIndex);
        for(int i = zip.size()-1;i!=-1;i--) {
            TextChange change = zip.get(i);
            removeTextChangedListener(watcher);
            setText(getText().replace(change.start, change.start + change.newS.length(), change.oldS));
            addTextChangedListener(watcher);
            setSelection(change.start + change.oldS.length());
            bringPointIntoView(change.start+change.oldS.length());
        }
        historyIndex--;
        currentZipper = new ArrayList<TextChange>();
        highlighter.highlight(getEditableText(),getText().toString());
    }
    public void searchDialog(){
        final EditText txtInput = new EditText(activity);
        txtInput.setSingleLine(true);
        txtInput.setText(lastSearchTerm);
        new AlertDialog.Builder(activity)
                .setTitle("Search")
                .setMessage("Enter text to search")
                .setView(txtInput)
                //.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String text = txtInput.getText().toString();
                        searchText(text);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
    public void searchText(String toSearch){
        lastSearchTerm = toSearch;
        if(toSearch.length()==0) return;
        int start = 0;
        if(getSelectionStart()!=-1)
            start=getSelectionStart();
        CharSequence seqafter = getText().subSequence(start,getText().length());
        int index = seqafter.toString().indexOf(toSearch);
        if(index!=-1){
            setSelection(start+index+toSearch.length());
            bringPointIntoView(start+index+toSearch.length());
        }
        else{
            if(toSearch.length()<start) {//text could be in the segment before selection
                CharSequence seqbefore = getText().subSequence(0,start);
                int index_b = seqbefore.toString().indexOf(toSearch);
                if(index_b!=-1){
                    setSelection(index_b+toSearch.length());
                    bringPointIntoView(index_b+toSearch.length());
                }
                else{
                    Toast.makeText(getContext(),"Text not found",Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(getContext(),"Text not found",Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void gotoLineDialog(){
        final EditText txtInput = new EditText(activity);
        txtInput.setSingleLine(true);
        txtInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog dialog;
        dialog = new AlertDialog.Builder(activity)
                .setTitle("Jump to Line")
                .setMessage("Enter line number")
                .setView(txtInput)
                //.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String linenum = txtInput.getText().toString();
                        gotoLine(linenum);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }
    public void gotoLine(String lineNum){
        /*int num = Math.max(0,Integer.parseInt(lineNum)-1);
        if(num>getLineCount()){
            num=getLineCount();
        }
        final int offset =getLayout().getLineEnd( num );
        setSelection(offset);*/
        int line = Math.max(0,Integer.parseInt(lineNum)-1);
        line = Math.max(0, Math.min(line - 1, getLineCount() - 1));
        setSelection(getLayout().getLineEnd(line));
        //bringPointIntoView(offset);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        curLine = getCurrentCursorLine();
        if(curLine==-1) return;
        curLineRect.top = curLine*getLineHeight();
        measure(0,0);
        curLineRect.right = Math.max(getMeasuredWidth(),getWidth());//getWidth();
        curLineRect.bottom = curLine*getLineHeight()+getLineHeight();
        setHorizontallyScrolling(true);
        setMovementMethod(new ScrollingMovementMethod());
        super.onSelectionChanged(selStart,selEnd);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if(drawLineNums) {
            int baseline = getBaseline();
            for (int i = 0; i < getLineCount(); i++) {
                canvas.drawText("" + (i + 1), lineNumRect.left, baseline, lineNumPaint);
                baseline += getLineHeight();
            }
        }
        canvas.drawRect(curLineRect,curLinePaint);
        super.onDraw(canvas);
    }
    public int getCurrentCursorLine(){// http://stackoverflow.com/a/9804858
        int selectionStart = Selection.getSelectionStart(this.getText());
        Layout layout = getLayout();
        if (selectionStart != -1 && layout!=null) {
            return layout.getLineForOffset(selectionStart);
        }
        return -1;
    }
    public void padLineNums(){
        if(getLineCount()<10 || !drawLineNums)
            setPadding((int)getTextSize(),0,0,0);
        else if(getLineCount()<100)
            setPadding((int) (getTextSize()*1.5),0,0,0);
        else if(getLineCount()<1000){
            setPadding((int)getTextSize()*2,0,0,0);
        }
    }
    public void findNext(String input){

    }
}
class TextChange{
    int start;
    CharSequence oldS;
    CharSequence newS;
    public TextChange(int start,CharSequence oldS,CharSequence newS){
        this.start=start;
        this.oldS=oldS;
        this.newS=newS;
    }
}
//StringBuilder lnString = new StringBuilder();
//int width = getWidth();
//int c = 0;
        /* getLayout().getText().charAt(getLayout().getLineEnd(i)-1)=='\n' && !lineList.contains(i) */
        /*StringReader reader = new StringReader(getText().toString());
        LineNumberReader lineReader = new LineNumberReader(reader);
        try {
            String line = "";
            while ((line = lineReader.readLine()) != null) {
                c++;
                lnString.append(c);
                lnString.append("\n");
                if (Math.round(getPaint().measureText(line, 0, line.length()) / ((float) width)) >= 1) {
                    for (double d = Math.ceil((double) (lineNumPaint.measureText(line, 0, line.length()) / ((float) width))); d > 1; d -= 1) {
                        lnString.append("\n");
                    }
                }
            }
            if(activity!=null)
                ((TextView)activity.findViewById(R.id.lineNums)).setText(lnString.toString());
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        finally{
            reader.close();
        }*/

        /*@Override
    protected void onScrollChanged(int x,int y,int oldx,int oldy){
        super.onScrollChanged(x,y,oldx,oldy);
        final int yy = y;
        if(activity!=null) {
            //activity.findViewById(R.id.lineNums).scrollTo(0, y);
            activity.findViewById(R.id.lineNums).post(new Runnable() {
                @Override
                public void run() {
                    activity.findViewById(R.id.lineNums).scrollTo(0, yy);//test
                }
            });
        }
    }*/