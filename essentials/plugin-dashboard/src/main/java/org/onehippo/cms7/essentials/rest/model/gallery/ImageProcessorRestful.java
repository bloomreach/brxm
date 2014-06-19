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

package org.onehippo.cms7.essentials.rest.model.gallery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.rest.model.PropertyRestful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "imageProcessors")
public class ImageProcessorRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private String name;
    private String path;
    private String id;
    private String className;
    private List<PropertyRestful> properties = new ArrayList<>();
    private List<ImageVariantRestful> variants = new ArrayList<>();

    public ImageProcessorRestful() {
    }

    public ImageProcessorRestful(final String name, final String path) {

        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public List<PropertyRestful> getProperties() {
        return properties;
    }

    public void setProperties(final List<PropertyRestful> properties) {
        this.properties = properties;
    }

    public List<ImageVariantRestful> getVariants() {
        return variants;
    }

    public void setVariants(final List<ImageVariantRestful> variants) {
        this.variants = variants;
    }

    public void addVariants(final Collection<ImageVariantRestful> variants) {
        this.variants.addAll(variants);
    }

    public void addVariant(final ImageVariantRestful variant) {
        this.variants.add(variant);
    }

    public ImageVariantRestful getVariantForNodeType(final String nodeType) {
        for (final ImageVariantRestful variant : this.variants) {
            if (StringUtils.equals(HippoNodeUtils.getTypeFromPrefixAndName(variant.getNamespace(), variant.getName()), nodeType)) {
                return variant;
            }
        }
        return null;
    }

    public ImageVariantRestful getVariant(final String id) {
        for (final ImageVariantRestful variant : this.variants) {
            if (StringUtils.equals(variant.getId(), id)) {
                return variant;
            }
        }
        return null;
    }

    public ImageVariantRestful getVariant(final String namaspace, final String name) {
        for (final ImageVariantRestful variant : this.variants) {
            if (StringUtils.equals(variant.getNamespace(), namaspace) && StringUtils.equals(variant.getName(), name)) {
                return variant;
            }
        }
        return null;
    }

    public void addProperty(final PropertyRestful property) {
        this.properties.add(property);
    }
}