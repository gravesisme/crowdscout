package com.dpg.crowdscout.utils;


import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.dpg.crowdscout.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Extends functionality provided by ImageCache.DownloadImageTask to ensure that a
 * similar download is not already in progress. Will also load the image into the
 * specified ImageView once the download has completed. Will additionally cancel
 * active downloads when views are recycled and the active download does not correspond
 * to the image url that should now be downloaded.
 *
 * Reference:
 * http://android-developers.blogspot.jp/2010/07/multithreading-for-performance.html
 */
public class ImageDownloader {
    private static final String LOG_TAG = ImageDownloader.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Dimension that will be initialized once using the screen dimensions. If
     * a target dimension is not provided and the downloaded image is bigger than
     * the screen's dimensions, it will be resized accordingly
     */
    private static volatile BitmapDimension s_maxDimension;

    private final Resources m_resources;
    private Bitmap m_placeholder;
    private BitmapDimension m_targetDimension;
    private BitmapFactory.Options m_bitmapFactoryOptions = new BitmapFactory.Options();
    private Executor m_executor;
    private int m_fadeInTime = 200;
    private boolean m_fadeIn = false;
    private boolean m_downloadingEnabled = true;

    public static class BitmapDimension {
        public int width;
        public int height;

        public BitmapDimension(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    public ImageDownloader(Context context) {
        this(context, null);
    }

    public ImageDownloader(Context context, Bitmap placeholder) {
        m_resources = context.getResources();
        m_placeholder = placeholder;

        // Setup the BitmapFactory options
        m_bitmapFactoryOptions.inPurgeable = true;
        m_bitmapFactoryOptions.inInputShareable = true;
        m_bitmapFactoryOptions.inMutable = true;

        // If the max dimension has not been initialized yet, create it now using the width of the screen
        if (s_maxDimension == null) {
            synchronized (ImageDownloader.class) {
                final DisplayMetrics displayMetrics = m_resources.getDisplayMetrics();
                s_maxDimension = new BitmapDimension(displayMetrics.widthPixels, displayMetrics.heightPixels);
                if (DEBUG) {
                    Log.d(LOG_TAG, "ImageDownloader(): MAX_DIMENSIONS initialized to: " + s_maxDimension);
                }
            }
        }
    }

    public void setBitmapFactoryOptions(BitmapFactory.Options bitmapFactoryOptions) {
        m_bitmapFactoryOptions = bitmapFactoryOptions;
    }

    public void setBitmapSampleSize(int sampleSize) {
        if (m_bitmapFactoryOptions != null) {
            m_bitmapFactoryOptions.inSampleSize = sampleSize;
        }
    }

    public void setBitmapTempStorageSize(int size) {
        if (m_bitmapFactoryOptions != null) {
            m_bitmapFactoryOptions.inTempStorage = new byte[size];
        }
    }

    public void setFadeInTime(int fadeInTime) {
        m_fadeInTime = fadeInTime;
    }

    public void setPlaceholder(Bitmap placeholder) {
        m_placeholder = placeholder;
    }

    public void setTargetDimension(BitmapDimension targetDimension) {
        m_targetDimension = targetDimension;
    }

    public void setExecutor(Executor threadExecutor) {
        m_executor = threadExecutor;
    }

    public void enableFadeIn() {
        m_fadeIn = true;
    }

    public void disableFadeIn() {
        m_fadeIn = false;
    }

    public void enableDownloading() {
        m_downloadingEnabled = true;
    }

    public void disableDownloading() {
        m_downloadingEnabled = false;
    }

    public boolean isDownloadingEnabled() {
        return m_downloadingEnabled;
    }

    public boolean isFadeInEnabled() {
        return m_fadeIn;
    }

    /**
     * Downloads the image from the specified URL - as long as there isn't
     * a same download already in progress - and will load it into the specified ImageView
     * as long as the ImageView is still in memory
     *
     * @param imageUrl    {@link String} The URL of the image that should be downloaded
     * @param imageView   {@link ImageView} The ImageView that the image will be loaded into
     * @param placeholder Image to display while downloading, and to fade in
     *                    from when such is enabled.
     * @param oomBitmap   Image to display on out-of-memory failure.   TBD
     * @param errorBitmap Image to display on any other error.   TBD
     * @throws MalformedURLException
     */
    public void loadImageInto(final String imageUrl, final ImageView imageView,
                              final Bitmap placeholder, BitmapDimension targetDimension,
                              final Bitmap oomBitmap,
                              final Bitmap errorBitmap) {

        // If a target dimension was not provided, use the max dimensions
        if (targetDimension == null && s_maxDimension != null) {
            targetDimension = new BitmapDimension(s_maxDimension.width, s_maxDimension.height);
        }

        // If an image cache is in use, check to see if it exists in the ImageCache
        if (m_imageCache != null) {
            Bitmap bitmap = getBitmapFromCache(imageUrl, targetDimension);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                return;
            }
        }

        // Use placeholder image when downloading is disabled
        if (!m_downloadingEnabled) {
            imageView.setImageBitmap(placeholder);
            return;
        }

        try {
            final URL url = new URL(imageUrl);
            // If the image hasn't been downloaded yet, try to download it - unless a download is already in progress
            if (cancelPotentialDownload(imageUrl, imageView, placeholder)) {
                final BitmapDownloaderTask task = new BitmapDownloaderTask(
                        imageView, imageUrl, targetDimension,
                        oomBitmap, errorBitmap);
                final DownloadedDrawable downloadedDrawable = new DownloadedDrawable(
                        m_resources, placeholder, task);

                imageView.setImageDrawable(downloadedDrawable);

                // If an Executor has been provided, use that; otherwise, execute normally
                if (m_executor != null) {
                    task.executeOnExecutor(m_executor, url);
                } else {
                    task.execute(url);
                }
            }
        } catch (MalformedURLException e) {
            if (DEBUG) {
                Log.e(LOG_TAG, "MalformedURLException Encountered. Unable to load image url: " + imageUrl, e);
            }
            // Use placeholder image as backup
            imageView.setImageBitmap(errorBitmap);
        }
    }

