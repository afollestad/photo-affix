package com.afollestad.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.adapters.PhotoGridAdapter;
import com.afollestad.photoaffix.adapters.SelectionCallback;
import com.afollestad.photoaffix.animation.HeightEvaluator;
import com.afollestad.photoaffix.animation.ViewHideAnimationListener;
import com.afollestad.photoaffix.data.Photo;
import com.afollestad.photoaffix.utils.Prefs;
import com.afollestad.photoaffix.utils.Util;
import com.afollestad.photoaffix.views.ColorCircleView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity implements SelectionCallback, ColorChooserDialog.ColorCallback {

    private static final int PERMISSION_RC = 69;

    @Bind(R.id.appbar_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.list)
    RecyclerView mList;
    @Bind(R.id.affixButton)
    Button mAffixButton;
    @Bind(R.id.settingsFrame)
    ViewGroup mSettingsFrame;

    @Bind(R.id.stackHorizontallySwitch)
    CheckBox mStackHorizontally;
    @Bind(R.id.bgFillColorCircle)
    ColorCircleView mBgFillColor;

    private PhotoGridAdapter mAdapter;
    private ArrayList<Bitmap> mImages;
    private Photo[] mSelectedPhotos;

    private int mOriginalSettingsFrameHeight = -1;
    private ValueAnimator mSettingsFrameAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.clear) {
                    clearSelection();
                    return true;
                }
                return false;
            }
        });

        mList.setLayoutManager(new GridLayoutManager(this,
                getResources().getInteger(R.integer.grid_width)));
        mAdapter = new PhotoGridAdapter(this);
        mAdapter.restoreInstanceState(savedInstanceState);
        mList.setAdapter(mAdapter);

        mStackHorizontally.setChecked(Prefs.stackHorizontally(this));
        mBgFillColor.setColor(Prefs.bgFillColor(this));
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
                .projection("_id", "_data", "datetaken")
                .sort("datetaken DESC")
                .all(new GetCallback<Photo>() {
                    @Override
                    public void result(Photo[] photos) {
                        mAdapter.setPhotos(photos);
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
        recycle(false);
    }

    public void recycle(final boolean reload) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recycle(reload);
                }
            });
            return;
        }
        if (mImages != null) {
            for (Bitmap bm : mImages)
                if (!bm.isRecycled()) bm.recycle();
        }
        if (reload) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            }, 1000);
        }
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
        mAdapter.clearSelected();
        mToolbar.getMenu().findItem(R.id.clear).setVisible(false);
    }

    @Override
    public void onSelectionChanged(int count) {
        mAffixButton.setText(getString(R.string.affix_x, count));
        mAffixButton.setEnabled(count > 0);
        mToolbar.getMenu().findItem(R.id.clear).setVisible(mAdapter != null && mAdapter.getSelectedCount() > 0);
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
        mSettingsFrameAnimator.setInterpolator(new DecelerateInterpolator());
        mSettingsFrameAnimator.setDuration(200);
        mSettingsFrameAnimator.start();
    }

    @OnClick(R.id.affixButton)
    public void onClickAffixButton(View v) {
        v.setEnabled(false);
        mSelectedPhotos = mAdapter.getSelectedPhotos();
        mImages = new ArrayList<>();
        try {
            loadImages();
            process();
        } catch (OutOfMemoryError e) {
            Util.showError(this, new Exception("You've run out of RAM for processing images; I'm working to improve memory usage! Sit tight while this app is in beta."));
            recycle(false);
        }
        v.setEnabled(true);
    }

    @OnClick({R.id.settingStackHorizontally, R.id.settingBgFillColor})
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
                        .preselect(Prefs.bgFillColor(this))
                        .show();
                break;
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, @ColorInt int selectedColor) {
        Prefs.bgFillColor(this, selectedColor);
        mBgFillColor.setColor(selectedColor);
    }

    private void loadImages() {
        // Loads raw Bitmaps for all selected photos
        for (Photo photo : mSelectedPhotos) {
            Bitmap bm = BitmapFactory.decodeFile(photo._data);
            mImages.add(bm);
        }
    }

    private void process() {
        // Lock orientation so the Activity won't change configuration during proessing
        Util.lockOrientation(this);

        final Bitmap result;
        final boolean horizontal = mStackHorizontally.isChecked();
        if (horizontal) {
            Util.log("Horizontally stacking");
            // The width of the resulting image will be the largest width of the selected images
            // The height of the resulting image will be the sum of all the selected images' heights
            int totalWidth = 0;
            int maxHeight = -1;
            for (Bitmap bm : mImages) {
                totalWidth += bm.getWidth();
                if (bm.getHeight() > maxHeight)
                    maxHeight = bm.getHeight();
            }
            Util.log("Total width = %d, max height = %d", maxHeight, totalWidth);
            result = Bitmap.createBitmap(totalWidth, maxHeight, Bitmap.Config.ARGB_8888);
        } else {
            Util.log("Vertically stacking");
            // The height of the resulting image will be the largest height of the selected images
            // The width of the resulting image will be the sum of all the selected images' widths
            int totalHeight = 0;
            int maxWidth = -1;
            for (Bitmap bm : mImages) {
                totalHeight += bm.getHeight();
                if (bm.getWidth() > maxWidth)
                    maxWidth = bm.getWidth();
            }
            Util.log("Max width = %d, total height = %d", maxWidth, totalHeight);
            result = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888);
        }

        final Canvas resultCanvas = new Canvas(result);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        resultCanvas.drawColor(Prefs.bgFillColor(this));

        final MaterialDialog progress = new MaterialDialog.Builder(this)
                .content(R.string.affixing_your_photos)
                .progress(true, -1)
                .cancelable(false)
                .show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Perform affixation
                if (horizontal) {
                    // Keep track of X position of the left of the next image to be drawn
                    int currentX = 0;
                    for (Bitmap bm : mImages) {
                        Util.log("CURRENT IMAGE width = %d, height = %d", bm.getWidth(), bm.getHeight());
                        Util.log("LEFT is at %d...", currentX);
                        // Padding is the offset used to vertically center smaller images
                        int padding = result.getHeight() - bm.getHeight();
                        if (padding > 0) padding /= 2;
                        Util.log("PADDING = %d", padding);
                        // Draw image vertically centered to the right of the last
                        resultCanvas.drawBitmap(bm, currentX, padding, paint);
                        // Right of this image is left of the next
                        currentX += bm.getWidth();
                    }
                } else {
                    // Keep track of Y position of the top of the next image to be drawn
                    int currentY = 0;
                    for (Bitmap bm : mImages) {
                        Util.log("CURRENT IMAGE width = %d, height = %d", bm.getWidth(), bm.getHeight());
                        Util.log("TOP is at %d...", currentY);
                        // Padding is the offset used to horizontally center smaller images
                        int padding = result.getWidth() - bm.getWidth();
                        if (padding > 0) padding /= 2;
                        Util.log("PADDING = %d", padding);
                        // Draw image horizontally centered below the last
                        resultCanvas.drawBitmap(bm, padding, currentY, paint);
                        // Bottom of this image is top of the next
                        currentY += bm.getHeight();
                    }
                }

                // Save results to file
                File cacheFile = Util.makeTempFile(MainActivity.this, ".jpg");
                Util.log("Saving result to %s", cacheFile.getAbsolutePath().replace("%", "%%"));
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(cacheFile);
                    result.compress(Bitmap.CompressFormat.JPEG, 100, os);
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
        // Recycle all the individual loaded images
        recycle(true);
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
        // Open the result in the viewer
        startActivity(new Intent(this, ViewerActivity.class)
                .setDataAndType(Uri.fromFile(file), "image/*"));
    }
}
