package derp.goforandroid;

import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by User on 31/03/2017.
 */

public class Utils {
    static String TAG = "Go";

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
    public static void zip(File directory, File zipfile) throws IOException {
        URI base = directory.toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else {
                        zout.putNextEntry(new ZipEntry(name));
                        copyFile(new FileInputStream(kid), zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    public static String executeCommand(String[] command, HashMap envVars, String workingDir) throws IOException {
        String output = "";

        //InputStream inputStream = Runtime.getRuntime().exec(command).getInputStream();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(envVars);
        builder.directory(new File(workingDir));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        /*InputStream inputStream = process.getInputStream();

        while( inputStream.available() <= 0)
            try { Thread.sleep(500); } catch(Exception ex) {}

        java.util.Scanner s = new java.util.Scanner(inputStream);
        while(s.hasNext())
            output += s.nextLine() + "\n";
            */
        String line = "";
        BufferedReader in =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((line = in.readLine()) != null) {
            Log.d(TAG,line);
            output+=line+"\n";
        }
        in.close();

        return output;
    }
    public static void goGet(String pk,String opt){//test

    }


    public static int dpToPx(Resources res, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }
    public static int spToPx(Resources res, int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, res.getDisplayMetrics());
    }
}
