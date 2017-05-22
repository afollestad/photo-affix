package com.afollestad.photoaffix.data;

import java.io.Serializable;

/** @author Aidan Follestad (afollestad) */
public class PhotoHolder implements Serializable {

  public Photo[] photos;

  public PhotoHolder() {}

  public PhotoHolder(Photo[] photos) {
    this.photos = photos;
  }
}
