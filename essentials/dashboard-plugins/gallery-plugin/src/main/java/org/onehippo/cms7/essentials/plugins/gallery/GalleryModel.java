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

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

/**
 * @version "$Id$"
 */
public class GalleryModel implements Restful {

    private static final long serialVersionUID = 1L;

    private String path;
    private String name;
    private boolean readOnly;
    private List<ImageModel> models;

    public GalleryModel() {
    }

    public GalleryModel(final String name) {
        this.name = name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void addModel(final ImageModel model) {
        if (models == null) {
            models = new ArrayList<>();
        }
        models.add(model);
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public List<ImageModel> getModels() {
        return models;
    }

    public void setModels(final List<ImageModel> models) {
        this.models = models;
    }
}