    public void loadImageInto(final String imageUrl, final ImageView imageView,
                              final Bitmap placeholder, BitmapDimension targetDimension) {
        loadImageInto(imageUrl, imageView, placeholder, targetDimension, placeholder, placeholder);
    }

    /**
     * Convenience method where a custom dimensions is required without a custom placeholder
     *
     * @param imageUrl
     * @param imageView
     * @param targetDimension
     */
    public void loadImageInto(final String imageUrl, final ImageView imageView, BitmapDimension targetDimension) {
        loadImageInto(imageUrl, imageView, m_placeholder, targetDimension);
    }

    /**
     * Convenience method where a custom placeholder is required without a custom dimension
     *
     * @param imageUrl
     * @param imageView
     * @param placeholder
     */
    public void loadImageInto(final String imageUrl, final ImageView imageView, final Bitmap placeholder) {
        loadImageInto(imageUrl, imageView, placeholder, m_targetDimension);
    }

    /**
     * Convenience method where instance variables for targetDimension and placeholder will be used
     *
     * @param imageUrl
     * @param imageView
     */
    public void loadImageInto(final String imageUrl, final ImageView imageView) {
        loadImageInto(imageUrl, imageView, m_placeholder, m_targetDimension);
    }

    public void cancelDownload(ImageView imageView) {
        Bitmap placeholder = m_placeholder != null ? m_placeholder : getPlaceholderBitmap(imageView);
        cancelDownload(imageView, placeholder);
    }

    public void cancelDownload(ImageView imageView, Bitmap placeholder) {
        cancelDownloadAndShowDrawable(imageView, placeholder == null
                ? null
                : new BitmapDrawable(m_resources, placeholder));
    }

    public void cancelDownloadAndShowDrawable(ImageView imageView, Drawable placeholder) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
        if (bitmapDownloaderTask != null) {
            bitmapDownloaderTask.cancel(true);
        }

        if (placeholder == null) {
            imageView.setImageDrawable(null);
        } else {
            imageView.setImageDrawable(placeholder);
        }
    }

