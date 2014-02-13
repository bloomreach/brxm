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
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.GenericEntity;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.utils.code.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * @version "$Id$"
 */

public abstract class RestfulList<T extends Restful> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(RestfulList.class);

    private static final long serialVersionUID = 1L;
    protected List<T> items = Lists.newArrayList();

    public void add(T resource) {
        items.add(resource);
    }

    public void addAll(Collection<T> items) {
        items.addAll(items);
    }

    public Iterator<T> iterator() {
        return items.iterator();
    }

    public abstract List<T> getItems();

}
