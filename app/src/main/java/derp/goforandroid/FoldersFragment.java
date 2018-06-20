package derp.goforandroid;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static android.os.Environment.MEDIA_BAD_REMOVAL;
import static android.os.Environment.MEDIA_MOUNTED_READ_ONLY;
import static android.os.Environment.MEDIA_SHARED;
import static android.os.Environment.MEDIA_UNMOUNTED;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FoldersFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FoldersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FoldersFragment extends Fragment {
    // TODO: not needed
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    ListView listView;

    File currentPath;

    String goExt = ".go";
    ArrayList fileList = new ArrayList<String>();
    CustomFolderAdapter<String> adapter;

    MainActivity activity;
    Dirs mDirs;
    boolean createProject = false;

    CharSequence fileOptions[] = new CharSequence[] {"Rename","Delete","Share","Copy to SD Card"};//"Run"
    CharSequence folderOptions[] = new CharSequence[] {"Rename","Delete","Share as zip"};

    String pendingCopy = null;

    private OnFragmentInteractionListener mListener;

    public FoldersFragment() {
        // Required empty public constructor
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        activity = (MainActivity)getActivity();
        mDirs = activity.mDirs;
        String path = mDirs.srcDir;
        if(!activity.lastFolderPath.equals("") && new File(activity.lastFolderPath).exists())
            path=activity.lastFolderPath;
        loadFileList(new File(path));
        listView = (ListView) activity.findViewById(R.id.file_listview);
        setupListView();
        activity.findViewById(R.id.newButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseNew();
            }
        });
        activity.findViewById(R.id.importButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importNew();
            }
        });
        setRetainInstance(true);
    }
    void importNew(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        String strType = "*/*";
        intent.setDataAndType(Uri.parse(currentPath.getAbsolutePath()), strType);
        activity.startActivityForResult(intent, 123);
    }
    void importFileFromUri(Uri uri){
        String destPath = currentPath + File.separator + activity.getFileNameFromUri(uri);
        try {
            InputStream in = activity.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(destPath);
            Utils.copyFile(in, out);
            updateFiles(currentPath);
        }catch(Exception ex){
            activity.showError("Could not import file",ex.toString(),false);
            ex.printStackTrace();
        }
    }

    void chooseNew(){
        if((currentPath+File.separator).equals(mDirs.GOPATH))
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Create...");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createNew(which);
            }
        };
        if( (currentPath+File.separator).equals(mDirs.srcDir) ){
            String[] options = {"New Script Package","New Library Package"};
            builder.setItems(options,listener);
            createProject = true;
        }
        else {
            String packageName = currentPath.getAbsolutePath().substring(mDirs.srcDir.length(), currentPath.getAbsolutePath().length());
            String[] options = {"New .Go File in (" + packageName + ")","New Folder in (" + packageName + ")"};
            builder.setItems(options,listener);
            createProject = false;
        }
        builder.show();
    }

    private void setupListView(){

        adapter = new CustomFolderAdapter<String>(activity, R.layout.myfile_listitem, fileList, this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String item = (String)listView.getItemAtPosition(position);
                if(item.equals("..")){
                    updateFiles(currentPath.getParentFile());
                }
                else if(item.endsWith(goExt)){
                    //activity.pager.setCurrentItem(1);
                    try {
                        ((EditFragment) activity.pageAdapter.getItem(1)).loadFile(currentPath + File.separator + item,activity);
                        switchToEditFrag();
                    }

                    catch(Exception ex){
                        ex.printStackTrace();
                        activity.showError("Could not load file",ex.toString(),false);
                    }
                }
                else if(new File(currentPath+File.separator+item).isDirectory()){
                    updateFiles(new File(currentPath+File.separator+item));
                }
                TextView pview = (TextView) activity.findViewById(R.id.pathTextView);
                pview.setText(mDirs.getRelativePath(currentPath.getAbsolutePath()));
            }

        });

        /*listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                fileOrFolderActionDialog(position);
                return true;
            }
        });*/

    }
    void fileOrFolderActionDialog(int position){
        final String item = (String)listView.getItemAtPosition(position);
        if(item.equals("..")) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if(new File(currentPath+File.separator+item).isDirectory()){
            builder.setTitle("Choose what to do with this folder");
            builder.setItems(folderOptions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    folderAction(which, item);
                }
            });
            builder.show();
        }
        else {
            builder.setTitle("Choose what to do with this file");
            builder.setItems(fileOptions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    fileAction(which, item);
                }
            });
            builder.show();
        }
    }

    void folderAction(int which,final String item) {
        switch (which) {
            case 0://rename
                renameFileDialog(item);
                break;
            case 1://delete
                deleteFileDialog(new File(currentPath+File.separator+item));
                break;
            case 2://share
                shareFolder(item);
                break;
        }
    }

    void fileAction(int which, final String item){
        switch(which){
            /*case 0://run
                String pkname = mDirs.getPackageName(currentPath+File.separator+item);
                runFile(pkname);
                break;*/
            case 0://rename
                renameFileDialog(item);
                break;
            case 1://delete
                deleteFileDialog(new File(currentPath+File.separator+item));
                break;
            case 2://share
                shareFile(new File(currentPath+File.separator+item));
                break;
            case 3://copy to sd
                copyToSD(item);
                break;
        }
    }
    void runFile(String name){
        getEditFragment().startBuild(name);
    }
    void shareFolder(String item){
        File source = new File(currentPath+File.separator+item);
        File dest = new File(mDirs.filesDir+"Go_"+item+"_shared.zip");
        if(dest.exists())
            dest.delete();
        try {
            Utils.zip(source, dest);
        }
        catch(Exception ex){
            activity.showError("Could not share folder","Folder compression failed",false);
            ex.printStackTrace();
        }
        if(dest.exists()){
            shareFile(dest);
        }
    }

    void shareFile(final File fileToShare){
        //http://stackoverflow.com/a/28874567
        Uri contentUri = FileProvider.getUriForFile(activity, "derp.goforandroid.fileprovider", fileToShare);
        Intent mailIntent = new Intent(Intent.ACTION_SEND);
        mailIntent.setType("message/rfc822");
        //mailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        //mailIntent.putExtra(Intent.EXTRA_TEXT, body);
        mailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        try {
            startActivity(Intent.createChooser(mailIntent, "Share File"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, "Could not contact provider", Toast.LENGTH_SHORT).show();
        }
    }

    void renameFileDialog(final String item){
        final EditText txtInput = new EditText(activity);
        txtInput.setSingleLine(true);
        txtInput.setText(item);
        new AlertDialog.Builder(activity)
                .setTitle("Set new name")
                .setMessage("")
                .setView(txtInput)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String name = txtInput.getText().toString();
                        renameFile(item,name);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    void copyToSD(String item){
        if(!mDirs.externalStorageAvailable()){
            switch(Environment.getExternalStorageState()){
                case(MEDIA_SHARED):
                    activity.showError("Could not copy to SD card","SD card is inaccessible when shared via USB mass Storage Mode",false);
                    break;
                case(MEDIA_UNMOUNTED):
                    activity.showError("Could not copy to SD card","SD card is not mounted",false);
                    break;
                case(MEDIA_BAD_REMOVAL):
                    activity.showError("Could not copy to SD card","SD card is not mounted",false);
                    break;
                case(MEDIA_MOUNTED_READ_ONLY):
                    activity.showError("Could not copy to SD card","SD card is read only",false);
                    break;
                default:
                    activity.showError("Could not copy to SD card","SD card is not available",false);
            }
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            //Toast.makeText(activity,"You need to grant permission to copy files to the SD Card",Toast.LENGTH_LONG).show();
            activity.requestSDPermission();
            pendingCopy = item;
            return;
        }
        String sdPath = Environment.getExternalStorageDirectory()+File.separator+"Go"+File.separator;
        String relPath = mDirs.getRelativePath(currentPath.getAbsolutePath());
        String newPath = sdPath+relPath;
        if(new File(newPath).mkdirs() || new File(newPath).exists()){
            try {
                InputStream in = new FileInputStream(new File(currentPath + File.separator + item));
                OutputStream out = new FileOutputStream(new File(newPath + File.separator + item));
                Utils.copyFile(in, out);
                Toast.makeText(activity,"File copied to: "+newPath,Toast.LENGTH_LONG).show();
            }
            catch(Exception ex){
                activity.showError("Could not copy to SD Card","Failed to copy file",false);
                ex.printStackTrace();
            }
        }
        else{
            activity.showError("Could not copy to SD Card","Failed to create directories",false);
        }
    }
    void renameFile(String oldName, String name){
        String oldPath = currentPath+File.separator+oldName;
        String newPath = currentPath+File.separator+name;
        if((new File(oldPath).getParentFile().getAbsolutePath()+File.separator).equals(mDirs.GOPATH))
            return;
        if(oldPath.endsWith(goExt) && !newPath.endsWith(goExt)){
            newPath+=goExt;
        }
        if(!new File(oldPath).renameTo(new File(newPath))){
            activity.showError("Error","File rename operation failed",false);
        }
        else{
            updateFiles(currentPath);
        }
    }
    void updateFiles(File path){
        adapter.clear();
        loadFileList(path);
        adapter.addAll(fileList);
        adapter.notifyDataSetChanged();
        activity.lastFolderPath = path.toString();
    }

    void createNew(int which){
        String what = "";
        if(createProject){
            if(which==0)
                what="Script Package";
            else
                what="Library Package";
        }
        else{
            if(which==0)
                what="Go File";
            else
                what="New Folder";
        }
        final String w = what;
        final EditText txtInput = new EditText(activity);
        txtInput.setSingleLine(true);
        //txtInput.setHint("");

        new AlertDialog.Builder(activity)
                .setTitle("Create New "+what)
                .setMessage("Enter "+what+" Name")
                .setView(txtInput)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String name = txtInput.getText().toString();
                        if(w.equals("Script Package"))
                            createNewScriptProject(name);
                        else if(w.equals("Library Package"))
                            createNewLibraryProject(name);
                        else if (w.equals("Go File"))
                            createNewScript(name);
                        else
                            createNewFolder(name);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    void createNewFolder(String name){
        try {
            File newFile = new File(currentPath + File.separator + name);
            if(!newFile.mkdirs())
                throw new Exception("Could not create folder");
            updateFiles(currentPath);

        }catch(Exception ex){
            ex.printStackTrace();
            activity.showError("Create Script Failed",ex.toString(),false);
        }
    }

    void createNewScriptProject(String name){
        //if(name.contains(".") || name.contains("\n")){
        //    activity.showError("Invalid Name","Directory names cannot contain dots or newlines",false);
        //}
        try {
            File newFile = new File(mDirs.srcDir + name + File.separator + name + goExt);
            if (!new File(mDirs.srcDir + name).mkdirs() || !newFile.createNewFile())
                throw new Exception("Could not create project files");
            updateFiles(new File(mDirs.srcDir + name));
            switchToEditFrag();
            getEditFragment().loadNewFile(false,newFile.toString(),activity,name);
        }catch(Exception ex){
            ex.printStackTrace();
            activity.showError("Create Project Failed",ex.toString(),false);
        }
    }
    void createNewScript(String name){
        if(name.endsWith(goExt)){
            name=name.substring(0,name.length()-3);
        }
        try {
            File newFile = new File(currentPath + File.separator + name + goExt);
            if(!newFile.createNewFile())
                throw new Exception("Could not create file");
            /*InputStream in = activity.getAssets().open("stub.go");
            OutputStream out = new FileOutputStream(newFile);
            Utils.copyFile(in, out);*/
            switchToEditFrag();
            updateFiles(currentPath);
            getEditFragment().loadNewFile(false,newFile.toString(),activity,"");

        }catch(Exception ex){
            ex.printStackTrace();
            activity.showError("Create Script Failed",ex.toString(),false);
        }
    }
    void createNewLibraryProject(String name){
        if(name.endsWith(goExt)){
            name=name.substring(0,name.length()-3);
        }
        try {
            File newFile = new File(mDirs.srcDir + name + File.separator + name + goExt);
            if (!new File(mDirs.srcDir + name).mkdirs() || !newFile.createNewFile())
                throw new Exception("Could not create project files");
            updateFiles(new File(mDirs.srcDir + name));
            switchToEditFrag();
            getEditFragment().loadNewFile(true,newFile.toString(),activity,name);

        }catch(Exception ex){
            ex.printStackTrace();
            activity.showError("Create Library Failed",ex.toString(),false);
        }
    }
    private EditFragment getEditFragment(){
        return ((EditFragment) activity.pageAdapter.getItem(1));
    }

    private void removeCorrespondingTab(String path) {
        for(int i=0;i<TabsDialog.tabsList.size();i++){
            String s = TabsDialog.tabsList.get(i);
            if (s.endsWith("*"))
                s = s.substring(0, s.length() - 1);
            if( mDirs.getRelativePath(path).equals(s)){
                EditFragment.tabsDialog.removeTab(i);
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {//http://stackoverflow.com/a/6425744
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    private void deleteFileDialog(final File path){
        if((path.getParentFile().getAbsolutePath()+File.separator).equals(mDirs.GOPATH) || path.toString().equals(mDirs.binDir+"gocode")) {
            Toast.makeText(activity, "You cannot remove this file", Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = "Are you sure you want to delete this ";
        if(path.isDirectory())
            msg+="folder?";
        else
            msg+="file?";
        new AlertDialog.Builder(activity)
                .setTitle("Confirm")
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        deleteRecursive(path);
                        removeCorrespondingTab(path.toString());
                        updateFiles(currentPath);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    private void loadFileList(File path) {
        this.currentPath = path;
        ArrayList<String> r = new ArrayList<String>();
        if (path.exists()) {
            if (!(path.getParentFile().getAbsolutePath()+File.separator).equals(mDirs.filesDir)) {
                r.add("..");
                if(!path.toString().startsWith(mDirs.binDir.substring(0,mDirs.binDir.length()-1)))
                    activity.findViewById(R.id.newButton).setVisibility(View.VISIBLE);
            }
            else{
                activity.findViewById(R.id.newButton).setVisibility(View.INVISIBLE);
            }
            //list directories before files
            FilenameFilter filefilter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    if (!sel.canRead()) return false;
                    if(sel.isDirectory()) return false;
                    //boolean endsWith = goExt != null ? filename.toLowerCase().endsWith(goExt) : true;
                    return true;
                }
            };
            FilenameFilter dirfilter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    if (!sel.canRead()) return false;
                    if(!sel.isDirectory()) return false;
                    return true;
                }
            };
            String[] fileList = path.list(filefilter);
            String[] dirList = path.list(dirfilter);
            if(fileList==null || dirList == null) {
                activity.showError("File load failed","Could not read folder",false);
                return;
            }
            for (String file : dirList) {
                r.add(file);
            }
            for (String file : fileList) {
                r.add(file);
            }
        }
        fileList = r;
    }
    void switchToEditFrag(){
        ((TabLayout)activity.findViewById(R.id.sliding_tabs)).getTabAt(1).select();
    }

    public static FoldersFragment newInstance(String param1, String param2) {
        FoldersFragment fragment = new FoldersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_folders, container, false);
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
    @Override
    public void onStop(){
        super.onStop();
        activity.currentFolderPath = this.currentPath.toString();
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}

class CustomFolderAdapter<String> extends ArrayAdapter<String> {
    //String [] fileNames;
    MainActivity activity;
    FoldersFragment frag;
    private static LayoutInflater inflater=null;
    public CustomFolderAdapter(MainActivity mainActivity, int layoutResourceId, ArrayList<String> files, FoldersFragment fr) {
        super(mainActivity,layoutResourceId,files);
        activity = mainActivity;
        //fileNames = (String[]) files.toArray();
        frag = fr;
        inflater = ( LayoutInflater ) activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public class Holder
    {
        TextView tv;
        ImageView img;
        Button dots;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder=new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.myfile_listitem, null);
        holder.tv=(TextView) rowView.findViewById(R.id.textView1);
        holder.img=(ImageView) rowView.findViewById(R.id.imageView1);
        holder.dots = (Button) rowView.findViewById(R.id.moreButton);
        holder.tv.setText(getItem(position).toString());
        if( new File(frag.currentPath+File.separator+getItem(position).toString()).isDirectory() )
            holder.img.setImageResource(R.drawable.folder_icon_blue);
        else if(getItem(position).toString().endsWith(".go"))
            holder.img.setImageResource(R.drawable.file_icon_blue);
        if(getItem(position).toString().equals("..")) {
            holder.dots.setVisibility(View.GONE);
        }
        else{
            holder.dots.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    frag.fileOrFolderActionDialog(position);
                }
            });
        }
        return rowView;
    }
}