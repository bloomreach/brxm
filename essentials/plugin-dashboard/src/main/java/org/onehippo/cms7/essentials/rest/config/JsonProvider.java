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

package org.onehippo.cms7.essentials.rest.config;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * @version "$Id$"
 */
@Provider
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class JsonProvider extends JSONProvider {

    private static Logger log = LoggerFactory.getLogger(JsonProvider.class);
    private static final ImmutableList<String> items = new ImmutableList.Builder<String>()
            .add("items")
            .add("item")
            .add("properties")
            .add("variants")
            .add("imageSets")
            .add("translations")
            .build();
    public JsonProvider() {
        setIgnoreNamespaces(true);
        setDropRootElement(true);
        setArrayKeys(items);
        setSerializeAsArray(true);


    }
}
