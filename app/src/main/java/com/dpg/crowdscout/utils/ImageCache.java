package com.dpg.crowdscout.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Caches downloaded images via the app-global HttpResponseCache, as well as creates and caches thumbnails.
 */
public class ImageCache {
    private static final String LOG_TAG = ImageCache.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final int THUMBNAIL_HEADER = 0xf00f0010;
    private static final int THUMBNAIL_CACHE_MAX_SIZE = 50 * 1024 * 1024; // 50 MiB

    // The minimum size that needs to be deleted from the thumbnail cache when it gets full.
    // Otherwise we would need to keep on deleting stuff all the time after it peaks.
    private static final int THUMBNAIL_CACHE_DELETE_SIZE = 5 * 1024 * 1024; // 5 MiB

    private static boolean s_thumbnailCacheInitialized = false;
    private static long s_thumbnailCacheSize = 0;

    public static abstract class DownloadImageTask extends AsyncTask<URL, Void, BitmapDrawable> {
        private BitmapFactory.Options m_options;
        private Context m_context;

        public DownloadImageTask() {
            m_options = null;
        }

        public DownloadImageTask(BitmapFactory.Options options) {
            m_options = options;
        }

        /**
         * Downloads a Image Resource, caches it and returns a BitmapDrawable.
         *
         * @param context - to set the correct target density in the resulting BitmapDrawable
         */
        public DownloadImageTask(@NonNull Context context) {
            m_context = context.getApplicationContext();
            m_options = null;
        }

        public DownloadImageTask(@NonNull Context context, BitmapFactory.Options options) {
            this(context);
            m_options = options;
        }

        @Override
        protected BitmapDrawable doInBackground(URL... urls) {
            Log.d(LOG_TAG, "DownloadImageTask URLs: " + Arrays.toString(urls));
            if (urls.length > 0) {
                return getDrawableBitmapFromUrl(urls[0], m_options, m_context);
            } else {
                throw new RuntimeException("URL must be passed to DownloadImageTask.execute()");
            }
        }
    }

