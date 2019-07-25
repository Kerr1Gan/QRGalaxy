package com.galaxy.android.async;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import com.common.utils.file.FileUtil;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by KerriGan on 2017/6/18.
 */
public class AppThumbTask extends AsyncTask<File, Void, Bitmap> {

    private LruCache<String, Bitmap> mLruCache;

    private WeakReference<Context> mContext;

    private WeakReference<ImageView> mImageView;

    public AppThumbTask(LruCache<String, Bitmap> lruCache, Context context, ImageView imgView) {
        mLruCache = lruCache;
        mContext = new WeakReference<>(context);
        mImageView = new WeakReference<>(imgView);
    }

    @Override
    protected Bitmap doInBackground(File... files) {
        File f = files[0];
        Context ctx = mContext.get();
        if (ctx == null) {
            return null;
        }
        try {
            Bitmap bit = FileUtil.INSTANCE.getAppThumbnail(ctx, f);
            if (bit != null) {
                mLruCache.put(f.getAbsolutePath(), bit);
            }
            return bit;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onPostExecute(Bitmap result) {
        super.onPostExecute(result);
        if (result != null) {
            if (mImageView.get() != null) {
                mImageView.get().setImageBitmap(result);
            }
        }
        mLruCache = null;
        mImageView = null;
        mContext = null;
    }

    @Override
    public void onCancelled() {
        super.onCancelled();
        mImageView = null;
        mLruCache = null;
        mContext = null;
    }
}
