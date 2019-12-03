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

package org.hippoecm.frontend.wicketevents;

import java.io.IOException;
import java.util.Collections;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxCallFailureHeaderItem extends HeaderItem {

    private static final Logger log = LoggerFactory.getLogger(AjaxCallFailureHeaderItem.class);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("ajax-call-failure-header-item");
    }

    @Override
    public void render(final Response response) {
        OnDomReadyHeaderItem.forScript(createAjaxCallFailureScript()).render(response);
    }

    private static String createAjaxCallFailureScript() {
        final String fileName = "ajax-call-failure-listener.js";
        try (PackageTextTemplate javaScript = new PackageTextTemplate(AjaxCallFailureHeaderItem.class, fileName)) {
            return javaScript.asString();
        } catch (IOException e) {
            log.error("Could not load script '{}'", fileName, e);
            return "";
        }
    }
}
