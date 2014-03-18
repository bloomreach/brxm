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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * @version "$Id$"
 */


@ApiModel
@XmlRootElement(name = "collection")
public class RestfulList<T extends Restful> implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<T> items = Lists.newArrayList();

    public void add(T resource) {
        items.add(resource);
    }

    public void addAll(Collection<T> items) {
        items.addAll(items);
    }

    public Iterator<T> iterator() {
        return items.iterator();
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = KeyValueRestful.class, name = "keyvalue"),
            @JsonSubTypes.Type(value = PluginRestful.class, name = "plugin"),
            @JsonSubTypes.Type(value = ProjectRestful.class, name = "project"),
            @JsonSubTypes.Type(value = PropertyRestful.class, name = "property"),
            @JsonSubTypes.Type(value = NodeRestful.class, name = "node"),
            @JsonSubTypes.Type(value = PostPayloadRestful.class, name = "payload"),
            @JsonSubTypes.Type(value = PluginModuleRestful.class, name = "module"),
            @JsonSubTypes.Type(value = ErrorMessageRestful.class, name = "error"),
            @JsonSubTypes.Type(value = QueryRestful.class, name = "query"),
            @JsonSubTypes.Type(value = MessageRestful.class, name = "message")})
    public List<T> getItems() {
        return items;
    }

}
