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

package org.onehippo.cms7.essentials.dashboard.rest;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

import com.wordnik.swagger.annotations.ApiModel;

/**
 * @version "$Id$"
 */
@ApiModel
@XmlRootElement(name = "keyvalue")
public class KeyValueRestful implements Restful, Comparable<KeyValueRestful> {

    private static final long serialVersionUID = 1L;

    private String key;
    private String value;

    public KeyValueRestful() {
    }

    public KeyValueRestful(final String key, final String value) {

        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KeyValueRestful{");
        sb.append("key='").append(key).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public int compareTo(KeyValueRestful other) {
        int keyCompared = getKey().compareTo(other.getKey());
        return keyCompared != 0 ? keyCompared : getValue().compareTo(other.getValue());
    }
}
