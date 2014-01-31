/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.essentials.rest.model.PropertyRestful;
import org.onehippo.cms7.essentials.rest.model.Restful;
import org.onehippo.cms7.essentials.rest.model.TranslationRestful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "variant")
public class ImageVariantRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private String id;
    private String namespace;
    private String name;
    private int width = 0;
    private int height = 0;
    private List<PropertyRestful> properties = new ArrayList<>();
    private List<TranslationRestful> translations = new ArrayList<>();
    private List<ImageSetRestful> imageSets = new ArrayList<>();

    public ImageVariantRestful() {
    }

    public ImageVariantRestful(final String namespace, final String name) {

        this.namespace = namespace;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
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

    public List<PropertyRestful> getProperties() {
        return properties;
    }

    public void setProperties(final List<PropertyRestful> properties) {
        this.properties = properties;
    }

    public List<TranslationRestful> getTranslations() {
        return translations;
    }

    public void setTranslations(final List<TranslationRestful> translations) {
        this.translations = translations;
    }

    public List<ImageSetRestful> getImageSets() {
        return imageSets;
    }

    public void setImageSets(final List<ImageSetRestful> imageSets) {
        this.imageSets = imageSets;
    }

    public void addTranslation(final TranslationRestful translation) {
        this.translations.add(translation);
    }

    public void addTranslations(final Collection<TranslationRestful> translations) {
        this.translations.addAll(translations);
    }

    public void addProperty(final PropertyRestful property) {
        this.properties.add(property);
    }

    public String getNodeType() {
        return getNamespace() + ':' + getName();
    }

    public PropertyRestful getProperty(final String propertyName) {
        for(PropertyRestful property : this.properties) {
            if(StringUtils.equals(property.getName(), propertyName)) {
                return property;
            }
        }
        return null;
    }

}
