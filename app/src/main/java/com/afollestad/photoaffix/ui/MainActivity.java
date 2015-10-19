package com.afollestad.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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

import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.photoaffix.BuildConfig;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.adapters.PhotoGridAdapter;
import com.afollestad.photoaffix.adapters.SelectionCallback;
import com.afollestad.photoaffix.animation.HeightEvaluator;
import com.afollestad.photoaffix.animation.ViewHideAnimationListener;
import com.afollestad.photoaffix.data.Photo;
import com.afollestad.photoaffix.dialogs.ImagePaddingDialog;
import com.afollestad.photoaffix.utils.Prefs;
import com.afollestad.photoaffix.utils.Util;
import com.afollestad.photoaffix.views.ColorCircleView;
import com.afollestad.photoaffix.views.DragSelectRecyclerView;
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
        SelectionCallback, ColorChooserDialog.ColorCallback, ImagePaddingDialog.PaddingCallback, DragSelectRecyclerView.DragListener {

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
    @Bind(R.id.imagePaddingLabel)
    TextView mImagePaddingLabel;

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
                }
                return false;
            }
        });

        mList.setLayoutManager(new GridLayoutManager(this,
                getResources().getInteger(R.integer.grid_width)));
        mList.setDragSelectListener(this);

        mAdapter = new PhotoGridAdapter(this);
        mAdapter.restoreInstanceState(savedInstanceState);
        mList.setAdapter(mAdapter);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        mList.setItemAnimator(animator);

        mStackHorizontally.setChecked(Prefs.stackHorizontally(this));
        mBgFillColor.setColor(Prefs.bgFillColor(this));
        final int[] padding = Prefs.imagePadding(this);
        mImagePaddingLabel.setText(getString(R.string.image_padding_x,
                padding[0], padding[1], padding[2], padding[3]));

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
                .projection("_id", "_data", "datetaken")
                .sort("datetaken DESC")
                .all(new GetCallback<Photo>() {
                    @Override
                    public void result(Photo[] photos) {
                        mAdapter.setPhotos(photos);
                        mEmpty.setVisibility(photos == null || photos.length == 0 ?
                                View.VISIBLE : View.GONE);
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
        mSettingsFrameAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mSettingsFrameAnimator.setDuration(200);
        mSettingsFrameAnimator.start();
    }

    @OnClick(R.id.affixButton)
    public void onClickAffixButton(View v) {
        v.setEnabled(false);
        mSelectedPhotos = mAdapter.getSelectedPhotos();
        try {
            process();
        } catch (OutOfMemoryError e) {
            Util.showError(this, new Exception("You've run out of RAM for processing images; I'm working to improve memory usage! Sit tight while this app is in beta."));
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
                        .preselect(Prefs.bgFillColor(this))
                        .show();
                break;
            case R.id.settingImagePadding:
                new ImagePaddingDialog().show(getFragmentManager(), "[IMAGE_PADDING_DIALOG]");
                break;
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, @ColorInt int selectedColor) {
        Prefs.bgFillColor(this, selectedColor);
        mBgFillColor.setColor(selectedColor);
    }

    @Override
    public void onPaddingChanged(int left, int top, int right, int bottom) {
        Prefs.imagePadding(this, left, top, right, bottom);
        mImagePaddingLabel.setText(getString(R.string.image_padding_x, left, top, right, bottom));
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

    private Bitmap getNextBitmap() {
        mTraverseIndex++;
        if (mTraverseIndex > mSelectedPhotos.length - 1)
            return null;
        Photo nextPhoto = mSelectedPhotos[mTraverseIndex];
        InputStream is = null;
        Bitmap bm = null;
        try {
            is = Util.openStream(this, nextPhoto.getUri());
            bm = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Util.showError(this, e);
        } finally {
            Util.closeQuietely(is);
        }
        return bm;
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
            // Traverse all selected images and load in their sizes
            mTraverseIndex = -1;
            int[] size;
            while ((size = getNextBitmapSize()) != null) {
                totalWidth += size[0];
                if (size[1] > maxHeight)
                    maxHeight = size[1];
            }
            Util.log("Total width = %d, max height = %d", totalWidth, maxHeight);
            result = Bitmap.createBitmap(totalWidth, maxHeight, Bitmap.Config.ARGB_8888);
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
                if (size[0] > maxWidth)
                    maxWidth = size[0];
            }
            Util.log("Max width = %d, total height = %d", maxWidth, totalHeight);
            result = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888);
        }

        final Canvas resultCanvas = new Canvas(result);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Fill the canvas (blank image) with the user's selected background fill color
        resultCanvas.drawColor(Prefs.bgFillColor(this));

        final MaterialDialog progress = new MaterialDialog.Builder(this)
                .content(R.string.affixing_your_photos)
                .progress(true, -1)
                .cancelable(false)
                .show();

        int[] imagePadding = Prefs.imagePadding(MainActivity.this);
        final int PADDING_LEFT = imagePadding[0];
        final int PADDING_TOP = imagePadding[1];
        final int PADDING_RIGHT = imagePadding[2];
        final int PADDING_BOTTOM = imagePadding[3];

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
                final Rect dstRect = new Rect(0, 0, 10, 10);

                if (horizontal) {
                    // Keep track of X position of the left of the next image to be drawn
                    int currentX = 0;
                    // Traverse all selected images and load in their raw image data
                    mTraverseIndex = -1;
                    Bitmap bm;
                    while ((bm = getNextBitmap()) != null) {
                        Util.log("CURRENT IMAGE width = %d, height = %d", bm.getWidth(), bm.getHeight());
                        // Padding is the offset used to vertically center smaller images
                        int padding = result.getHeight() - bm.getHeight();
                        if (padding > 0) padding /= 2;
                        Util.log("PADDING = %d", padding);

                        // Draw image vertically centered to the right of the last
                        final float dstRatio = (float) bm.getWidth() / (float) bm.getHeight();
                        int dstHeight = bm.getHeight() - (padding * 2);

                        // Apply vertical padding
                        dstHeight -= PADDING_TOP;
                        dstHeight -= PADDING_BOTTOM;
                        int dstWidth = (int) (dstHeight * dstRatio);

                        // Apply horizontal padding
                        if (mTraverseIndex == 0) {
                            // First, normal padding on left
                            dstWidth -= PADDING_LEFT;
                            dstWidth -= (PADDING_RIGHT / 2);

                            dstRect.left = currentX + PADDING_LEFT;
                            dstRect.right = (currentX + dstWidth) - (PADDING_RIGHT / 2);
                        } else if (mTraverseIndex == mSelectedPhotos.length - 1) {
                            // Last, normal padding on right
                            dstWidth -= (PADDING_LEFT / 2);
                            dstWidth -= PADDING_RIGHT;

                            dstRect.left = currentX + (PADDING_LEFT / 2);
                            dstRect.right = (currentX + dstWidth) - PADDING_RIGHT;
                        } else {
                            // Middle, half padding on left and right
                            dstWidth -= (PADDING_LEFT / 2);
                            dstWidth -= (PADDING_RIGHT / 2);

                            dstRect.left = currentX + (PADDING_LEFT / 2);
                            dstRect.right = (currentX + dstWidth) - (PADDING_RIGHT / 2);
                        }

                        dstRect.top = padding + PADDING_TOP;
                        dstRect.bottom = bm.getHeight() - (padding + PADDING_BOTTOM);

                        Util.log("LEFT = %d, RIGHT = %d, TOP = %d, BOTTOM = %d",
                                dstRect.left, dstRect.right, dstRect.top, dstRect.bottom);
                        resultCanvas.drawBitmap(bm, null, dstRect, paint);

                        // Right of this image is left of the next
                        currentX += bm.getWidth();
                        // Recycle so it doesn't take up any memory
                        bm.recycle();
                    }
                } else {
                    // Keep track of Y position of the top of the next image to be drawn
                    int currentY = 0;
                    // Traverse all selected images and load in their raw image data
                    mTraverseIndex = -1;
                    Bitmap bm;
                    while ((bm = getNextBitmap()) != null) {
                        Util.log("CURRENT IMAGE width = %d, height = %d", bm.getWidth(), bm.getHeight());
                        Util.log("TOP is at %d...", currentY);
                        // Padding is the offset used to horizontally center smaller images
                        int padding = result.getWidth() - bm.getWidth();
                        if (padding > 0) padding /= 2;
                        Util.log("PADDING = %d", padding);

                        // Draw image horizontally centered below the last
                        final float dstRatio = (float) bm.getWidth() / (float) bm.getHeight();
                        int dstWidth = bm.getWidth() - (padding * 2);

                        // Apply horizontal padding
                        dstWidth -= PADDING_LEFT;
                        dstWidth -= PADDING_RIGHT;
                        int dstHeight = (int) (dstWidth / dstRatio);

                        // Apply vertical padding
                        if (mTraverseIndex == 0) {
                            // First, normal padding on top
                            dstHeight -= PADDING_TOP;
                            dstHeight -= (PADDING_BOTTOM / 2);

                            dstRect.top = currentY + PADDING_TOP;
                            dstRect.bottom = currentY + dstHeight + (PADDING_BOTTOM / 2);
                        } else if (mTraverseIndex == mSelectedPhotos.length - 1) {
                            // Last, normal padding on bottom
                            dstHeight -= (PADDING_TOP / 2);
                            dstHeight -= PADDING_BOTTOM;

                            dstRect.top = currentY + (PADDING_TOP / 2);
                            dstRect.bottom = currentY + dstHeight + PADDING_BOTTOM;
                        } else {
                            // Middle, half padding on top and bottom
                            dstHeight -= (PADDING_TOP / 2);
                            dstHeight -= (PADDING_BOTTOM / 2);

                            dstRect.top = currentY + (PADDING_TOP / 2);
                            dstRect.bottom = currentY + dstHeight + (PADDING_BOTTOM / 2);
                        }

                        dstRect.left = padding + PADDING_LEFT;
                        dstRect.right = bm.getWidth() - (padding + PADDING_RIGHT);

                        Util.log("LEFT = %d, RIGHT = %d, TOP = %d, BOTTOM = %d",
                                dstRect.left, dstRect.right, dstRect.top, dstRect.bottom);
                        resultCanvas.drawBitmap(bm, null, dstRect, paint);

                        // Bottom of this image is top of the next
                        currentY += bm.getHeight();
                        // Recycle so it doesn't take up any memory
                        bm.recycle();
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

    @Override
    public void onDragSelection(int initial, int index, int minReached, int maxReached) {
        if (mAdapter != null)
            mAdapter.selectRange(initial, index, minReached, maxReached);
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.getSelectedCount() > 0)
            clearSelection();
        else super.onBackPressed();
    }
}
