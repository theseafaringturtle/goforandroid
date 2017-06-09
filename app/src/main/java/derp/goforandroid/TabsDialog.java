package derp.goforandroid;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by User on 23/04/2017.
 */

public class TabsDialog {
    MainActivity activity;
    EditFragment fragment;
    static int currentTabIndex = 0;

    AlertDialog.Builder builderSingle;
    AlertDialog dialog;
    static ArrayAdapter<String> arrayAdapter;
    static ArrayList<String> tabsList = new ArrayList<>();
    static ArrayList<Integer> unsavedTabs = new ArrayList<>();

    public TabsDialog(MainActivity act,EditFragment frag){//http://webcache.googleusercontent.com/a/15762955
        this.activity = act;
        this.fragment = frag;
        builderSingle = new AlertDialog.Builder(activity);
        builderSingle.setTitle("Select Tab");

        //arrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.select_dialog_singlechoice);
        arrayAdapter = new CustomTabsAdapter<>(act,R.layout.mytab_listitem,tabsList,frag);
        //arrayAdapter.add("New File");

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                chooseTab(which);
                currentTabIndex = which;
            }
        });
        dialog = builderSingle.create();
    }
    public void show(){
        dialog.show();
    }
    void removeTab(int pos){
        if(currentTabIndex == pos){
            if(currentTabIndex>0) {
                chooseTab(currentTabIndex - 1);
                currentTabIndex = currentTabIndex - 1;
            }
            else if(currentTabIndex+1<tabsList.size()) {
                chooseTab(currentTabIndex + 1);
            }
        }
        tabsList.remove(pos);
        EditFragment.openDocuments.remove(pos);
        arrayAdapter.notifyDataSetChanged();
        if(EditFragment.openDocuments.size()==0){
            activity.pager.setPagingEnabled(false);
            dialog.dismiss();
            ((TabLayout)activity.findViewById(R.id.sliding_tabs)).getTabAt(0).select();
            EditFragment.currentPath = "";
            EditFragment.noTabs=true;
        }
    }
    void chooseTab(int which){
        String strName = arrayAdapter.getItem(which);
        ((Button)activity.findViewById(R.id.openTabButton)).setText(strName);
        fragment.inView.removeTextChangedListener(fragment.inView.watcher);
        fragment.inView.setText(fragment.openDocuments.get(which));
        fragment.inView.post(new Runnable() {//todo refactor
            @Override
            public void run() {
                fragment.inView.addTextChangedListener(fragment.inView.watcher);
                fragment.inView.padLineNums();
                fragment.inView.highlighter.highlight(fragment.inView.getEditableText(),fragment.inView.getText().toString());
                fragment.inView.historyClear();
            }
        });
        EditFragment.currentPath = fragment.mDirs.GOPATH+strName;
    }
}
class CustomTabsAdapter<String> extends ArrayAdapter<String> {
    //String [] fileNames;
    MainActivity activity;
    EditFragment frag;
    private static LayoutInflater inflater=null;
    public CustomTabsAdapter(MainActivity mainActivity, int layoutResourceId, ArrayList<String> files, EditFragment fr) {
        super(mainActivity,layoutResourceId,files);
        activity = mainActivity;
        frag = fr;
        inflater = ( LayoutInflater ) activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public class Holder
    {
        TextView tv;
        Button del;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder=new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.mytab_listitem, null);
        holder.tv=(TextView) rowView.findViewById(R.id.textView1);
        holder.del = (Button) rowView.findViewById(R.id.deleteButton);
        holder.tv.setText(getItem(position).toString());
        if(position==TabsDialog.currentTabIndex)
            holder.tv.setTextColor(Color.BLUE);
        if(TabsDialog.unsavedTabs.contains(position))
            holder.tv.append("*");
        holder.del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditFragment.tabsDialog.removeTab(position);
                }
            });
        return rowView;
    }
}