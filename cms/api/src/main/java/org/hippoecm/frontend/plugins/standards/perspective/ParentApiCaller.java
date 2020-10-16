/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * This class modifies the dom to append javascript when
 * any method of the {@link ParentApi} interface is called.
 */
public class ParentApiCaller implements ParentApi {

    static final String JAVA_SCRIPT_TEMPLATE = "Hippo.navapp.updateNavLocation(%s)";
    private Supplier<IPartialPageRequestHandler> targetSupplier;

    public ParentApiCaller() {
        setTargetSupplier(() -> RequestCycle.get().find(AjaxRequestTarget.class));
    }

    // For testing purposes
    ParentApi setTargetSupplier(final Supplier<IPartialPageRequestHandler> targetSupplier) {
        this.targetSupplier = targetSupplier;
        return this;
    }

    @Override
    public void updateNavLocation(final String path, final String breadcrumbLabel, final boolean addHistory) {
        final IPartialPageRequestHandler target = targetSupplier.get();

        if (target == null) {
            return;
        }

        String script = this.prepareJS(path, breadcrumbLabel, addHistory);

        target.appendJavaScript(script);
    }

    private String prepareJS(final String path, final String breadcrumbLabel, final boolean addHistory) {
        final JSONObject params = new JSONObject();
        params.put("path", path);

        if (breadcrumbLabel != null && breadcrumbLabel.length() > 0) {
            params.put("breadcrumbLabel", breadcrumbLabel);
        }

        if (addHistory) {
            params.put("addHistory", true);
        }

        return String.format(JAVA_SCRIPT_TEMPLATE, params);
    }

}
