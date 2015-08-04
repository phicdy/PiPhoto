package com.phicdy.piphoto.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by phicdy on 15/07/31.
 */
public class FileUtil {

    public static final String PHOTO_FOLDER_NAME = "photos";
    public static final String EXTRA_PHOTO = "extraPhotos";

    public static File getPhotoFolder() {
        return new File(getSDCardRootPath() + PHOTO_FOLDER_NAME);
    }

    public static String getPhotoFolderPath() {
        return getSDCardRootPath() + PHOTO_FOLDER_NAME + "/";
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
            }else {
                sdCardRootPath = Environment.getExternalStorageDirectory().getPath();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        if (sdCardRootPath != null && !sdCardRootPath.endsWith("/")) {
            sdCardRootPath += "/";
        }

        return sdCardRootPath;
    }
}
