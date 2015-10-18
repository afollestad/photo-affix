package com.afollestad.photoaffix.data;

import android.net.Uri;

import com.afollestad.inquiry.annotations.Column;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Photo implements Serializable {

    public Photo() {
    }

    public Photo(Uri uri) {
        _data = uri.toString();
    }

    @Column
    public long _id;
    @Column
    public String _data;
    @Column
    public long datetaken;

    public Uri getUri() {
        Uri uri = Uri.parse(_data);
        if (uri.getScheme() == null)
            uri = Uri.parse(String.format("file://%s", uri.getPath()));
        return uri;
    }
}