    /**
     * Called when the processing is complete and the final drawable should be
     * set on the ImageView.
     *
     * @param imageView
     * @param bitmap
     */
    private void setImageDrawable(@NonNull ImageView imageView, Bitmap bitmap) {
        final Bitmap placeholder;

        // If bitmap is null or fadeIn is enabled, we will need the placeholder image
        if (bitmap == null || m_fadeIn) {
            placeholder = m_placeholder != null ? m_placeholder : getPlaceholderBitmap(imageView);
        } else {
            placeholder = null;
        }

        // Fade in image as long as both the placeholder and provided bitmap are valid
        if (m_fadeIn && placeholder != null && bitmap != null) {
            fadeInImage(imageView, placeholder, bitmap);
        }

        // Otherwise, ignore fade in transition and just set the image
        else {
            // If both the placeholder and provided bitmap are invalid, just remove any
            // existing drawables from the image view
            if (bitmap == null && placeholder == null) {
                imageView.setImageDrawable(null);
            } else {
                // Otherwise, use either the placeholder or provided bitmap
                imageView.setImageDrawable(new BitmapDrawable(m_resources, bitmap == null ? placeholder : bitmap));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private void fadeInImage(@NonNull ImageView imageView, @NonNull Bitmap fromBitmap, @NonNull Bitmap toBitmap) {
        final Drawable drawable = (new BitmapDrawable(m_resources, toBitmap));

        // Transition drawable with a transparent drawable and the final drawable
        TransitionDrawable td =
                new TransitionDrawable(new Drawable[]{
                        new ColorDrawable(android.R.color.transparent),
                        drawable
                });

        // Set background to fromBitmap
        Drawable bitmapDrawable = new BitmapDrawable(m_resources, fromBitmap);
        Helpers.INSTANCE.setBackground(imageView, bitmapDrawable);

        // Set image
        imageView.setImageDrawable(td);

        // Specify transition time
        td.startTransition(m_fadeInTime);

        // Remove background drawable once transition completes
        final ImageView view = imageView;
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    view.setBackgroundDrawable(null);
                } else {
                    view.setBackground(null);
                }
            }
        }, m_fadeInTime + 1);
    }

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    private class BitmapDownloaderTask extends AsyncTask<URL, Void, Bitmap> {
        /**
         * ImageView is stored as a WeakReference, so that a download in progress
         * does not prevent a killed activity's ImageView from being garbage collected
         */
        private final WeakReference<ImageView> m_imageViewReference;
        private final String m_imageUrl;
        private final BitmapDimension m_bitmapDimension;
        private final WeakReference<Bitmap> m_oomBitmapReference;
        private final WeakReference<Bitmap> m_errorBitmapReference;

        public BitmapDownloaderTask(ImageView imageView, String imageUrl,
                                    BitmapDimension targetDimension,
                                    Bitmap oomBitmap, Bitmap errorBitmap) {
            super();
            m_imageViewReference = new WeakReference<ImageView>(imageView);
            m_oomBitmapReference = new WeakReference<Bitmap>(oomBitmap);
            m_errorBitmapReference = new WeakReference<Bitmap>(errorBitmap);
            m_imageUrl = imageUrl;
            m_bitmapDimension = targetDimension;
        }

        @Override
        protected Bitmap doInBackground(URL... urls) {
            // Lower thread priority to limit impact on list scrolling
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            Bitmap bitmap = null;

            // Download the passed in url
            if (urls != null && urls.length > 0) {
                bitmap = getBitmapFromURL(urls[0], m_bitmapFactoryOptions,
                        m_oomBitmapReference.get(),
                        m_errorBitmapReference.get());
            }

            if (bitmap != null) {

                // If a target dimension was specified, resize the bitmap now
                if (m_bitmapDimension != null) {
                    bitmap = resizeBitmap(bitmap, m_bitmapDimension);
                }

                // Update the cache
                if (m_imageCache != null) {
                    addBitmapToCache(m_imageUrl, bitmap, m_bitmapDimension);
                }
            }

            return bitmap;
        }

        /**
         * Once the image is downloaded, associate it with the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                if (DEBUG) Log.d(LOG_TAG, "onPostExecute(): isCancelled() == true.. returning");
                return;
            }

            ImageView imageView = getAttachedImageView();
            if (imageView != null) {
                // Change bitmap only if this process is still associated with it
                setImageDrawable(imageView, bitmap);
            }
        }

        /**
         * Returns the ImageView associated with this task as long as the ImageView's task still
         * points to this task as well. Returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            // Make sure the image view reference still exists
            ImageView imageView = m_imageViewReference == null ? null : m_imageViewReference.get();

            if (imageView != null) {
                // Get the download task associated with the image view
                BitmapDownloaderTask bitmapWorkerTask = getBitmapDownloaderTask(imageView);

                // Return the image view only if this process is still associated with it
                if (this == bitmapWorkerTask) {
                    return imageView;
                }
            }

            return null;
        }

        public String getImageUrl() {
            return m_imageUrl;
        }
    }

    /**
     * A fake Drawable that will be attached to the imageView while the download is in progress.
     * <p/>
     * Contains a reference to the actual download task, so that a download task can be stopped
     * if a new binding is required, and makes sure that only the last started download process can
     * bind its result, independently of the download finish order.
     */
    private static class DownloadedDrawable extends BitmapDrawable {
        // WeakReferences to limit object dependencies
        private final WeakReference<BitmapDownloaderTask> m_bitmapDownloaderTaskReference;
        private final WeakReference<Bitmap> m_placeholderReference;

        public DownloadedDrawable(Resources res, Bitmap placeholder, BitmapDownloaderTask bitmapDownloaderTask) {
            super(res, placeholder);
            m_placeholderReference = new WeakReference<Bitmap>(placeholder);
            m_bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return m_bitmapDownloaderTaskReference.get();
        }

        /**
         * could return null
         */
        public Bitmap getPlaceholder() {
            if (m_placeholderReference != null) {
                return m_placeholderReference.get();
            }
            return null;
        }
    }

    /**
     * could return null.
     */
    private static Bitmap getPlaceholderBitmap(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                return ((DownloadedDrawable) drawable).getPlaceholder();
            }
        }
        return null;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated with this imageView.
     * null if there is no such task.
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    private static Bitmap resizeBitmap(Bitmap bitmap, BitmapDimension targetDimension) {
        int targetWidth = targetDimension.width;
        int targetHeight = targetDimension.height;

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Resize based on one dimensions
        if (targetWidth <= 0) {
            targetWidth = Math.round(((float) targetHeight / bitmapHeight) * bitmapWidth);
        } else if (targetHeight <= 0) {
            targetHeight = Math.round(((float) targetWidth / bitmapWidth) * bitmapHeight);
        }

        if (DEBUG) {
            Log.d(LOG_TAG, "resizeBitmap(): Original Dimensions ("
                            + (new BitmapDimension(bitmapWidth, bitmapHeight).toString())
                            + ") => Target Dimensions ("
                            + (new BitmapDimension(targetWidth, targetHeight).toString())
                            + ")"
            );
        }

        // If the image is already smaller than the target dimensions, return the original
        if ((targetWidth >= bitmapWidth) && (targetHeight >= bitmapHeight)) {
            return bitmap;
        } else {
            // If one of the target dimensions is smaller than the current dimensions, resize the bitmap
            return ThumbnailUtils.extractThumbnail(bitmap, targetWidth, targetHeight);
        }
    }

    /**
     * Returns true if the current download has been canceled or if there was no download in
     * progress on this image view.
     * Returns false if the download in progress deals with the same url. The download is not
     * stopped in that case.
     */
    private static boolean cancelPotentialDownload(String url, ImageView imageView, Bitmap placeholder) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.getImageUrl();
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
                imageView.setImageBitmap(placeholder);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * Downloads an image from the specified URL and returns it as a Bitmap
     * If a problem arises during download, it will return null
     * Inspiration: http://stackoverflow.com/a/8993175/768104
     *
     * @param url         {@link URL} The image url that should be downloaded
     * @param oomBitmap   Bitmap to use on out-of-memory situation.  Could be null.
     * @param errorBitmap Bitmap to use on error situation.  Could be null.
     * @return {@link Bitmap} Requested image as bitmap or null if there was a problem downloading the image
     */
    private static Bitmap getBitmapFromURL(URL url,
                                           BitmapFactory.Options bitmapFactoryOptions,
                                           Bitmap oomBitmap,
                                           Bitmap errorBitmap) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        if (url == null) {
            return null;
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Cache-Control", "max-stale=86400");
            connection.setUseCaches(true);
            connection.setInstanceFollowRedirects(true);
            inputStream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapFactoryOptions);
        } catch (OutOfMemoryError oome) {
            // under normal circumstances it doesn't make sense to deal with OOM
            // but in the case of image loading large objects are created and the app might well be
            // able to continue to operate using the available memory

            // for example of a crash see:
            // https://rink.hockeyapp.net/manage/apps/51176/app_versions/14/crash_reasons/9817654
            bitmap = oomBitmap;
            if (DEBUG) {
                Log.e(LOG_TAG, String.format("Could not load Bitmap from (%s) due to OutOfMemoryError",
                        url.toString()), oome);
                oome.printStackTrace();
            }
        } catch (IOException ioe) {
            bitmap = errorBitmap;
            if (DEBUG) {
                Log.e(LOG_TAG, String.format("Could not load Bitmap from (%s)", url.toString()), ioe);
                ioe.printStackTrace();
            }
        } finally {
            // Close the input stream
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore -- there's nothing we can do.
                }
            }

            // Disconnect from the connection
            if (connection != null) {
                connection.disconnect();
            }
        }

        return bitmap;
    }

    // *******************************************************
    // Image Cache Mediator
    // *******************************************************

    private ImageCache m_imageCache;

    private static final Object IMAGE_CACHE_LOCK = new Object();

    public void setImageCache(final ImageCache imageCache) {
        synchronized (IMAGE_CACHE_LOCK) {
            m_imageCache = imageCache;
        }
    }

    public void addBitmapToCache(final String imageUrl, final Bitmap bitmap, final BitmapDimension bitmapDimension) {
        synchronized (IMAGE_CACHE_LOCK) {
            if (m_imageCache != null) {
                final String key = getKey(imageUrl, bitmapDimension);
                m_imageCache.putBitmap(key, bitmap);
            }
        }
    }

    public Bitmap getBitmapFromCache(final String imageUrl, final BitmapDimension bitmapDimension) {
        synchronized (IMAGE_CACHE_LOCK) {
            if (m_imageCache != null) {
                final String key = getKey(imageUrl, bitmapDimension);
                return m_imageCache.getBitmap(key);
            }
            return null;
        }
    }

    private static String getKey(String imageUrl, BitmapDimension bitmapDimension) {
        if (bitmapDimension == null) {
            return imageUrl;
        } else {
            return (imageUrl + ":" + bitmapDimension.toString());
        }
    }

    // *******************************************************
    // Image Cache
    // *******************************************************

    public static interface ImageCache {
        Bitmap getBitmap(String key);

        void putBitmap(String key, Bitmap bitmap);

        void clear();

        void destroy();
    }

    public static class BitmapLruCache extends LruCache<String, Bitmap> implements ImageCache {
        /**
         * Stores a WeakReference to the last BitmapLruCache object created, which is used to copy content
         * from old image caches to new image caches whenever newInstance() is triggered
         */
        public static WeakReference<BitmapLruCache> s_previousInstance;

        /**
         * Helper that will return the last ImageCache created - if it still exists
         */
        static BitmapLruCache getPreviousInstance() {
            return s_previousInstance == null ? null : s_previousInstance.get();
        }

        /**
         * Factory method for creating a new ImageCache
         */
        public static BitmapLruCache newInstance(Context context) {
            int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
            // Use 1/8th of the available memory for this memory cache.
            int cacheSize = 1024 * 1024 * memClass / 8;

            // Create a new image cache
            final BitmapLruCache cache = new BitmapLruCache(cacheSize);

            // Get the last instance created
            final BitmapLruCache previousInstance = getPreviousInstance();

            // If the last instance created exists, copy its contents into the current ImageCache
            if (previousInstance != null) {
                cache.copyContents(previousInstance);

                // Destroy the previous cache once its contents have been copied to the new instance
                previousInstance.destroy();
            }

            // Update the weak reference
            s_previousInstance = new WeakReference<BitmapLruCache>(cache);

            // Return new ImageCache
            return cache;
        }

        public BitmapLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getRowBytes() * value.getHeight();
        }

        @Override
        public Bitmap getBitmap(String url) {
            return get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            put(url, bitmap);
        }

        @Override
        public void clear() {
            evictAll();
        }

        /**
         * In addition to clearing all entries, this method will also remove the static weak reference if
         * the active LruCache is the same as the object being held in the s_previousInstance weak reference
         */
        @Override
        public void destroy() {
            // Clear all entries
            clear();

            // If the current instance is also the value contained in the static weak reference, clear the reference
            final BitmapLruCache previousInstance = s_previousInstance == null ? null : s_previousInstance.get();
            if (this == previousInstance) {
                if (DEBUG) {
                    Log.d(LOG_TAG, "destroy(): Clearing s_previousInstance");
                }
                s_previousInstance.clear();
                s_previousInstance = null;
            } else if (DEBUG) {
                Log.d(LOG_TAG, "destroy(): Letting s_previousInstance live another day");
            }
        }

        /**
         * Helper used to copy the contents of another BitmapLruCache into the active one
         *
         * @param bitmapLruCache {@link BitmapLruCache} The BitmapLruCache to copy content from
         */
        private void copyContents(@NonNull BitmapLruCache bitmapLruCache) {
            final Map<String, Bitmap> snapshot = bitmapLruCache.snapshot();

            // If a snapshot could not be created, then there is nothing to copy
            if (snapshot == null) {
                return;
            }

            if (DEBUG) {
                Log.d(LOG_TAG, String.format("copyContents(): Copying (%d) entries into the current ImageCache", snapshot.size()));
            }

            // Add each entry from the snapshot to the current LruCache
            for (String key : snapshot.keySet()) {
                Bitmap bitmap = snapshot.get(key);
                if (bitmap != null) {
                    if (DEBUG) {
                        Log.d(LOG_TAG, "copyContent(): Copying bitmap (" + key + ") from old cache!");
                    }
                    put(key, bitmap);
                }
            }
        }
    }
}
