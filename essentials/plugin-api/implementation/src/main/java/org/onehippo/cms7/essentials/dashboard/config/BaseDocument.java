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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ResidualPropertiesCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 * @version "$Id$"
 */
@DocumentType("BaseDocument")
@Node(discriminator = true, jcrType = "essentials:document")
public class BaseDocument implements Document {

    @Field(path = true)
    private String path;

    @Field
    private String name;

    @Collection(elementClassName = String.class, collectionConverter = MultiValueCollectionConverterImpl.class)
    private List<String> properties = new LinkedList<>();


    public BaseDocument() {
    }

    public BaseDocument(final String name) {
        this.name = name;
    }

    public BaseDocument(final String name, final String path) {
        this.name = name;
        this.path = path;
    }


    @Override
    public List<String> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(final List<String> properties) {
        this.properties = properties;
    }

    @Override
    public void addProperty(final String value) {
        properties.add(value);
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }



    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BaseDocument");
        sb.append("{name='").append(name).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }
}