    private static boolean setLastModifiedDateForFile(File file) {
        boolean result = true;

        // cannot use system implementation of setLastModified,
        // see http://code.google.com/p/android/issues/detail?id=18624
        final boolean isAndroidBuggy = true;
        if (!isAndroidBuggy) {
            result = file.setLastModified(System.currentTimeMillis());
        } else {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(file, "rw");
                long length = raf.length();
                raf.setLength(length + 1);
                raf.setLength(length);
            } catch (IOException e) {
                result = false;
                e.printStackTrace();
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        // Ignore this exception.
                    }
                }
            }
        }

        return result;
    }

    private static String getBitmapFileCacheDir() {
        return Helpers.INSTANCE.getAppContext().getCacheDir() + File.separator + "bitmapcache";
    }

    public static String getBitmapThumbnailCacheDir() {
        String dir = getBitmapFileCacheDir() + "/thumb";
        if (!s_thumbnailCacheInitialized) {
            File baseDir = new File(dir);
            if (baseDir.exists() && baseDir.isDirectory() && baseDir.canRead() && baseDir.canWrite()) {
                s_thumbnailCacheInitialized = true;
            } else {
                if (baseDir.mkdirs()) {
                    s_thumbnailCacheInitialized = true;
                } else {
                    Log.w(LOG_TAG, "getBitmapThumbnailCacheDir: could not create baseDir " + baseDir);
                }
            }
        }
        Log.d(LOG_TAG, "getBitmapThumbnailCacheDir dir: " + dir);
        return dir;
    }

    private static String getCacheFilename(URL url) {
        final String rep = "_";
        String filename = url.getPath().replace("/", rep);

        String query = url.getQuery();
        if (query != null && !query.isEmpty()) {
            query = query.replace("/", rep)
                    .replace("?", rep)
                    .replace("\\", rep)
                    .replace("<", rep)
                    .replace(">", rep)
                    .replace("&", rep)
                    .replace("%", rep);
            filename = filename.concat(query);
        }

        return filename;
    }

    private static BitmapDrawable getDrawableFromBitmap(Bitmap bitmap, @Nullable Context context) {
        if (bitmap != null) {
            BitmapDrawable bitmapDrawable = null;
            if (context == null) {
                bitmapDrawable = new BitmapDrawable(bitmap);
            } else {
                bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
            }
            if (bitmapDrawable != null
                    && bitmapDrawable.getIntrinsicWidth() > -1
                    && bitmapDrawable.getIntrinsicHeight() > -1) {
                return bitmapDrawable;
            }
        }
        return null;
    }

    /**
     * Downloads an image from an URL.
     *
     * @param imageURL URL to the image
     * @return bitmap The BitmapDrawable object for the image
     */
    private static BitmapDrawable getDrawableBitmapFromUrl(URL imageURL, BitmapFactory.Options options,
                                                           @Nullable Context context) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        HttpURLConnection boundsConnection = null;
        try {
            connection = (HttpURLConnection) imageURL.openConnection();

            if (connection.getContentType() != null && connection.getContentType().startsWith("image/")) {
                if (options != null && options.inSampleSize == -1 && options.outHeight > 0) {
                    // Create a new connection from the pool as we cannot reset the original connection's stream after
                    // decoding the bounds. Note that even wrapping the original stream in a BufferedInputStream and
                    // using mark() / reset() does not work reliably.
                    boundsConnection = (HttpURLConnection) imageURL.openConnection();
                    InputStream boundsStream = boundsConnection.getInputStream();

                    final int reqHeight = options.outHeight;
                    options.inSampleSize = 1;
                    options.outHeight = -1;

                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(boundsStream, null, options);

                    if (options.outHeight > 0) {
                        final int heightRatio = Math.round((float) options.outHeight / (float) reqHeight);
                        options.inSampleSize = Integer.highestOneBit(heightRatio);
                        Log.d(LOG_TAG, "Decoding bitmap with a sample size of " + options.inSampleSize);
                    }

                    options.inJustDecodeBounds = false;
                    boundsStream.close();
                }

                InputStream stream = connection.getInputStream();

                // Returns null if the image data could not be decoded.
                bitmap = BitmapFactory.decodeStream(stream, null, options);

                stream.close();
            }

            if (DEBUG) {
                final HttpResponseCache cache = HttpResponseCache.getInstalled();
                if (cache != null) {
                    Log.d(LOG_TAG, "HTTP cache stats");
                    Log.d(LOG_TAG, "Hit count: " + cache.getHitCount());
                    Log.d(LOG_TAG, "Net requests: " + cache.getNetworkCount());
                    Log.d(LOG_TAG, "Total requests: " + cache.getRequestCount());
                }
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, String.format("Could not load Bitmap from (%s)", imageURL), ioe);
        } catch (OutOfMemoryError oome) {
            // it should not be caught but better than crash.
            Log.e(LOG_TAG, String.format("Could not load Bitmap from (%s)", imageURL), oome);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                // ignore the exception
            }
            try {
                if (boundsConnection != null) {
                    boundsConnection.disconnect();
                }
            } catch (Exception e) {
                // ignore the exception
            }
        }

        return getDrawableFromBitmap(bitmap, context);
    }

    private static class FileDateComparator implements Comparator<File>, Serializable {
        static final long serialVersionUID = 1L;

        @Override
        public int compare(File lhs, File rhs) {
            if (lhs.lastModified() < rhs.lastModified()) {
                return -1;
            } else if (lhs.lastModified() > rhs.lastModified()) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * Remove the oldest files from the cache until we drop under a defined threshold.
     */
    private static void pruneThumbnailCache() {
        if (s_thumbnailCacheSize >= THUMBNAIL_CACHE_MAX_SIZE) {
            File thumbnailDir = new File(getBitmapThumbnailCacheDir());
            File[] files = thumbnailDir.listFiles();
            List<File> fileList = Arrays.asList(files);
            Collections.sort(fileList, new FileDateComparator());

            int i = 0;
            while (s_thumbnailCacheSize >= THUMBNAIL_CACHE_MAX_SIZE - THUMBNAIL_CACHE_DELETE_SIZE) {
                File fileToDelete = fileList.get(i);
                long fileSize = fileToDelete.length();
                if (fileToDelete.delete()) {
                    s_thumbnailCacheSize -= fileSize;
                }
                ++i;
            }
        }
    }

    /**
     * Calculate the size of the thumbnail cache.
     *
     * @return the amount of bytes that the thumbnail cache presently consumes.
     */
    private static long getThumbnailCacheSize() {
        String dir = getBitmapThumbnailCacheDir();
        return Helpers.INSTANCE.calculateSizeRecursive(new File(dir));
    }

    /**
     * Saves the passed bitmap as a thumbnail, using the sourceURL to create the filename.
     */
    public static void saveThumbnail(final Bitmap bitmap, final URL sourceURL) {
        String dir = getBitmapThumbnailCacheDir();
        String filename = getCacheFilename(sourceURL);
        File file = new File(dir, filename);
        FileOutputStream out = null;

        if (DEBUG) {
            Log.d(LOG_TAG, "Saving thumbnail for " + sourceURL + " to " + file);
        }

        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (s_thumbnailCacheSize == 0) {
            s_thumbnailCacheSize = getThumbnailCacheSize();
        }

        s_thumbnailCacheSize += file.length();
        pruneThumbnailCache();
    }

    // Load thumbnail from disk if it exists.
    public static BitmapDrawable loadThumbnail(final URL originalURL) {
        BitmapDrawable drawable = null;

        String path = getCacheFilename(originalURL);
        File file = new File(getBitmapThumbnailCacheDir(), path);
        if (file.exists()) {
            // Update the last modification date so this file does not get pruned since we used it now.
            if (!setLastModifiedDateForFile(file)) {
                Log.w(LOG_TAG, "loadThumbnail: Could not update modification date for file " + file);
            }

            FileInputStream fis = null;
            DataInputStream dis = null;
            try {
                fis = new FileInputStream(file);
                dis = new DataInputStream(fis);

                final int header = dis.readInt();

                // Skip over width and height, which are now unused, if this is our custom thumbnail format.
                if (header == THUMBNAIL_HEADER) {
                    dis.readInt();
                    dis.readInt();
                } else {
                    dis.close();
                    fis = new FileInputStream(file);
                }

                drawable = getDrawableFromBitmap(BitmapFactory.decodeStream(fis), null);
                dis.close();
                fis.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "loadThumbnail: Failed loading thumbnail from cache file " + file, e);
            } finally {
                try {
                    if (dis != null) {
                        dis.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    // Ignore this.
                }
            }
        }

        if (DEBUG) {
            if (drawable != null) {
                Log.d(LOG_TAG, "Thumbnail cache hit for file " + file);
            }
        }

        return drawable;
    }
}
