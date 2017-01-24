package com.afollestad.photoaffix.ui;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.photoaffix.R;
import com.afollestad.photoaffix.utils.Util;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ViewerActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener, PhotoViewAttacher.OnPhotoTapListener {

    @BindView(R.id.appbar_toolbar) Toolbar toolbar;
    @BindView(R.id.image) ImageView image;
    @BindView(R.id.progress) ProgressBar progress;

    private Unbinder unbinder;
    private PhotoViewAttacher attacher;
    private ViewPropertyAnimator toolbarAnimator;

    private final SimpleTarget<GlideDrawable> mTarget = new SimpleTarget<GlideDrawable>() {
        @Override
        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
            image.setImageDrawable(resource);
            attacher.update();
            progress.setVisibility(View.GONE);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        unbinder = ButterKnife.bind(this);

        toolbar.inflateMenu(R.menu.menu_viewer);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.setNavigationOnClickListener(v -> finish());

        attacher = new PhotoViewAttacher(image);
        attacher.setOnPhotoTapListener(this);
        attacher.setOnViewTapListener((view, x, y) -> onPhotoTap(null, 0f, 0f));

        Glide.with(this)
                .load(new File(getIntent().getData().getPath()))
                .into(mTarget);

        if (toolbar != null) {
            new Handler().postDelayed(() -> {
                if (toolbar != null)
                    onPhotoTap(null, 0f, 0f);
            }, 2000);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        unbinder = null;
    }

    @Override public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share: {
                Intent target = new Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_STREAM, getIntent().getData())
                        .setDataAndType(getIntent().getData(), "image/*");
                Intent chooser = Intent.createChooser(target, getString(R.string.share_using));
                startActivity(chooser);
                break;
            }
            case R.id.edit: {
                Intent target = new Intent(Intent.ACTION_EDIT)
                        .setDataAndType(getIntent().getData(), "image/*");
                Intent chooser = Intent.createChooser(target, getString(R.string.edit_with));
                startActivity(chooser);
                break;
            }
            case R.id.openExternal: {
                Intent target = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(getIntent().getData(), "image/*");
                Intent chooser = Intent.createChooser(target, getString(R.string.open_with));
                startActivity(chooser);
                break;
            }
            case R.id.delete: {
                new MaterialDialog.Builder(this)
                        .content(R.string.confirm_delete)
                        .positiveText(R.string.yes)
                        .negativeText(android.R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                delete();
                            }
                        }).show();
                break;
            }
        }
        return false;
    }

    private void delete() {
        final MaterialDialog progress = new MaterialDialog.Builder(this)
                .content(R.string.deleting)
                .cancelable(false)
                .progress(true, -1)
                .show();
        File file = new File(getIntent().getData().getPath());
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        MediaScannerConnection.scanFile(ViewerActivity.this,
                new String[]{file.getAbsolutePath()}, null,
                (path, uri) -> {
                    MainActivity.dismissDialog(progress);
                    Util.log("Scanned %s, uri = %s", path, uri != null ? uri.toString().replace("%", "%%") : null);
                    finish();
                });
    }

    @Override public void onPhotoTap(View view, float x, float y) {
        if (toolbar == null) return;
        else if (toolbarAnimator != null) toolbarAnimator.cancel();
        toolbarAnimator = toolbar.animate();
        if (toolbar.getAlpha() > 0f) {
            toolbarAnimator.alpha(0f);
        } else {
            toolbarAnimator.alpha(1f);
        }
        toolbarAnimator.setDuration(200);
        toolbarAnimator.start();
    }

    @Override public void onOutsidePhotoTap() {
    }
}
