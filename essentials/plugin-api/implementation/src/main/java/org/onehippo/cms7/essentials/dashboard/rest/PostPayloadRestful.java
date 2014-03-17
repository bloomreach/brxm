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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.util.LinkedHashMap;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

import com.wordnik.swagger.annotations.ApiModel;

/**
 * Holds ordered map of key value properties.
 *
 * @version "$Id$"
 */
@ApiModel
@XmlRootElement(name = "payload")
public class PostPayloadRestful implements Restful {

    private static final long serialVersionUID = 1L;


    public PostPayloadRestful() {
    }
    public PostPayloadRestful(final String key, final String value) {

        add(key, value);
    }



    private LinkedHashMap<String, String> values = new LinkedHashMap<>();

    public void add(final String key, final String value) {
        values.put(key, value);
    }

    // keep concrete class:
    @SuppressWarnings("CollectionDeclaredAsConcreteClass")
    public LinkedHashMap<String, String> getValues() {
        if (values == null) {
            return new LinkedHashMap<>();
        }
        return values;
    }

    public void setValues(final LinkedHashMap<String, String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PostPayloadRestful{");
        sb.append("values=").append(values);
        sb.append('}');
        return sb.toString();
    }
}
