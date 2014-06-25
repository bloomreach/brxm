/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugins.gallery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @version "$Id$"
 */
public class ImageModel implements Serializable {


    private static final long serialVersionUID = 1L;
    private final String prefix;
    private String path;
    private String parentPath;
    private int width;
    private int height;
    private String name;
    private String originalName = null;
    private boolean readOnly;
    private List<TranslationModel> translations = new ArrayList<>();

    public ImageModel(final String prefix) {
        this.prefix = prefix;
        if (prefix.equals("hippogallery")) {
            readOnly = true;
        }
    }

    public ImageModel(final String prefix, final String name, final int width, final int height) {
        this(prefix);
        this.width = width;
        this.height = height;
        this.name = name;
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

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getParentPath() {
        return parentPath;
    }

    /**
     * Set the name of the image and set the original name of the image,
     * when the original name was not set before.
     *
     * @param name the name of the image
     */
    public void setName(final String name) {
        if (getOriginalName() == null) {
            setOriginalName(name);
        }
        this.name = name;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setParentPath(final String parentPath) {
        this.parentPath = parentPath;
    }

    /**
     * When the image is retrieved from the repository, the name under which the
     * image is stored is also stored in the model. The original name is used to
     * determine the name as which the node is originally stored (e.g. this can
     * be used for updating nodes.
     *
     * @param originalName the name under which the image is stored, when first retrieved
     */
    private void setOriginalName(final String originalName) {
        this.originalName = originalName;
    }

    /**
     * When the image is retrieved from the repository, the original name should be
     * stored by calling ${@link #setOriginalName(String)}. When the name is stored,
     * this method will return the original name as stored in the repository. When
     * no stored name is this method will return. Usually this means that the image
     * was not stored in repository before. When null is returned it at least means
     * that it's not possible to determine the old image node (e.g. when renaming
     * of deleting the image).
     *
     * @return the stored name (i.e. original node name)
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * Determine, based on the stored name (see {@link #getOriginalName()}), whether
     * the name has changed.
     *
     * @return true when stored name is not null and not equals current name, false
     * otherwise
     */
    public boolean isNameChanged() {
        if (getOriginalName() == null) {
            return false;
        } else {
            return !getOriginalName().equals(getName());
        }
    }

    /**
     * Get the type of the image based on the prefix and name.
     *
     * @return type of the image (i.e. 'prefix:name')
     */
    public String getType() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getPrefix());
        sb.append(':');
        sb.append(getName());
        return sb.toString();
    }

    /**
     * Get the type of the image based on the prefix and the original name.
     *
     * @return type of the original image (i.e. 'prefix:originalName')
     */
    public String getOriginalType() {
        if (getOriginalName() == null) {
            return getType();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(getPrefix());
        sb.append(':');
        sb.append(getOriginalName());
        return sb.toString();
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void addTranslation(final TranslationModel model) {
        translations.add(model);
    }

    public void addTranslation(final String language, final String message, final String property) {
        translations.add(new TranslationModel(StringUtils.trimToEmpty(language), StringUtils.trimToEmpty(message), StringUtils.trimToEmpty(property)));
    }

    public void removeTranslation(final TranslationModel model) {
        translations.remove(model);
    }

    public List<TranslationModel> getTranslations() {
        return translations;
    }

    public void setTranslations(final List<TranslationModel> translations) {
        if (translations == null) {
            this.translations = new ArrayList<>();
        } else {
            this.translations = translations;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ImageModel{");
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", originalName='").append(originalName).append('\'');
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", readOnly=").append(readOnly);
        sb.append(", translations=").append(translations.size());
        sb.append('}');
        return sb.toString();
    }

}
