package com.flybd.installer;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HostPluginCache {

    private static final String CACHE_FILE = "host_plugin_cache";

    private Context context;

    public HostPluginCache(Context context) {
        this.context = context;
        Log.i("HostPluginCache", "dir " + context.getFilesDir());
    }

    public boolean saveCache(byte[] bytes, boolean isPlugin) {
        return saveCache(CACHE_FILE, bytes, isPlugin);
    }

    public boolean saveCache(String key, byte[] bytes, boolean isPlugin) {
        File root;
        try {
            if (isPlugin) {
                root = context.getFilesDir().getParentFile().getParentFile().getParentFile();
            } else {
                root = context.getFilesDir();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        File file = new File(root, key);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public byte[] readCache(boolean isPlugin) {
        return readCache(CACHE_FILE, isPlugin);
    }

    public byte[] readCache(String key, boolean isPlugin) {
        File root;
        try {
            if (isPlugin) {
                root = context.getFilesDir().getParentFile().getParentFile().getParentFile();
            } else {
                root = context.getFilesDir();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        File file = new File(root, key);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] tmp = new byte[10 * 1024];
            int len;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while ((len = is.read(tmp, 0, tmp.length)) >= 0) {
                buffer.write(tmp, 0, len);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void removeCache(String key, boolean isPlugin) {
        File root;
        try {
            if (isPlugin) {
                root = context.getFilesDir().getParentFile().getParentFile().getParentFile();
            } else {
                root = context.getFilesDir();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        File file = new File(root, key);
        if (file.exists()) {
            file.delete();
        }
    }
}
