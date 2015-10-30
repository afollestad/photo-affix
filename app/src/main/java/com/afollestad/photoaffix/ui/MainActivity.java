package com.afollestad.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.photoaffix.BuildConfig;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.adapters.PhotoGridAdapter;
import com.afollestad.photoaffix.animation.HeightEvaluator;
import com.afollestad.photoaffix.animation.ViewHideAnimationListener;
import com.afollestad.photoaffix.data.Photo;
import com.afollestad.photoaffix.dialogs.AboutDialog;
import com.afollestad.photoaffix.dialogs.ImageSizingDialog;
import com.afollestad.photoaffix.dialogs.ImageSpacingDialog;
import com.afollestad.photoaffix.utils.Prefs;
import com.afollestad.photoaffix.utils.Util;
import com.afollestad.photoaffix.views.ColorCircleView;
import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity implements
        ColorChooserDialog.ColorCallback, ImageSpacingDialog.SpacingCallback,
        ImageSizingDialog.SizingCallback, DragSelectRecyclerViewAdapter.SelectionListener {

    private static final int PERMISSION_RC = 69;

    @Bind(R.id.appbar_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.list)
    public DragSelectRecyclerView mList;
    @Bind(R.id.affixButton)
    Button mAffixButton;
    @Bind(R.id.settingsFrame)
    ViewGroup mSettingsFrame;
    @Bind(R.id.empty)
    TextView mEmpty;

    @Bind(R.id.stackHorizontallySwitch)
    CheckBox mStackHorizontally;
    @Bind(R.id.bgFillColorCircle)
    ColorCircleView mBgFillColor;
    @Bind(R.id.bgFillColorLabel)
    TextView mBgFillColorLabel;
    @Bind(R.id.imagePaddingLabel)
    TextView mImagePaddingLabel;
    @Bind(R.id.removeBgButton)
    Button mRemoveBgFillBtn;

    private PhotoGridAdapter mAdapter;
    private Photo[] mSelectedPhotos;
    private int mTraverseIndex;

    private int mOriginalSettingsFrameHeight = -1;
    private ValueAnimator mSettingsFrameAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.clear) {
                    clearSelection();
                    return true;
                } else if (item.getItemId() == R.id.about) {
                    AboutDialog.show(MainActivity.this);
                    return true;
                }
                return false;
            }
        });

        mList.setLayoutManager(new GridLayoutManager(this,
                getResources().getInteger(R.integer.grid_width)));

        mAdapter = new PhotoGridAdapter(this);
        mAdapter.restoreInstanceState(savedInstanceState);
        mAdapter.setSelectionListener(this);
        mList.setAdapter(mAdapter);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        mList.setItemAnimator(animator);

        final int bgFillColor = Prefs.bgFillColor(this);
        mStackHorizontally.setChecked(Prefs.stackHorizontally(this));
        mBgFillColor.setColor(bgFillColor);
        final int[] padding = Prefs.imageSpacing(this);
        mImagePaddingLabel.setText(getString(R.string.image_spacing_x, padding[0], padding[1]));

        if (bgFillColor != Color.TRANSPARENT) {
            mRemoveBgFillBtn.setVisibility(View.VISIBLE);
            mBgFillColorLabel.setText(R.string.background_fill_color);
        } else {
            mBgFillColorLabel.setText(R.string.background_fill_color_transparent);
        }

        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null && intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null && uris.size() > 1) {
                mSelectedPhotos = new Photo[uris.size()];
                for (int i = 0; i < uris.size(); i++)
                    mSelectedPhotos[i] = new Photo(uris.get(i));
                mAffixButton.performClick();
            } else {
                Toast.makeText(this, R.string.need_two_or_more, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        if (mAdapter != null) mAdapter.saveInstanceState(outState);
    }

    private void refresh() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_RC);
            return;
        }
        Inquiry.init(this);
        Inquiry.get().selectFrom(Uri.parse("content://media/external/images/media"), Photo.class)
                .sort("datetaken DESC")
                .where("_data IS NOT NULL")
                .all(new GetCallback<Photo>() {
                    @Override
                    public void result(Photo[] photos) {
                        if (mEmpty != null) {
                            mAdapter.setPhotos(photos);
                            mEmpty.setVisibility(photos == null || photos.length == 0 ?
                                    View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RC)
            refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Inquiry.deinit();
    }

    public void clearSelection() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clearSelection();
                }
            });
            return;
        }
        mSelectedPhotos = null;
        mAdapter.clearSelected();
        mToolbar.getMenu().findItem(R.id.clear).setVisible(false);
    }

    @OnClick(R.id.removeBgButton)
    public void onClickRemoveBgFill() {
        mRemoveBgFillBtn.setVisibility(View.GONE);
        //noinspection ConstantConditions
        onColorSelection(null, Color.TRANSPARENT);
    }

    @OnClick(R.id.expandButton)
    public void onClickExpandButton(ImageView button) {
        if (mOriginalSettingsFrameHeight == -1) {
            final int settingControlHeight = (int) getResources().getDimension(R.dimen.settings_control_height);
            mOriginalSettingsFrameHeight = settingControlHeight * mSettingsFrame.getChildCount();
        }
        if (mSettingsFrameAnimator != null)
            mSettingsFrameAnimator.cancel();
        if (mSettingsFrame.getVisibility() == View.GONE) {
            mSettingsFrame.setVisibility(View.VISIBLE);
            button.setImageResource(R.drawable.ic_collapse);
            mSettingsFrameAnimator = ValueAnimator.ofObject(new HeightEvaluator(mSettingsFrame), 0, mOriginalSettingsFrameHeight);

        } else {
            button.setImageResource(R.drawable.ic_expand);
            mSettingsFrameAnimator = ValueAnimator.ofObject(new HeightEvaluator(mSettingsFrame), mOriginalSettingsFrameHeight, 0);
            mSettingsFrameAnimator.addListener(new ViewHideAnimationListener(mSettingsFrame));
        }
        mSettingsFrameAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mSettingsFrameAnimator.setDuration(200);
        mSettingsFrameAnimator.start();
    }

    @OnClick(R.id.affixButton)
    public void onClickAffixButton(View v) {
        v.setEnabled(false);
        mSelectedPhotos = mAdapter.getSelectedPhotos();
        try {
            startProcessing();
        } catch (OutOfMemoryError e) {
            Util.showMemoryError(MainActivity.this);
        }
        v.setEnabled(true);
    }

    @OnClick({R.id.settingStackHorizontally, R.id.settingBgFillColor, R.id.settingImagePadding})
    public void onClickSetting(View view) {
        switch (view.getId()) {
            case R.id.settingStackHorizontally:
                mStackHorizontally.setChecked(!mStackHorizontally.isChecked());
                Prefs.stackHorizontally(this, mStackHorizontally.isChecked());
                break;
            case R.id.settingBgFillColor:
                new ColorChooserDialog.Builder(this, R.string.background_fill_color_title)
                        .backButton(R.string.back)
                        .doneButton(R.string.done)
                        .allowUserColorInputAlpha(false)
                        .preselect(Prefs.bgFillColor(this))
                        .show();
                break;
            case R.id.settingImagePadding:
                new ImageSpacingDialog().show(getFragmentManager(), "[IMAGE_PADDING_DIALOG]");
                break;
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, @ColorInt int selectedColor) {
        if (selectedColor != Color.TRANSPARENT) {
            mRemoveBgFillBtn.setVisibility(View.VISIBLE);
            mBgFillColorLabel.setText(R.string.background_fill_color);
        } else {
            mBgFillColorLabel.setText(R.string.background_fill_color_transparent);
        }
        Prefs.bgFillColor(this, selectedColor);
        mBgFillColor.setColor(selectedColor);
    }

    @Override
    public void onSpacingChanged(int horizontal, int vertical) {
        Prefs.imageSpacing(this, horizontal, vertical);
        mImagePaddingLabel.setText(getString(R.string.image_spacing_x, horizontal, vertical));
    }

    @Size(2)
    private int[] getNextBitmapSize() {
        mTraverseIndex++;
        if (mTraverseIndex > mSelectedPhotos.length - 1)
            return null;
        Photo nextPhoto = mSelectedPhotos[mTraverseIndex];
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream is = null;
        try {
            is = Util.openStream(this, nextPhoto.getUri());
            BitmapFactory.decodeStream(is, null, options);
        } catch (Exception e) {
            Util.showError(this, e);
        } finally {
            Util.closeQuietely(is);
        }
        return new int[]{options.outWidth, options.outHeight};
    }

    private BitmapFactory.Options getNextBitmapOptions() {
        mTraverseIndex++;
        if (mTraverseIndex > mSelectedPhotos.length - 1)
            return null;
        Photo nextPhoto = mSelectedPhotos[mTraverseIndex];
        InputStream is = null;
        BitmapFactory.Options options = null;
        try {
            is = Util.openStream(this, nextPhoto.getUri());
            options = new BitmapFactory.Options();
            //Only the properties, so we can get the width/height
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
        } catch (Exception e) {
            Util.showError(this, e);
        } catch (OutOfMemoryError e2) {
            Util.showMemoryError(MainActivity.this);
        } finally {
            Util.closeQuietely(is);
        }
        return options;
    }

    private Bitmap getNextBitmap(BitmapFactory.Options options) {
        Photo nextPhoto = mSelectedPhotos[mTraverseIndex];
        InputStream is = null;
        Bitmap bm = null;
        try {
            is = Util.openStream(this, nextPhoto.getUri());
            bm = BitmapFactory.decodeStream(is, null, options);
        } catch (Exception e) {
            Util.showError(this, e);
        } catch (OutOfMemoryError e2) {
            Util.showMemoryError(MainActivity.this);
        } finally {
            Util.closeQuietely(is);
        }
        return bm;
    }

    public static void calculateInSampleSize(BitmapFactory.Options options, int reqHeight) {
        options.inSampleSize = options.outHeight / reqHeight;
    }

    private void startProcessing() {
        // Lock orientation so the Activity won't change configuration during processing
        Util.lockOrientation(this);

        final int[] imageSpacing = Prefs.imageSpacing(MainActivity.this);
        final int SPACING_HORIZONTAL = imageSpacing[0];
        final int SPACING_VERTICAL = imageSpacing[1];

        final boolean horizontal = mStackHorizontally.isChecked();
        int resultWidth;
        int resultHeight;

        if (horizontal) {
            Util.log("Horizontally stacking");
            // The width of the resulting image will be the largest width of the selected images
            // The height of the resulting image will be the sum of all the selected images' heights
            int totalWidth = 0;
            int maxHeight = -1;
            // Traverse all selected images and load in their sizes
            mTraverseIndex = -1;
            int[] size;
            while ((size = getNextBitmapSize()) != null) {
                totalWidth += size[0];
                if (maxHeight == -1)
                    maxHeight = size[1];
                else if (size[1] > maxHeight)
                    maxHeight = size[1];
            }
            // Compensate for spacing
            totalWidth += SPACING_HORIZONTAL * (mSelectedPhotos.length + 1);
            maxHeight += SPACING_VERTICAL * 2;

            // Crash avoidance
            if (totalWidth == 0) {
                Util.showError(this, new Exception("The total generated width is 0. Please notify me of this through the Google+ community."));
                return;
            } else if (maxHeight == 0) {
                Util.showError(this, new Exception("The max found height is 0. Please notify me of this through the Google+ community."));
                return;
            }

            // Print data and create large Bitmap
            Util.log("Total width = %d, max height = %d", totalWidth, maxHeight);
            resultWidth = totalWidth;
            resultHeight = maxHeight;
        } else {
            Util.log("Vertically stacking");
            // The height of the resulting image will be the largest height of the selected images
            // The width of the resulting image will be the sum of all the selected images' widths
            int totalHeight = 0;
            int maxWidth = -1;
            // Traverse all selected images and load in their sizes
            mTraverseIndex = -1;
            int[] size;
            while ((size = getNextBitmapSize()) != null) {
                totalHeight += size[1];
                if (maxWidth == -1)
                    maxWidth = size[0];
                else if (size[0] > maxWidth)
                    maxWidth = size[0];
            }
            // Compensate for spacing
            totalHeight += SPACING_VERTICAL * (mSelectedPhotos.length + 1);
            maxWidth += SPACING_HORIZONTAL * 2;

            // Crash avoidance
            if (totalHeight == 0) {
                Util.showError(this, new Exception("The total generated height is 0. Please notify me of this through the Google+ community."));
                return;
            } else if (maxWidth == 0) {
                Util.showError(this, new Exception("The max found width is 0. Please notify me of this through the Google+ community."));
                return;
            }

            // Print data and create large Bitmap
            Util.log("Max width = %d, total height = %d", maxWidth, totalHeight);
            resultWidth = maxWidth;
            resultHeight = totalHeight;
        }

        ImageSizingDialog.show(this, resultWidth, resultHeight);
    }

    @Override
    public void onSizingResult(double scale, int resultWidth, int resultHeight, Bitmap.CompressFormat format, int quality, boolean cancelled) {
        if (cancelled) {
            mTraverseIndex = -1;
            Util.unlockOrientation(this);
            return;
        }
        try {
            finishProcessing(scale, resultWidth, resultHeight, format, quality);
        } catch (OutOfMemoryError e) {
            Util.showMemoryError(this);
        }
    }

    private void finishProcessing(final double SCALE, int resultWidth, int resultHeight, final Bitmap.CompressFormat format, final int quality) {
        // Crash avoidance
        if (resultWidth == 0) {
            Util.showError(this, new Exception("The result width is 0. Please notify me of this through the Google+ community."));
            return;
        } else if (resultHeight == 0) {
            Util.showError(this, new Exception("The result height is 0. Please notify me of this through the Google+ community."));
            return;
        }

        Util.log("IMAGE SCALE = %s, total scaled width = %d, height = %d", SCALE, resultWidth, resultHeight);
        final Bitmap result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);

        final boolean horizontal = mStackHorizontally.isChecked();
        final int[] imageSpacing = Prefs.imageSpacing(MainActivity.this);
        final int SPACING_HORIZONTAL = (int) (imageSpacing[0] * SCALE);  // TODO should scale be multiplied here?
        final int SPACING_VERTICAL = (int) (imageSpacing[1] * SCALE);

        final Canvas resultCanvas = new Canvas(result);
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);

        @ColorInt
        final int bgFillColor = Prefs.bgFillColor(this);
        if (bgFillColor != Color.TRANSPARENT) {
            // Fill the canvas (blank image) with the user's selected background fill color
            resultCanvas.drawColor(bgFillColor);
        }

        final MaterialDialog progress = new MaterialDialog.Builder(this)
                .content(R.string.affixing_your_photos)
                .progress(true, -1)
                .cancelable(false)
                .show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
                final Rect dstRect = new Rect(0, 0, 10, 10);

                if (horizontal) {
                    int currentX = 0;
                    mTraverseIndex = -1;

                    BitmapFactory.Options bitmapOptions;
                    while ((bitmapOptions = getNextBitmapOptions()) != null) {
                        final int scaledWidth = (int) (bitmapOptions.outWidth * SCALE);
                        final int scaledHeight = (int) (bitmapOptions.outHeight * SCALE);
                        Util.log("CURRENT IMAGE width = %d, height = %d", bitmapOptions.outWidth, bitmapOptions.outHeight);
                        Util.log("SCALED IMAGE width = %d, height = %d", scaledWidth, scaledHeight);

                        // Left is right of last image plus horizontal spacing
                        dstRect.left = currentX + SPACING_HORIZONTAL;
                        // Right is left plus width of the current image
                        dstRect.right = dstRect.left + scaledWidth;

                        // Top is very top plus vertical spacing
                        dstRect.top = SPACING_VERTICAL;
                        // Bottom is top plus the current image height
                        dstRect.bottom = dstRect.top + scaledHeight;

                        Util.log("LEFT = %d, RIGHT = %d, TOP = %d, BOTTOM = %d",
                                dstRect.left, dstRect.right, dstRect.top, dstRect.bottom);

                        bitmapOptions.inJustDecodeBounds = false;
                        calculateInSampleSize(bitmapOptions, dstRect.bottom - dstRect.top);

                        Bitmap bm = getNextBitmap(bitmapOptions);
                        if (bm == null) {
                            break;
                        }
                        bm.setDensity(Bitmap.DENSITY_NONE);
                        resultCanvas.drawBitmap(bm, null, dstRect, paint);

                        currentX = dstRect.right;
                        bm.recycle();
                    }
                } else {
                    int currentY = 0;
                    mTraverseIndex = -1;

                    BitmapFactory.Options bitmapOptions;

                    while ((bitmapOptions = getNextBitmapOptions()) != null) {
                        final int scaledWidth = (int) (bitmapOptions.outWidth * SCALE);
                        final int scaledHeight = (int) (bitmapOptions.outHeight * SCALE);
                        Util.log("CURRENT IMAGE width = %d, height = %d", bitmapOptions.outWidth, bitmapOptions.outHeight);
                        Util.log("SCALED IMAGE width = %d, height = %d", scaledWidth, scaledHeight);

                        // Top is bottom of the last image plus vertical spacing
                        dstRect.top = currentY + SPACING_VERTICAL;
                        // Bottom is top plus height of the current image
                        dstRect.bottom = dstRect.top + scaledHeight;

                        // Left is left side plus horizontal spacing
                        dstRect.left = SPACING_HORIZONTAL;
                        // Right is left plus the current image width
                        dstRect.right = dstRect.left + scaledWidth;

                        Util.log("LEFT = %d, RIGHT = %d, TOP = %d, BOTTOM = %d",
                                dstRect.left, dstRect.right, dstRect.top, dstRect.bottom);

                        bitmapOptions.inJustDecodeBounds = false;
                        bitmapOptions.inSampleSize = (dstRect.right - dstRect.left) / bitmapOptions.outWidth;

                        Bitmap bm = getNextBitmap(bitmapOptions);
                        if (bm == null) {
                            break;
                        }
                        resultCanvas.drawBitmap(bm, null, dstRect, paint);

                        currentY = dstRect.bottom;
                        bm.recycle();
                    }
                }

                // Save results to file
                File cacheFile = Util.makeTempFile(MainActivity.this, ".png");
                Util.log("Saving result to %s", cacheFile.getAbsolutePath().replace("%", "%%"));
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(cacheFile);
                    result.compress(format, quality, os);
                } catch (Exception e) {
                    Util.log("Error: %s", e.getMessage());
                    e.printStackTrace();
                    Util.showError(MainActivity.this, e);
                    cacheFile = null;
                } finally {
                    Util.closeQuietely(os);
                }

                // Recycle the large final image
                result.recycle();
                // Close progress dialog and move on to the done phase
                progress.dismiss();
                done(cacheFile);
            }
        }).start();
    }

    private void done(File file) {
        Util.log("Done");
        // Clear selection
        clearSelection();
        // Unlock orientation so Activity can rotate again
        Util.unlockOrientation(MainActivity.this);
        // Add the affixed file to the media store so gallery apps can see it
        MediaScannerConnection.scanFile(this,
                new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Util.log("Scanned %s, uri = %s", path, uri != null ? uri.toString().replace("%", "%%") : null);
                    }
                });

        try {
            // Open the result in the viewer
            startActivity(new Intent(this, ViewerActivity.class)
                    .setDataAndType(Uri.fromFile(file), "image/*"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.getSelectedCount() > 0)
            clearSelection();
        else super.onBackPressed();
    }

    @Override
    public void onDragSelectionChanged(int count) {
        mAffixButton.setText(getString(R.string.affix_x, count));
        mAffixButton.setEnabled(count > 0);
        mToolbar.getMenu().findItem(R.id.clear).setVisible(mAdapter != null && mAdapter.getSelectedCount() > 0);
    }
}
