package derp.goforandroid;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.design.widget.TabItem;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    String GO = "Go";
    String SETTINGS = "GoSettings";

    Dirs mDirs;

    List<Fragment> fList = new ArrayList<Fragment>();
    MyPageAdapter pageAdapter;
    CustomViewPager pager;

    String currentFolderPath = "";
    String lastFolderPath = "";
    String currentFilePath = "";
    String lastFilePath = "";

    private static final int WRITE_EXT_STORAGE_REQUEST = 0;
    //GDrive drive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mDirs = new Dirs(this);
        deleteTempFiles();
        updateUserPrefs();

        fList.add(FoldersFragment.newInstance());
        fList.add(EditFragment.newInstance(false));
        fList.add(ConsoleFragment.newInstance(false));

        pageAdapter = new MyPageAdapter(getSupportFragmentManager(), fList);
        pager = (CustomViewPager) findViewById(R.id.viewpager);
        pager.setAdapter(pageAdapter);
        addPagerListeners();

        SharedPreferences sharedPreferences = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        if (!mDirs.checkFiles() || !(sharedPreferences.contains("extracted"))) {
            findViewById(R.id.sliding_tabs).setVisibility(GONE);
            findViewById(R.id.sliding_tabs).setEnabled(false);
            pageAdapter.fragments.add(pageAdapter.fragments.size(), InstallFragment.newInstance(false));
            pageAdapter.notifyDataSetChanged();
            pager.setCurrentItem(3);
            pager.setPagingEnabled(false);

        } else {
            installFinished();
        }
        final MainActivity that = this;
        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {//http://stackoverflow.com/a/23545532
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(that, "derp.goforandroid.PrefsActivity");
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, PrefsFragment.class.getName());//http://stackoverflow.com/a/10960720
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivityForResult(intent, 0);
            }
        });
        //drive = new GDrive(this);
    }

    public String getFileNameFromUri(Uri uri) {//https://stackoverflow.com/a/25005243
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case 0:
                updateUserPrefs();
                break;
            case 123:
                if(data==null) return;
                Uri uri = data.getData();
                if(pager.getCurrentItem() == 0)
                    ((FoldersFragment)pageAdapter.getItem(0)).utils.importFileFromUri(uri);
        }
    }

    public void updateUserPrefs() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String fsize = sharedPrefs.getString("fontSize", "18");
        EditFragment.fontSize = Integer.parseInt(fsize);
        EditCode.highlightLine = sharedPrefs.getBoolean("highlightLine", false);
        EditCode.drawLineNums = sharedPrefs.getBoolean("lineNumbers", true);
        EditCode.autoComplete = sharedPrefs.getBoolean("autoComplete", true);
        PrettifyHighlighter.syntaxHighlighting = sharedPrefs.getString("syntaxHighlighting", "Simple");
    }

    public void addPagerListeners() {
        ((TabLayout) findViewById(R.id.sliding_tabs)).addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1 && EditFragment.noTabs) {//no open files
                    ((TabLayout) findViewById(R.id.sliding_tabs)).removeOnTabSelectedListener(this);
                    ((TabLayout) findViewById(R.id.sliding_tabs)).getTabAt(0).select();
                    pager.setCurrentItem(0);
                    ((TabLayout) findViewById(R.id.sliding_tabs)).addOnTabSelectedListener(this);
                    return;
                }
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position != 3)
                    ((TabLayout) findViewById(R.id.sliding_tabs)).getTabAt(position).select();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //drive.initClient();
    }



    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("lastFile", currentFilePath);
        editor.putString("lastFolder", currentFolderPath);
        editor.commit();
        //drive.stopClient();
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            FoldersFragment frag = (FoldersFragment) pageAdapter.fragments.get(0);
            if (frag.currentPath!=null && !(frag.currentPath + File.separator).equals(mDirs.GOPATH.toString())) {//frag.currentPath!=null fix for reported crash
                frag.updateFiles(frag.currentPath.getParentFile());
                return;
            }
        }
        if(TabsDialog.unsavedTabs.size()>0){
            new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Some files have not been saved, are you sure you want to quit?")
                    .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        }
        else {
            finish();
        }
    }

    public void installFinished() {
        //(findViewById(R.id.tabItem3)).performClick();
        /*Fragment edFragment = EditFragment.newInstance("",false);
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frag_holder, edFragment);
        ft.commit();*/
        restoreLastFileandFolder();
        findViewById(R.id.sliding_tabs).setVisibility(VISIBLE);
        findViewById(R.id.sliding_tabs).setEnabled(true);
        if (pageAdapter.fragments.get(pageAdapter.fragments.size() - 1) instanceof InstallFragment) {
            pageAdapter.fragments.remove(pageAdapter.fragments.size() - 1);
            pageAdapter.notifyDataSetChanged();
        }
        if (!lastFilePath.equals("")) {
            EditFragment.noTabs = false;
            ((TabLayout) findViewById(R.id.sliding_tabs)).getTabAt(1).select();
        } else
            ((TabLayout) findViewById(R.id.sliding_tabs)).getTabAt(0).select();
        //pager.setCurrentItem(1,true);
        pager.setPagingEnabled(true);
    }

    void deleteTempFiles() {
        for (File f : new File(mDirs.filesDir).listFiles()) {
            if (f.toString().endsWith("_shared.zip")) {
                f.delete();
            }
        }
        File ac = new File(mDirs.filesDir + "autocomplete.go");
        if (ac.exists())
            ac.delete();
    }

    void showError(String title, String msg, final boolean fatal) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (fatal) finish();
                            }
                        }
                ).show();
    }

    public void restoreLastFileandFolder() {
        SharedPreferences sharedPreferences = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        String lastFile = sharedPreferences.getString("lastFile", "");
        if (!lastFile.equals("")) {
            lastFilePath = lastFile;
        }
        String lastFolder = sharedPreferences.getString("lastFolder", "");
        if (!lastFolder.equals("")) {
            lastFolderPath = lastFolder;
        }
    }

    void requestSDPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_EXT_STORAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXT_STORAGE_REQUEST: {
                //todo maybe permission not set yet when dialog returns
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (pager.getCurrentItem() == 0) {
                        FoldersFragment frag = (FoldersFragment) pageAdapter.fragments.get(0);
                        if(frag.pendingCopy != null) {
                            frag.utils.copyToSD(frag.pendingCopy);
                            frag.pendingCopy = null;
                        }
                    }
                } else {
                    showError("Permission not granted", "SD Card permission required to copy files to the SD Card", false);
                }
            }
        }
    }


    class MyPageAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments;

        public MyPageAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return this.fragments.get(position);
        }

        @Override
        public int getCount() {
            return this.fragments.size();
        }
    }
}

