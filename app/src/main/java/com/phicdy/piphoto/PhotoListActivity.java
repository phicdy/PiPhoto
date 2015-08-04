package com.phicdy.piphoto;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.phicdy.piphoto.util.FileUtil;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;


public class PhotoListActivity extends ActionBarActivity {

    private String host = "";
    private String user = "";
    private String password = "";
    private ArrayList<String> photoPathList;

    private GridView gridView;
    private MyProgressDialogFragment progressDialog;
    private MyProgressDialogFragment downloadProgressDialog;
    private BroadcastReceiver receiver;

    private static final String UPDATE_LS = "updateLs";
    private static final String REFRESH_PHOTOS = "refreshPhotos";
    private static final String FINISH_TAKING_PHOTO = "finishTakingPhoto";
    private static final String FAIL = "fail";
    private static final String FAIL_REASON = "failReason";
    private static final String LS_RESULT = "lsResult";
    private static final String PHOTO_NAME = "photoName";

    private static final int START_RECORD = 1;

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
    @Override
    protected void onResume() {
        super.onResume();
        setReceiver();
    }

    @Override
    protected void onPause() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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

    private void setReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(UPDATE_LS)) {
                    tvLsResult.setVisibility(View.VISIBLE);
                    tvLsResult.setText(intent.getStringExtra(LS_RESULT));
                } else if (action.equals(REFRESH_PHOTOS)) {
                    downloadProgressDialog.getDialog().dismiss();
                    refreshPhotos();
                } else if(action.equals(FINISH_TAKING_PHOTO)) {
                    progressDialog.getDialog().dismiss();
                    String photoName = intent.getStringExtra(PHOTO_NAME);
                    if (photoName != null && !photoName.equals("")) {
                        downloadPhoto(photoName);
                    }
                } else if(action.equals(FAIL)) {
                    String failReason = intent.getStringExtra(FAIL_REASON);
                    Toast.makeText(getApplicationContext(), getString(R.string.fail) + ":" + failReason, Toast.LENGTH_LONG).show();

                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_LS);
        filter.addAction(REFRESH_PHOTOS);
        filter.addAction(FINISH_TAKING_PHOTO);
        filter.addAction(FAIL);
        registerReceiver(receiver, filter);
    }

    private void takePhoto() {
        new Thread() {
            @Override
            public void run() {
                Session session = null;
                Channel channel = null;
                String photoName = null;
                try {
                    session = createSession();
                    session.connect();

                    String flodlerpath = FileUtil.getSDCardRootPath() + "/photos";

                    File saveFolder  = new File(flodlerpath);
                    if(!saveFolder.exists()) {
                        saveFolder.mkdir();
                    }

                    DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss",
                            Locale.JAPAN);
                    String now = dateFormat.format(new Date());
                    photoName = now + ".jpg";

                    String command = "raspistill -vf -hf -o " + FileUtil.PHOTO_FOLDER_NAME + "/" + photoName;
                    channel = session.openChannel("exec");
                    ((ChannelExec) channel).setCommand(command);
                    channel.setInputStream(null);
                    ((ChannelExec) channel).setErrStream(System.err);

                    channel.connect();
                } catch (JSchException e) {
                    e.printStackTrace();
                } finally {
                    if (channel != null) {
                        channel.disconnect();
                    }
                    if (session != null) {
                        session.disconnect();
                    }
                    if (photoName != null && !photoName.equals("")) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(FINISH_TAKING_PHOTO);
                        intent.putExtra(PHOTO_NAME, photoName);
                        sendBroadcast(intent);
                    }else {
                        sendBroadcast(new Intent(FINISH_TAKING_PHOTO));
                    }
                }
            }
        }.start();
        progressDialog = MyProgressDialogFragment.newInstance(getString(R.string.taking_photo));
        progressDialog.show(getFragmentManager(), null);
    }

    private void takePhotoRegularly(final String interval, final String endDate) {
        new Thread() {
            @Override
            public void run() {
                Session session = null;
                Channel channel = null;
                try {
                    session = createSession();
                    session.connect();

                    Log.d("PiPhoto", "Connection success");
                    String flodlerpath = FileUtil.getSDCardRootPath() + "/photos";

                    File saveFolder  = new File(flodlerpath);
                    if(!saveFolder.exists()) {
                        saveFolder.mkdir();
                    }

                    String command = "python scripts/takePhotoRegularly.py " + interval + " " + endDate;
                    channel = session.openChannel("exec");
                    ((ChannelExec) channel).setCommand(command);
                    channel.setInputStream(null);
                    ((ChannelExec) channel).setErrStream(System.err);

                    channel.connect();
                } catch (JSchException e) {
                    Log.d("PiPhoto", "Connection failed");
                    e.printStackTrace();
                } finally {
                    if (channel != null) {
                        channel.disconnect();
                    }
                    if (session != null) {
                        session.disconnect();
                    }
                }
            }
        }.start();
    }

    private void downloadPhoto(final String fileName) {
        new Thread() {
            @Override
            public void run() {
                Session session = null;
                ChannelSftp channel = null;
                try {
                    session = createSession();
                    session.connect();
                    channel = (ChannelSftp) session.openChannel("sftp");
                    channel.connect();

                    // ls
                    Vector list = channel.ls(FileUtil.PHOTO_FOLDER_NAME);
                    // System.out.println(list.get(0).getClass().getName());
                    for (int i = 0; i < list.size(); i++) {
                        Log.d("PiPhoto", String.valueOf(list.get(i)));
                    }

                    // lstat
                    String filePath = FileUtil.PHOTO_FOLDER_NAME + "/" + fileName;
                    try {
                        SftpATTRS stat = channel.lstat(filePath);
                    } catch (SftpException e) {
                        e.printStackTrace();
                        Log.d("PiPhoto", "Failed to lstat");
                        if (channel != null) {
                            channel.disconnect();
                        }
                        if (session != null) {
                            session.disconnect();
                        }
                        return;
                    }

                    String folderPath = FileUtil.getPhotoFolderPath();

                    File saveFolder = new File(folderPath);
                    if (!saveFolder.exists()) {
                        saveFolder.mkdir();
                    }
                    channel.get(filePath, folderPath + fileName);

                    session.disconnect();
                    channel.disconnect();
                } catch (SftpException e) {
                    e.printStackTrace();
                } catch (JSchException e) {
                    e.printStackTrace();
                } finally {
                    if (channel != null) {
                        channel.disconnect();
                    }
                    if (session != null) {
                        session.disconnect();
                    }
                    sendBroadcast(new Intent(REFRESH_PHOTOS));
                }

            }
        }.start();
        downloadProgressDialog = MyProgressDialogFragment.newInstance(getString(R.string.downloading));
        downloadProgressDialog.show(getFragmentManager(), null);
    }

    private void downloadAllPhotos() {
        new Thread() {
            @Override
            public void run() {
                Session session = null;
                ChannelSftp channel = null;
                try {
                    session = createSession();
                    session.connect();
                    Log.d("PiPhoto", "Connection success");
                    channel = (ChannelSftp) session.openChannel("sftp");
                    channel.connect();

                    // ls
                    Vector list = channel.ls(FileUtil.PHOTO_FOLDER_NAME);
                    // System.out.println(list.get(0).getClass().getName());
                    for (int i = 0; i < list.size(); i++) {
                        Log.d("PiPhoto", String.valueOf(list.get(i)));
                        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)list.get(i);
                        String rasPiFilePath = FileUtil.PHOTO_FOLDER_NAME + "/" + entry.getFilename();
                        if (!rasPiFilePath.endsWith(".jpg")) {
                            continue;
                        }
                        String outputFolderPath = FileUtil.getPhotoFolderPath();

                        File saveFolder = new File(outputFolderPath);
                        if (!saveFolder.exists()) {
                            saveFolder.mkdir();
                        }
                        String outputFilePath = outputFolderPath + entry.getFilename();
                        if (new File(outputFilePath).exists()) {
                            continue;
                        }
                        channel.get(rasPiFilePath, outputFilePath);
                    }

                    session.disconnect();
                    channel.disconnect();
                } catch (SftpException e) {
                    Log.d("PiPhoto", "Connection failed");
                    Intent intent = new Intent(FAIL);
                    intent.putExtra(FAIL_REASON, "SftpException");
                    sendBroadcast(intent);
                    e.printStackTrace();
                } catch (JSchException e) {
                    Log.d("PiPhoto", "Connection failed");
                    Intent intent = new Intent(FAIL);
                    intent.putExtra(FAIL_REASON, "JSchException");
                    sendBroadcast(intent);
                    e.printStackTrace();
                } finally {
                    if (channel != null) {
                        channel.disconnect();
                    }
                    if (session != null) {
                        session.disconnect();
                    }
                    sendBroadcast(new Intent(REFRESH_PHOTOS));
                }

            }
        }.start();
        downloadProgressDialog = MyProgressDialogFragment.newInstance(getString(R.string.downloading));
        downloadProgressDialog.show(getFragmentManager(), null);
    }


    private Session createSession() {
        JSch jsch = new JSch();

        // connect session
        Session session = null;
        try {
            session = jsch.getSession(user, host, 22);

            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return session;
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
