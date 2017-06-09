package derp.goforandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by User on 05/04/2017.
 */

public class Dirs {
    Context mContext;

    String filesDir = "";
    String GOROOT = "";
    String GOPATH = "";
    String goExePath = "";
    String goToolsDir="";
    //user dirs in GOPATH
    String srcDir;
    String pkgDir;
    String binDir;
    //String SETTINGS = "GoSettings";

    String compFolder = "go-linux-arm";

    public Dirs(Context context){
        mContext = context;
        filesDir = mContext.getFilesDir().getAbsolutePath()+ File.separator;

        GOROOT = filesDir+compFolder;
        goExePath = GOROOT+File.separator+"bin"+File.separator;
        goToolsDir = GOROOT+File.separator+"pkg"+ File.separator+"tool"+File.separator+"linux_arm"+File.separator;

        //SharedPreferences sharedPreferences = mContext.getSharedPreferences(SETTINGS, MODE_PRIVATE);
        //boolean extStorage = sharedPreferences.getBoolean("externalStorage",false);
        //if(extStorage)
        //    GOPATH = Environment.getExternalStorageDirectory()+File.separator+"GoForAndroid";
        //else
            GOPATH = filesDir+"go"+File.separator;
        srcDir = GOPATH+"src"+File.separator;
        pkgDir = GOPATH+"pkg"+File.separator+"linux_arm"+File.separator;
        binDir = GOPATH+"bin"+File.separator;
    }

    public boolean checkFiles(){
        File f = new File(filesDir+compFolder+File.separator+"VERSION");
        return f.exists();
    }
    public String getRelativePath(String path){
        if(GOPATH.length() > path.length())
            return "";
        return path.substring(GOPATH.length(), path.length());
    }
    public String getPackageName(String path){
        if(srcDir.length() > path.length())
            return "";
        String rel = path.substring(srcDir.length(), path.length());
        rel=rel.substring(0,rel.lastIndexOf(File.separator));
        return rel;
    }
    public String getBinNameFromPackageName(String pkgName){
        if(pkgName.lastIndexOf(File.separator)== -1)
            return pkgName;
        else
            return pkgName.substring(pkgName.lastIndexOf(File.separator)+1,pkgName.length());
    }
    boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}
