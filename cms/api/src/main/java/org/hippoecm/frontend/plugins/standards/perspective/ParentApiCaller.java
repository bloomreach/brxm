/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standards.perspective;

import java.util.function.Supplier;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class modifies the dom to append javascript when
 * any method of the {@link ParentApi} interface is called.
 */
public class ParentApiCaller implements ParentApi {

    private static final Logger log = LoggerFactory.getLogger(ParentApiCaller.class);
    private static final String UPDATE_NAV_LOCATION = "updateNavLocation";
    private static final String NAVIGATE = "navigate";
    private Supplier<IPartialPageRequestHandler> targetSupplier;
    private final ObjectMapper mapper;


    public ParentApiCaller() {
        mapper = Json.getMapper();
        setTargetSupplier(() -> RequestCycle.get().find(AjaxRequestTarget.class));
    }

    // For testing purposes
    ParentApi setTargetSupplier(final Supplier<IPartialPageRequestHandler> targetSupplier) {
        this.targetSupplier = targetSupplier;
        return this;
    }

    @Override
    public void updateNavLocation(final NavLocation location) {
        apply(UPDATE_NAV_LOCATION, location);
    }

    @Override
    public void navigate(final NavLocation location) {
        apply(NAVIGATE, location);
    }


    private void apply(String method, NavLocation location) {
        final IPartialPageRequestHandler target = targetSupplier.get();
        if (target != null) {
            try {
                target.appendJavaScript(getJavaScript(method, location));
            } catch (JsonProcessingException e) {
                log.warn("Could not marshall {navLocation:{}}", location);
            }
        }
    }

    private String getJavaScript(String method, NavLocation location) throws JsonProcessingException {
        return String.format("Hippo.AppToNavApp && Hippo.AppToNavApp.%s(%s)", method, mapper.writeValueAsString(location));
    }
}
