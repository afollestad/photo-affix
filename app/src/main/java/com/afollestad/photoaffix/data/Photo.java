package com.afollestad.photoaffix.data;

import com.afollestad.inquiry.annotations.Column;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Photo implements Serializable {

    public Photo() {
    }

    @Column
    public long _id;
    @Column
    public String _data;
    @Column
    public long datetaken;
}
