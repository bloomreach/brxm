/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.onehippo.cms7.essentials.sdk.api.rest.PluginDescriptor;

/**
 * In old versions of Essentials, all plugin descriptors were put together into a single JSON file, which listed the
 * individual plugin descriptors as 'items'. For backwards compatibility reasons, we keep supporting this arrangement
 * through this class. New mechanisms should preferably just use a plain List&lt;PluginDescriptor&gt;.
 */
public class PluginDescriptorList implements Serializable {

    private List<PluginDescriptor> items = new ArrayList<>();

    public void add(PluginDescriptor plugin) {
        items.add(plugin);
    }

    public Iterator<PluginDescriptor> iterator() {
        return items.iterator();
    }

    public List<PluginDescriptor> getItems() {
        return items;
    }
}
