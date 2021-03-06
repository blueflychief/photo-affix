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

import butterknife.Bind;
import butterknife.ButterKnife;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ViewerActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener, PhotoViewAttacher.OnPhotoTapListener {

    @Bind(R.id.appbar_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.image)
    ImageView mImage;
    @Bind(R.id.progress)
    ProgressBar mProgress;

    private PhotoViewAttacher mAttacher;
    private ViewPropertyAnimator mToolbarAnimator;

    private final SimpleTarget<GlideDrawable> mTarget = new SimpleTarget<GlideDrawable>() {
        @Override
        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
            mImage.setImageDrawable(resource);
            mAttacher.update();
            mProgress.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        ButterKnife.bind(this);

        mToolbar.inflateMenu(R.menu.menu_viewer);
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.setNavigationIcon(R.drawable.ic_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mAttacher = new PhotoViewAttacher(mImage);
        mAttacher.setOnPhotoTapListener(this);
        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                onPhotoTap(null, 0f, 0f);
            }
        });

        Glide.with(this)
                .load(new File(getIntent().getData().getPath()))
                .into(mTarget);

        if (mToolbar != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mToolbar != null)
                        onPhotoTap(null, 0f, 0f);
                }
            }, 2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
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
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        MainActivity.dismissDialog(progress);
                        Util.log("Scanned %s, uri = %s", path, uri != null ? uri.toString().replace("%", "%%") : null);
                        finish();
                    }
                });
    }

    @Override
    public void onPhotoTap(View view, float x, float y) {
        if (mToolbar == null) return;
        else if (mToolbarAnimator != null) mToolbarAnimator.cancel();
        mToolbarAnimator = mToolbar.animate();
        if (mToolbar.getAlpha() > 0f) {
            mToolbarAnimator.alpha(0f);
        } else {
            mToolbarAnimator.alpha(1f);
        }
        mToolbarAnimator.setDuration(200);
        mToolbarAnimator.start();
    }
}
