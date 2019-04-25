/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.navigation;

import java.io.IOException;
import java.util.Collections;

import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.HippoHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs and injects a javascript implementation of the nav-app to app API.
 */
public class NavAppToAppHeaderItem extends HippoHeaderItem {

    private static final String NAVIGATION_API_JS = "nav-app-to-app.js";

    private static final Logger log = LoggerFactory.getLogger(NavAppToAppHeaderItem.class);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("navigation-header-item");
    }

    @Override
    public void render(final Response response) {
        OnDomReadyHeaderItem.forScript(createScript()).render(response);
    }

    private String createScript() {

        try (final PackageTextTemplate javaScript = new PackageTextTemplate(NavAppToAppHeaderItem.class, NAVIGATION_API_JS)) {

            return javaScript.asString(Collections.emptyMap());

        } catch (IOException e) {
            log.error("Failed to create script for resource {}, returning empty string instead.", NAVIGATION_API_JS, e);
            return "";
        }
    }
}
