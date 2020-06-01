/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.behaviors;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behavior to open the root folder.
 * <p>
 * This class adds header item containing javascript ( {@link #OPEN_ROOT_FOLDER_JS} ).
 * </p>
 * <p>
 * That script adds two functions:
 * <ul>
 *     <li><code>Hippo.openRootFolder()</code></li>
 * </ul>
 *
 * </p>
 */
public abstract class OpenRootFolderBehavior extends AbstractDefaultAjaxBehavior {

    private static final Logger log = LoggerFactory.getLogger(OpenRootFolderBehavior.class);

    private static final String OPEN_ROOT_FOLDER_JS = "open-root-folder.js";

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(OnLoadHeaderItem.forScript(createScript()));
    }

    private String createScript() {
        final Map<String, String> scriptParams = new TreeMap<>();
        scriptParams.put("callbackUrl", this.getCallbackUrl().toString());
        String resource = null;
        try (final PackageTextTemplate openRootFolderJs = new PackageTextTemplate(OpenRootFolderBehavior.class, OPEN_ROOT_FOLDER_JS)) {
            resource = openRootFolderJs.asString(scriptParams);
        } catch (IOException e) {
            log.warn("Resource {} could not be closed.", OPEN_ROOT_FOLDER_JS, e);
        }
        return resource;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        onOpenRootFolder(target);
    }

    protected abstract void onOpenRootFolder(final AjaxRequestTarget target);
}
