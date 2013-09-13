/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.gallery.model;

import java.io.Serializable;

/**
 * @version "$Id: ImageModel.java 164011 2013-05-11 14:05:01Z mmilicevic $"
 */
public class ImageModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private int width;
    private int height;
    private String name;
    private boolean readOnly;

    public ImageModel() {
    }

    public ImageModel(final int width, final int height, final String name) {

        this.width = width;
        this.height = height;
        this.name = name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ImageModel{");
        sb.append("width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", name='").append(name).append('\'');
        sb.append(", readOnly=").append(readOnly);
        sb.append('}');
        return sb.toString();
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }
}
