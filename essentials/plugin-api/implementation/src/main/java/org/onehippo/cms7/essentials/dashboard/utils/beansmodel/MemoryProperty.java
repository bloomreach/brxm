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

package org.onehippo.cms7.essentials.dashboard.utils.beansmodel;

/**
 * @version "$Id: MemoryProperty.java 172944 2013-08-06 16:37:37Z mmilicevic $"
 */
public class MemoryProperty {


    private String name;
    private String type;
    private boolean multiple;
    private final MemoryBean parent;

    public MemoryProperty(final MemoryBean parent) {
        this.parent = parent;
    }

    public MemoryBean getParent() {
        return parent;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(final boolean multiple) {
        this.multiple = multiple;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }


    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MemoryProperty{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", multiple=").append(multiple);
        sb.append(", parent=").append(parent);
        sb.append('}');
        return sb.toString();
    }
}
