package com.phicdy.piphoto;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;


public class PhotoListActivity extends ActionBarActivity {

    private String host = "";
    private String user = "";
    private String password = "";
    private ArrayList<String> photoPathList;

    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        gridView = (GridView) findViewById(R.id.gv_photos);
        refreshPhotos();
        setAllListener();
    }

    private void refreshPhotos() {
        loadPhotoPathList();
        BitmapAdapter adapter = new BitmapAdapter(
                getApplicationContext(), R.layout.list_item_photo,
                photoPathList);
        gridView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void loadPhotoPathList() {
        photoPathList = new ArrayList<>();

        File photoFolder = FileUtil.getPhotoFolder();
        File[] photos = photoFolder.listFiles();
        for (int i = 0; i < photos.length; i++) {
            photoPathList.add(photos[i].getPath());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_photo_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void doProc() {
    private void setAllListener() {
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), PhotoActivity.class);
                intent.putExtra(FileUtil.EXTRA_PHOTO, photoPathList.get(position));
                startActivity(intent);
            }
        });
    }
        new Thread() {
            @Override
            public void run() {

                try {
                    JSch jsch = new JSch();

                    // connect session
                    Session session = jsch.getSession(user, host, 22);
                    session.setPassword(password);
                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.connect();

                    // sftp remotely
                    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                    channel.connect();

                    // ls
                    Vector list = channel.ls(".");
                    // System.out.println(list.get(0).getClass().getName());
                    for (int i = 0; i < list.size(); i++) {
                        Log.d("PiPhoto", String.valueOf(list.get(i)));
                    }

                    // lstat
                    try {
                        SftpATTRS stat = channel.lstat("test.jpg");
                    } catch (SftpException ex) {
                        ex.printStackTrace();
                    }

                    String path = getSDCardRootPath() + "/photos/test2.jpg";
                    String flodlerpath = getSDCardRootPath() + "/photos";

                    File saveFolder  = new File(flodlerpath);
                    if(!saveFolder.exists()) {
                        saveFolder.mkdir();
                    }
                    channel.get("./test.jpg", path);

                    session.disconnect();
                    channel.disconnect();

                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (SftpException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    public static String getAppPath(Context context) {
        PackageManager pkgMgr = context.getPackageManager();
        try {
            String path = pkgMgr.getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir;
            if(path.endsWith("/")) {
                return path;
            }else {
                return path + "/";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSDCardRootPath() {
        String sdCardRootPath = null;
        Scanner scanner = null;
        try {
            // Get mount information
            File fstab = new File("/system/etc/vold.fstab");
            if (fstab.exists()) {
                scanner = new Scanner(new FileInputStream(new File(
                        "/system/etc/vold.fstab")));
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("dev_mount")
                            || line.startsWith("fuse_mount")) {
                        sdCardRootPath = line.replaceAll("\t", " ").split(" ")[2];
                    }
                }
    private class BitmapAdapter extends ArrayAdapter<String> {

        private int resourceId;

        public BitmapAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(resourceId, parent, false);

                holder = new ViewHolder();
                holder.image = (ImageView)convertView;
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
                holder.image.setVisibility(View.INVISIBLE);
            }

            PhotoTask task = new PhotoTask(holder.image);
            task.execute(getItem(position));

            return convertView;
        }



        class ViewHolder {
            ImageView image;
        }
    }

    class PhotoTask extends AsyncTask<String, Void, Bitmap> {

        private ImageView mView;

        public PhotoTask(ImageView view) {
            mView = view;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return BitmapFactory.decodeFile(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(result, 100, 100, true);
            mView.setImageBitmap(resizedBitmap);
            mView.setVisibility(View.VISIBLE);
        }
    }
}
