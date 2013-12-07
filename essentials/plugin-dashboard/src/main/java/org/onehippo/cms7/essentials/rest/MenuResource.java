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

package org.onehippo.cms7.essentials.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.inject.jaxrs.Injected;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@Path("/menu/")
public class MenuResource{

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(MenuResource.class);


    @GET
    @Path("/")
    public PluginRestful getMenu() {
        return new PluginRestful();
    }
}
