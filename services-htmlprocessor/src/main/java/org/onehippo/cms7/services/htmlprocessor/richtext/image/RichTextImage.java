/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.services.htmlprocessor.richtext.image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;

import static org.onehippo.cms7.services.htmlprocessor.util.JcrUtil.PATH_SEPARATOR;

/**
 * This class represents an image resource that a rich text field has a reference to.
 * It handles url generation and maintains the facetname attribute.
 */
public class RichTextImage implements Serializable {

    public static final String DOCUMENT_PATH_PLACEHOLDER = "{_document}";

    private String path;
    private String name;
    private String uuid;
    private List<String> resourceDefinitions;
    private String selectedResourceDefinition;
    private URLEncoder urlEncoder;

    public RichTextImage(final String targetPath, final String name, final URLEncoder urlEncoder) {
        this.path = targetPath;
        this.name = name;
        this.urlEncoder = urlEncoder != null ? urlEncoder : URLEncoder.OPAQUE;
        this.resourceDefinitions = new ArrayList<>();
    }

    public List<String> getResourceDefinitions() {
        return resourceDefinitions;
    }

    public void setResourceDefinitions(final List<String> resourceDefinitions) {
        this.resourceDefinitions = resourceDefinitions;
        if (resourceDefinitions.size() == 0) {
            this.selectedResourceDefinition = null;
        } else if (!resourceDefinitions.contains(selectedResourceDefinition)) {
            selectedResourceDefinition = resourceDefinitions.get(0);
        }
    }

    public String getSelectedResourceDefinition() {
        return selectedResourceDefinition;
    }

    public void setSelectedResourceDefinition(final String selectedResourceDefinition) {
        this.selectedResourceDefinition = selectedResourceDefinition;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFacetSelectPath() {
        if (selectedResourceDefinition != null) {
            return name + PATH_SEPARATOR + DOCUMENT_PATH_PLACEHOLDER + PATH_SEPARATOR + selectedResourceDefinition;
        } else {
            return name;
        }
    }

    public String getUrl() {
        final String docUrl = "binaries" + path;
        final String url = selectedResourceDefinition != null ?
                docUrl + PATH_SEPARATOR + selectedResourceDefinition : docUrl;
        return encodeUrl(url);
    }

    private String encodeUrl(final String url) {
        return urlEncoder.encode(url);
    }

    public boolean isValid() {
        return path != null && !(getResourceDefinitions().size() > 1 && selectedResourceDefinition == null);
    }

    public String getPath() {
        return path;
    }

}
