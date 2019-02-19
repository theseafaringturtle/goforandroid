package derp.goforandroid;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.client.methods.HttpUriRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import static android.os.Environment.MEDIA_BAD_REMOVAL;
import static android.os.Environment.MEDIA_MOUNTED_READ_ONLY;
import static android.os.Environment.MEDIA_SHARED;
import static android.os.Environment.MEDIA_UNMOUNTED;

//holds any FoldersFragment methods that do more than just interact with the file system
public class FolderUtils {

    MainActivity activity;
    FoldersFragment frag;

    public FolderUtils(MainActivity activity, FoldersFragment frag){
        this.activity = activity;
        this.frag = frag;
    }

    void importRepoDialog(){
        LayoutInflater inflater = ( LayoutInflater ) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.dialog_git, null);//(ViewGroup)this.getView()
        new AlertDialog.Builder(activity)
                .setTitle("Clone Git Repository")
                .setMessage("")
                .setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String frontUrl = ((EditText)dialogView.findViewById(R.id.gitUrl)).getText().toString();
                        final String branch = ((EditText)dialogView.findViewById(R.id.gitBranch)).getText().toString();
                        importGitRepo(frontUrl,branch);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
    void importGitRepo(final String frontUrl, final String branch){
        final URL actualURL;
        String folderName;
        //do not confuse front url (e.g. golang.org) with actual repo URL! (github, googlesource etc)
        try{
            actualURL = new URL(frontUrl);
            folderName = actualURL.getHost() + actualURL.getPath();
            folderName = folderName.replace(".git","");
        }
        catch(java.net.MalformedURLException ex){
            activity.showError("Not Valid","Invalid repo URL",false);
            return;
        }
        final String folder = folderName;
        final ProgressDialog progressDialog = ProgressDialog.show(activity,
                "Git",
                "Cloning repo...", true);
        //Android does not implement the Java standard fully, therefore JGit is broken and held together with duct tape
        Runnable gitRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (branch.isEmpty()) {
                        Git.cloneRepository()
                                .setURI(actualURL.toString())
                                .setDirectory(new File(frag.currentPath+File.separator+folder))
                                .call();
                    } else {
                        Git.cloneRepository()
                                .setURI(actualURL.toString())
                                .setDirectory(new File(frag.currentPath+File.separator+folder))
                                .setBranchesToClone(Arrays.asList("refs/heads/" + branch))
                                .setBranch("refs/heads/" + branch)
                                .call();
                    }
                }
                catch(final GitAPIException ex){//todo use better checks for success/failure
                    frag.deleteRecursive(new File(frag.currentPath+File.separator+folder));
                    if(!scanHtmlForImports(frontUrl))
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.showError("Couldn't fetch repository",ex.toString(),false);
                            }
                    });
                }
                catch(Exception ex){
                    ex.printStackTrace();
                }
                finally{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            frag.updateFiles(frag.currentPath);
                            if(progressDialog != null)
                                progressDialog.dismiss();
                            if(new File(frag.currentPath+File.separator+folder).exists())
                                Toast.makeText(activity,"Repo cloned successfully",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };
        Thread gitThread = new Thread(gitRunnable,"gitfetcher");
        gitThread.start();
    }
    private boolean scanHtmlForImports(String url){
        try {
            Document doc = Jsoup.connect(url).timeout(5000).get();
            Elements metas = doc.select("meta[name=go-import]");
            String content = metas.attr("content");
            String[] tokens = content.split(" ");
            String front = tokens[0];
            if (!tokens[1].equals("git")){
                Toast.makeText(activity,"Repository does not use git",Toast.LENGTH_SHORT).show();
                return true;
            }
            Git.cloneRepository()
                    .setURI(tokens[2])
                    .setDirectory(new File(frag.currentPath+File.separator+front))
                    .call();
            return true;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    void importNewFile(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        String strType = "*/*";
        intent.setDataAndType(Uri.parse(frag.currentPath.getAbsolutePath()), strType);
        activity.startActivityForResult(intent, 123);
    }

    void importFileFromUri(Uri uri){
        String destPath = frag.currentPath + File.separator + activity.getFileNameFromUri(uri);
        try {
            InputStream in = activity.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(destPath);
            Utils.copyFile(in, out);
            frag.updateFiles(frag.currentPath);
        }catch(Exception ex){
            activity.showError("Could not import file",ex.toString(),false);
            ex.printStackTrace();
        }
    }
    void shareFolder(String item){
        File source = new File(frag.currentPath+File.separator+item);
        File dest = new File(frag.mDirs.filesDir+"Go_"+item+"_shared.zip");
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
            frag.startActivity(Intent.createChooser(mailIntent, "Share File"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, "Could not contact provider", Toast.LENGTH_SHORT).show();
        }
    }
    void copyToSD(String item){
        if(!frag.mDirs.externalStorageAvailable()){
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
            frag.pendingCopy = item;
            return;
        }
        String sdPath = Environment.getExternalStorageDirectory()+File.separator+"Go"+File.separator;
        String relPath = frag.mDirs.getRelativePath(frag.currentPath.getAbsolutePath());
        String newPath = sdPath+relPath;
        if(new File(newPath).mkdirs() || new File(newPath).exists()){
            try {
                InputStream in = new FileInputStream(new File(frag.currentPath + File.separator + item));
                OutputStream out = new FileOutputStream(new File(newPath + File.separator + item));
                Toast.makeText(activity,"Copying file...",Toast.LENGTH_SHORT).show();
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
}
