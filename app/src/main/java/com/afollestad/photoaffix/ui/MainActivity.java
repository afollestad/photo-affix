package com.afollestad.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
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
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
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
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.afollestad.photoaffix.utils.Util.log;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity
    implements ColorChooserDialog.ColorCallback,
    ImageSpacingDialog.SpacingCallback,
    ImageSizingDialog.SizingCallback,
    DragSelectRecyclerViewAdapter.SelectionListener {

  private static final int PERMISSION_RC = 69;
  private static final int BROWSE_RC = 21;

  @BindView(R.id.list)
  public DragSelectRecyclerView list;

  @BindView(R.id.appbar_toolbar)
  Toolbar toolbar;

  @BindView(R.id.affixButton)
  Button affixButton;

  @BindView(R.id.settingsFrame)
  ViewGroup settingsFrame;

  @BindView(R.id.empty)
  TextView empty;

  @BindView(R.id.stackHorizontallyLabel)
  TextView stackHorizontallyLabel;

  @BindView(R.id.stackHorizontallySwitch)
  CheckBox stackHorizontallyCheck;

  @BindView(R.id.bgFillColorCircle)
  ColorCircleView bgFillColorCircle;

  @BindView(R.id.bgFillColorLabel)
  TextView bgFillColorLabel;

  @BindView(R.id.imagePaddingLabel)
  TextView imagePaddingLabel;

  @BindView(R.id.removeBgButton)
  Button removeBgFillBtn;

  @BindView(R.id.scalePriorityLabel)
  TextView scalePriorityLabel;

  @BindView(R.id.scalePrioritySwitch)
  CheckBox scalePrioritySwitch;

  private PhotoGridAdapter adapter;
  private Photo[] selectedPhotos;
  private int traverseIndex;
  private boolean autoSelectFirst;

  private int originalSettingsFrameHeight = -1;
  private ValueAnimator settingsFrameAnimator;

  private Unbinder unbinder;

  // Avoids a rare crash
  public static void dismissDialog(@Nullable Dialog dialog) {
    if (dialog == null) return;
    try {
      dialog.dismiss();
    } catch (Throwable ignored) {
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    unbinder = ButterKnife.bind(this);
    Inquiry.newInstance(this, null).build();

    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == R.id.clear) {
            clearSelection();
            return true;
          } else if (item.getItemId() == R.id.about) {
            AboutDialog.show(MainActivity.this);
            return true;
          }
          return false;
        });

    list.setLayoutManager(
        new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width)));

    adapter = new PhotoGridAdapter(this);
    adapter.restoreInstanceState(savedInstanceState);
    adapter.setSelectionListener(this);
    list.setAdapter(adapter);

    DefaultItemAnimator animator = new DefaultItemAnimator();
    animator.setSupportsChangeAnimations(false);
    list.setItemAnimator(animator);

    final boolean stackHorizontally = Prefs.stackHorizontally(this);
    stackHorizontallyCheck.setChecked(stackHorizontally);
    stackHorizontallyLabel.setText(
        stackHorizontally ? R.string.stack_horizontally : R.string.stack_vertically);

    final boolean scalePriority = Prefs.scalePriority(this);
    scalePrioritySwitch.setChecked(scalePriority);
    scalePriorityLabel.setText(
        scalePriority ? R.string.scale_priority_on : R.string.scale_priority_off);

    final int bgFillColor = Prefs.bgFillColor(this);
    bgFillColorCircle.setColor(bgFillColor);
    final int[] padding = Prefs.imageSpacing(this);
    imagePaddingLabel.setText(getString(R.string.image_spacing_x, padding[0], padding[1]));

    if (bgFillColor != Color.TRANSPARENT) {
      removeBgFillBtn.setVisibility(View.VISIBLE);
      bgFillColorLabel.setText(R.string.background_fill_color);
    } else {
      bgFillColorLabel.setText(R.string.background_fill_color_transparent);
    }

    processIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    processIntent(intent);
  }

  private void processIntent(Intent intent) {
    if (intent != null && Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
      ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
      if (uris != null && uris.size() > 1) {
        selectedPhotos = new Photo[uris.size()];
        for (int i = 0; i < uris.size(); i++) selectedPhotos[i] = new Photo(uris.get(i));
        beginProcessing();
      } else {
        Toast.makeText(this, R.string.need_two_or_more, Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbinder.unbind();
    unbinder = null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    super.onSaveInstanceState(outState, outPersistentState);
    if (adapter != null) adapter.saveInstanceState(outState);
  }

  private void refresh() {
    int permission =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
    if (permission != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_RC);
      return;
    }

    //noinspection VisibleForTests
    Inquiry.get(this)
        .selectFrom(Uri.parse("content://media/external/images/media"), Photo.class)
        .sort("datetaken DESC")
        .where("_data IS NOT NULL")
        .all(
            photos -> {
              if (isFinishing()) {
                return;
              }
              if (empty != null) {
                adapter.setPhotos(photos);
                empty.setVisibility(
                    photos == null || photos.length == 0 ? View.VISIBLE : View.GONE);
                if (photos != null && photos.length > 0 && autoSelectFirst) {
                  adapter.shiftSelections();
                  adapter.setSelected(1, true);
                  autoSelectFirst = false;
                }
              }
            });
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_RC) {
      refresh();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) Inquiry.destroy(this);
  }

  public void clearSelection() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      runOnUiThread(this::clearSelection);
      return;
    }
    selectedPhotos = null;
    adapter.clearSelected();
    toolbar.getMenu().findItem(R.id.clear).setVisible(false);
  }

  @OnClick(R.id.removeBgButton)
  public void onClickRemoveBgFill() {
    removeBgFillBtn.setVisibility(View.GONE);
    //noinspection ConstantConditions
    onColorSelection(null, Color.TRANSPARENT);
  }

  @OnClick(R.id.expandButton)
  public void onClickExpandButton(ImageView button) {
    if (originalSettingsFrameHeight == -1) {
      final int settingControlHeight =
          (int) getResources().getDimension(R.dimen.settings_control_height);
      originalSettingsFrameHeight = settingControlHeight * settingsFrame.getChildCount();
    }
    if (settingsFrameAnimator != null) settingsFrameAnimator.cancel();
    if (settingsFrame.getVisibility() == View.GONE) {
      settingsFrame.setVisibility(View.VISIBLE);
      button.setImageResource(R.drawable.ic_collapse);
      settingsFrameAnimator =
          ValueAnimator.ofObject(
              new HeightEvaluator(settingsFrame), 0, originalSettingsFrameHeight);

    } else {
      button.setImageResource(R.drawable.ic_expand);
      settingsFrameAnimator =
          ValueAnimator.ofObject(
              new HeightEvaluator(settingsFrame), originalSettingsFrameHeight, 0);
      settingsFrameAnimator.addListener(new ViewHideAnimationListener(settingsFrame));
    }
    settingsFrameAnimator.setInterpolator(new FastOutSlowInInterpolator());
    settingsFrameAnimator.setDuration(200);
    settingsFrameAnimator.start();
  }

  private void beginProcessing() {
    affixButton.setEnabled(false);
    try {
      startProcessing();
    } catch (OutOfMemoryError e) {
      Util.showMemoryError(MainActivity.this);
    }
    affixButton.setEnabled(true);
  }

  @OnClick(R.id.affixButton)
  public void onClickAffixButton(View v) {
    selectedPhotos = adapter.getSelectedPhotos();
    beginProcessing();
  }

  @OnClick({
      R.id.settingStackHorizontally,
      R.id.settingBgFillColor,
      R.id.settingImagePadding,
      R.id.settingScalePriority
  })
  public void onClickSetting(View view) {
    switch (view.getId()) {
      case R.id.settingStackHorizontally:
        stackHorizontallyCheck.setChecked(!stackHorizontallyCheck.isChecked());
        stackHorizontallyLabel.setText(
            stackHorizontallyCheck.isChecked()
                ? R.string.stack_horizontally
                : R.string.stack_vertically);
        Prefs.stackHorizontally(this, stackHorizontallyCheck.isChecked());
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
      case R.id.settingScalePriority:
        scalePrioritySwitch.setChecked(!scalePrioritySwitch.isChecked());
        scalePriorityLabel.setText(
            scalePrioritySwitch.isChecked()
                ? R.string.scale_priority_on
                : R.string.scale_priority_off);
        Prefs.scalePriority(this, scalePrioritySwitch.isChecked());
        break;
    }
  }

  @Override
  public void onColorSelection(
      @NonNull ColorChooserDialog colorChooserDialog, @ColorInt int selectedColor) {
    if (selectedColor != Color.TRANSPARENT) {
      removeBgFillBtn.setVisibility(View.VISIBLE);
      bgFillColorLabel.setText(R.string.background_fill_color);
    } else {
      bgFillColorLabel.setText(R.string.background_fill_color_transparent);
    }
    Prefs.bgFillColor(this, selectedColor);
    bgFillColorCircle.setColor(selectedColor);
  }

  @Override
  public void onColorChooserDismissed(@NonNull ColorChooserDialog colorChooserDialog) {
  }

  @Override
  public void onSpacingChanged(int horizontal, int vertical) {
    Prefs.imageSpacing(this, horizontal, vertical);
    imagePaddingLabel.setText(getString(R.string.image_spacing_x, horizontal, vertical));
  }

  @Size(2)
  private int[] getNextBitmapSize() {
    if (selectedPhotos == null || selectedPhotos.length == 0) {
      selectedPhotos = adapter.getSelectedPhotos();
      if (selectedPhotos == null || selectedPhotos.length == 0)
        return new int[]{10, 10}; // crash workaround
    }
    traverseIndex++;
    if (traverseIndex > selectedPhotos.length - 1) return null;
    Photo nextPhoto = selectedPhotos[traverseIndex];
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    InputStream is = null;
    try {
      is = Util.openStream(this, nextPhoto.getUri());
      BitmapFactory.decodeStream(is, null, options);
    } catch (Exception e) {
      Util.showError(this, e);
      return new int[]{0, 0};
    } finally {
      Util.closeQuietely(is);
    }
    return new int[]{options.outWidth, options.outHeight};
  }

  @Nullable
  private BitmapFactory.Options getNextBitmapOptions() {
    if (selectedPhotos == null) return null;
    traverseIndex++;
    if (traverseIndex > selectedPhotos.length - 1) return null;
    Photo nextPhoto = selectedPhotos[traverseIndex];
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
      return null;
    } catch (OutOfMemoryError e2) {
      Util.showMemoryError(MainActivity.this);
      return null;
    } finally {
      Util.closeQuietely(is);
    }
    return options;
  }

  private Bitmap getNextBitmap(BitmapFactory.Options options) {
    Photo nextPhoto = selectedPhotos[traverseIndex];
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

  private void startProcessing() {
    // Lock orientation so the Activity won't change configuration during processing
    Util.lockOrientation(this);

    final int[] imageSpacing = Prefs.imageSpacing(MainActivity.this);
    final int SPACING_HORIZONTAL = imageSpacing[0];
    final int SPACING_VERTICAL = imageSpacing[1];

    final boolean horizontal = stackHorizontallyCheck.isChecked();
    int resultWidth;
    int resultHeight;

    log("--------------------------------");
    if (horizontal) {
      log("Horizontally stacking");

      // The width of the resulting image will be the largest width of the selected images
      // The height of the resulting image will be the sum of all the selected images' heights
      int maxHeight = -1;
      int minHeight = -1;

      // Traverse all selected images to find largest and smallest heights
      traverseIndex = -1;
      int[] size;
      while ((size = getNextBitmapSize()) != null) {
        if (size[0] == 0 && size[1] == 0) return;
        log("Image %d size: %d/%d", traverseIndex, size[0], size[1]);
        if (maxHeight == -1) maxHeight = size[1];
        else if (size[1] > maxHeight) maxHeight = size[1];
        if (minHeight == -1) minHeight = size[1];
        else if (size[1] < minHeight) minHeight = size[1];
      }
      log("Min height: %d, max height: %d", minHeight, maxHeight);

      // Traverse images again now that we know the min/max height, scale widths accordingly
      traverseIndex = -1;
      int totalWidth = 0;
      boolean scalePriority = Prefs.scalePriority(this);
      while ((size = getNextBitmapSize()) != null) {
        if (size[0] == 0 && size[1] == 0) return;
        int w = size[0];
        int h = size[1];
        float ratio = (float) w / (float) h;
        if (scalePriority) {
          // Scale to largest
          if (h < maxHeight) {
            h = maxHeight;
            w = (int) ((float) h * ratio);
            log(
                "Height of image %d is less than max (%d), scaled up to %d/%d...",
                traverseIndex, maxHeight, w, h);
          }
        } else {
          // Scale to smallest
          if (h > minHeight) {
            h = minHeight;
            w = (int) ((float) h * ratio);
            log(
                "Height of image %d is larger than min (%d), scaled down to %d/%d...",
                traverseIndex, minHeight, w, h);
          }
        }
        totalWidth += w;
      }

      // Compensate for spacing
      totalWidth += SPACING_HORIZONTAL * (selectedPhotos.length + 1);
      minHeight += SPACING_VERTICAL * 2;
      maxHeight += SPACING_VERTICAL * 2;

      // Crash avoidance
      if (totalWidth == 0) {
        Util.showError(
            this,
            new Exception(
                "The total generated width is 0. Please "
                    + "notify me of this through the Google+ community."));
        return;
      } else if (maxHeight == 0) {
        Util.showError(
            this,
            new Exception(
                "The max found height is 0. Please notify "
                    + "me of this through the Google+ community."));
        return;
      }

      // Print data and create large Bitmap
      log("Total width with spacing = %d, max height with spacing = %d", totalWidth, maxHeight);
      resultWidth = totalWidth;
      resultHeight = scalePriority ? maxHeight : minHeight;
    } else {
      log("Vertically stacking");

      // The height of the resulting image will be the largest height of the selected images
      // The width of the resulting image will be the sum of all the selected images' widths
      int maxWidth = -1;
      int minWidth = -1;

      // Traverse all selected images and load min/max width, scale height accordingly
      traverseIndex = -1;
      int[] size;
      while ((size = getNextBitmapSize()) != null) {
        if (size[0] == 0 && size[1] == 0) return;
        log("Image %d size: %d/%d", traverseIndex, size[0], size[1]);
        if (maxWidth == -1) maxWidth = size[0];
        else if (size[0] > maxWidth) maxWidth = size[0];
        if (minWidth == -1) minWidth = size[0];
        else if (size[0] < minWidth) minWidth = size[0];
      }

      // Traverse images again now that we know the min/max height, scale widths accordingly
      traverseIndex = -1;
      int totalHeight = 0;
      boolean scalePriority = Prefs.scalePriority(this);
      while ((size = getNextBitmapSize()) != null) {
        if (size[0] == 0 && size[1] == 0) return;
        int w = size[0];
        int h = size[1];
        float ratio = (float) h / (float) w;
        if (scalePriority) {
          // Scale to largest
          if (w < maxWidth) {
            w = maxWidth;
            h = (int) ((float) w * ratio);
            log(
                "Height of image %d is larger than min (%d), scaled down to %d/%d...",
                traverseIndex, maxWidth, w, h);
          }
        } else {
          // Scale to smallest
          if (w > minWidth) {
            w = minWidth;
            h = (int) ((float) w * ratio);
            log(
                "Width of image %d is larger than min (%d), scaled height down to %d/%d...",
                traverseIndex, minWidth, w, h);
          }
        }
        totalHeight += h;
      }

      // Compensate for spacing
      totalHeight += SPACING_VERTICAL * (selectedPhotos.length + 1);
      minWidth += SPACING_HORIZONTAL * 2;
      maxWidth += SPACING_HORIZONTAL * 2;

      // Crash avoidance
      if (totalHeight == 0) {
        Util.showError(
            this,
            new Exception(
                "The total generated height is 0. Please "
                    + "notify me of this through the Google+ community."));
        return;
      } else if (maxWidth == 0) {
        Util.showError(
            this,
            new Exception(
                "The max found width is 0. Please notify "
                    + "me of this through the Google+ community."));
        return;
      }

      // Print data and create large Bitmap
      log("Max width with spacing = %d, total height with spacing = %d", maxWidth, totalHeight);
      resultWidth = scalePriority ? maxWidth : minWidth;
      resultHeight = totalHeight;
    }

    ImageSizingDialog.show(this, resultWidth, resultHeight);
  }

  @Override
  public void onSizingResult(
      double scale,
      int resultWidth,
      int resultHeight,
      Bitmap.CompressFormat format,
      int quality,
      boolean cancelled) {
    if (cancelled) {
      traverseIndex = -1;
      Util.unlockOrientation(this);
      return;
    }
    try {
      finishProcessing(scale, resultWidth, resultHeight, format, quality);
    } catch (OutOfMemoryError e) {
      Util.showMemoryError(this);
    }
  }

  private void finishProcessing(
      final double SCALE,
      final int resultWidth,
      final int resultHeight,
      final Bitmap.CompressFormat format,
      final int quality) {
    // Crash avoidance
    if (resultWidth == 0) {
      Util.showError(
          this,
          new Exception(
              "The result width is 0. Please notify "
                  + "me of this through the Google+ community."));
      return;
    } else if (resultHeight == 0) {
      Util.showError(
          this,
          new Exception(
              "The result height is 0. Please notify "
                  + "me of this through the Google+ community."));
      return;
    }

    log(
        "IMAGE SCALE = %s, total scaled width = %d, total scaled height = %d",
        SCALE, resultWidth, resultHeight);
    final Bitmap result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);

    final boolean horizontal = stackHorizontallyCheck.isChecked();
    final int[] imageSpacing = Prefs.imageSpacing(MainActivity.this);
    final int SPACING_HORIZONTAL =
        (int) (imageSpacing[0] * SCALE); // TODO should scale be multiplied here?
    final int SPACING_VERTICAL = (int) (imageSpacing[1] * SCALE);

    final Canvas resultCanvas = new Canvas(result);
    final Paint paint = new Paint();
    paint.setFilterBitmap(true);
    paint.setAntiAlias(true);
    paint.setDither(true);

    @ColorInt final int bgFillColor = Prefs.bgFillColor(this);
    if (bgFillColor != Color.TRANSPARENT) {
      // Fill the canvas (blank image) with the user's selected background fill color
      resultCanvas.drawColor(bgFillColor);
    }

    final MaterialDialog progress =
        new MaterialDialog.Builder(this)
            .content(R.string.affixing_your_photos)
            .progress(true, -1)
            .cancelable(false)
            .show();

    new Thread(
        () -> {
          // Used to set destination dimensions when drawn onto the canvas, e.g. when padding is used
          final Rect dstRect = new Rect(0, 0, 10, 10);
          int processedCount = 0;
          final boolean scalingPriority = Prefs.scalePriority(MainActivity.this);

          if (horizontal) {
            int currentX = 0;
            traverseIndex = -1;
            BitmapFactory.Options bitmapOptions;
            while ((bitmapOptions = getNextBitmapOptions()) != null) {
              log("--------------------------------");
              processedCount++;

              final int width = bitmapOptions.outWidth;
              final int height = bitmapOptions.outHeight;
              final float ratio = (float) width / (float) height;

              int scaledWidth = (int) (width * SCALE);
              int scaledHeight = (int) (height * SCALE);

              if (scalingPriority) {
                // Scale up to largest height, fill total height
                if (scaledHeight < resultHeight) {
                  scaledHeight = resultHeight;
                  scaledWidth = (int) ((float) scaledHeight * ratio);
                }
              } else {
                // Scale down to smallest height, fill total height
                if (scaledHeight > resultHeight) {
                  scaledHeight = resultHeight;
                  scaledWidth = (int) ((float) scaledHeight * ratio);
                }
              }

              log(
                  "CURRENT IMAGE width = %d, height = %d, w/h = %s",
                  width, height, (float) width / (float) height);
              log(
                  "SCALED IMAGE width = %d, height = %d, w/h = %s",
                  scaledWidth, scaledHeight, (float) scaledWidth / (float) scaledHeight);

              // Left is right of last image plus horizontal spacing
              dstRect.left = currentX + SPACING_HORIZONTAL;
              // Right is left plus width of the current image
              dstRect.right = dstRect.left + scaledWidth;
              dstRect.top = SPACING_VERTICAL;
              dstRect.bottom = scaledHeight + SPACING_VERTICAL;

              log(
                  "LEFT = %d, RIGHT = %d, TOP = %d, BOTTOM = %d, TOTAL SCALED HEIGHT = %d, TOTAL SCALED WIDTH = %d",
                  dstRect.left,
                  dstRect.right,
                  dstRect.top,
                  dstRect.bottom,
                  resultHeight,
                  resultWidth);

              bitmapOptions.inJustDecodeBounds = false;
              bitmapOptions.inSampleSize =
                  (dstRect.bottom - dstRect.top) / bitmapOptions.outHeight;

              final Bitmap bm = getNextBitmap(bitmapOptions);
              if (bm == null) break;
              try {
                bm.setDensity(Bitmap.DENSITY_NONE);
                resultCanvas.drawBitmap(bm, null, dstRect, paint);
              } catch (RuntimeException e) {
                Util.showMemoryError(MainActivity.this);
              } finally {
                bm.recycle();
              }

              currentX = dstRect.right;
            }
          } else {
            int currentY = 0;
            traverseIndex = -1;
            BitmapFactory.Options bitmapOptions;
            while ((bitmapOptions = getNextBitmapOptions()) != null) {
              log("--------------------------------");
              processedCount++;

              final int width = bitmapOptions.outWidth;
              final int height = bitmapOptions.outHeight;
              final float ratio = (float) height / (float) width;

              int scaledWidth = (int) (width * SCALE);
              int scaledHeight = (int) (height * SCALE);

              if (scalingPriority) {
                // Scale up to largest width, fill total width
                if (scaledWidth < resultWidth) {
                  scaledWidth = resultWidth;
                  scaledHeight = (int) ((float) scaledWidth * ratio);
                }
              } else {
                // Scale down to smallest width, fill total width
                if (scaledWidth > resultWidth) {
                  scaledWidth = resultWidth;
                  scaledHeight = (int) ((float) scaledWidth * ratio);
                }
              }

              log(
                  "CURRENT IMAGE width = %d, height = %d, w/h = %s",
                  width, height, (float) width / (float) height);
              log(
                  "SCALED IMAGE width = %d, height = %d, w/h = %s",
                  scaledWidth, scaledHeight, (float) width / (float) height);

              // Top is bottom of the last image plus vertical spacing
              dstRect.top = currentY + SPACING_VERTICAL;
              // Bottom is top plus height of the current image
              dstRect.bottom = dstRect.top + scaledHeight;
              dstRect.left = SPACING_HORIZONTAL;
              dstRect.right = scaledWidth + SPACING_HORIZONTAL;

              log(
                  "LEFT = %d, RIGHT = %d, TOP = %d, BOTTOM = %d, TOTAL SCALED WIDTH: %d, TOTAL SCALED HEIGHT: %d",
                  dstRect.left,
                  dstRect.right,
                  dstRect.top,
                  dstRect.bottom,
                  resultWidth,
                  resultHeight);

              bitmapOptions.inJustDecodeBounds = false;
              bitmapOptions.inSampleSize =
                  (dstRect.right - dstRect.left) / bitmapOptions.outWidth;

              final Bitmap bm = getNextBitmap(bitmapOptions);
              if (bm == null) break;
              try {
                bm.setDensity(Bitmap.DENSITY_NONE);
                resultCanvas.drawBitmap(bm, null, dstRect, paint);
              } catch (RuntimeException e) {
                Util.showMemoryError(MainActivity.this);
              } finally {
                bm.recycle();
              }

              currentY = dstRect.bottom;
            }
          }

          if (processedCount == 0) {
            try {
              result.recycle();
            } catch (Throwable ignored) {
            }
            dismissDialog(progress);
            return;
          }

          log("--------------------------------");

          // Save results to file
          File cacheFile = Util.makeTempFile(MainActivity.this, ".png");
          log("Saving result to %s", cacheFile.getAbsolutePath().replace("%", "%%"));
          FileOutputStream os = null;
          try {
            os = new FileOutputStream(cacheFile);
            result.compress(format, quality, os);
          } catch (Exception e) {
            log("Error: %s", e.getMessage());
            e.printStackTrace();
            Util.showError(MainActivity.this, e);
            cacheFile = null;
          } finally {
            Util.closeQuietely(os);
          }

          // Recycle the large final image
          result.recycle();
          // Close progress dialog and move on to the done phase
          dismissDialog(progress);
          done(cacheFile);
        })
        .start();
  }

  private void done(File file) {
    log("Done");
    // Clear selection
    clearSelection();
    // Unlock orientation so Activity can rotate again
    Util.unlockOrientation(MainActivity.this);
    // Add the affixed file to the media store so gallery apps can see it
    MediaScannerConnection.scanFile(
        this,
        new String[]{file.toString()},
        null,
        (path, uri) ->
            log(
                "Scanned %s, uri = %s",
                path, uri != null ? uri.toString().replace("%", "%%") : null));

    try {
      // Open the result in the viewer
      startActivity(
          new Intent(this, ViewerActivity.class).setDataAndType(Uri.fromFile(file), "image/*"));
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void onBackPressed() {
    if (adapter.getSelectedCount() > 0) {
      clearSelection();
    } else super.onBackPressed();
  }

  @Override
  public void onDragSelectionChanged(int count) {
    affixButton.setText(getString(R.string.affix_x, count));
    affixButton.setEnabled(count > 0);
    toolbar
        .getMenu()
        .findItem(R.id.clear)
        .setVisible(adapter != null && adapter.getSelectedCount() > 0);
  }

  public void browseExternalPhotos() {
    Intent intent =
        new Intent(Intent.ACTION_GET_CONTENT)
            .setType("image/*");
    startActivityForResult(intent, BROWSE_RC);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == BROWSE_RC && resultCode == RESULT_OK) {
      final MaterialDialog progress =
          new MaterialDialog.Builder(this)
              .content(R.string.retrieving_photo)
              .progress(true, -1)
              .cancelable(false)
              .show();
      new Thread(
          () -> {
            InputStream input = null;
            FileOutputStream output = null;
            final File targetFile = Util.makeTempFile(MainActivity.this, ".png");
            try {
              input = Util.openStream(this, data.getData());
              output = new FileOutputStream(targetFile);
              Util.copyStream(input, output);
              output.close();
              MediaScannerConnection.scanFile(
                  this,
                  new String[]{targetFile.toString()},
                  null,
                  (path, uri) -> {
                    log(
                        "Scanned %s, uri = %s",
                        path, uri != null ? uri.toString().replace("%", "%%") : null);
                    autoSelectFirst = true;
                    refresh();
                  });
            } catch (Exception e) {
              Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
              if (progress != null) {
                progress.dismiss();
              }
              Util.closeQuietely(input);
              Util.closeQuietely(output);
            }
          })
          .start();
    }
  }
}
