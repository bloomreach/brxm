/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer;

import java.util.Arrays;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.extjs.ExtWidgetRegistryHeaderItem;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class TemplateComposerApiHeaderItem extends HeaderItem {

    // All individual javascript files used in the template composer API. The array order determines the include order,
    // which matters. All files listed below should also be present in the aggregation section in frontend-api/pom.xml.
    private static final String[] JAVASCRIPT_FILES = {
            "PropertiesEditor.js",
            "PlainPropertiesEditor.js",
            "VariantAdder.js",
            "PlainVariantAdder.js"
    };
    private static final JavaScriptResourceReference[] JAVASCRIPT_REFERENCES;

    private static final JavaScriptResourceReference ALL_JAVASCRIPT =
            new JavaScriptResourceReference(TemplateComposerApiHeaderItem.class, "template-composer-api-all.js");

    static {
        JAVASCRIPT_REFERENCES = new JavaScriptResourceReference[JAVASCRIPT_FILES.length];
        for (int i = 0; i < JAVASCRIPT_FILES.length; i++) {
            JAVASCRIPT_REFERENCES[i] = new JavaScriptResourceReference(TemplateComposerApiHeaderItem.class, JAVASCRIPT_FILES[i]);
        }
    }

    private static final TemplateComposerApiHeaderItem INSTANCE = new TemplateComposerApiHeaderItem();

    public static TemplateComposerApiHeaderItem get() {
        return INSTANCE;
    }

    private TemplateComposerApiHeaderItem() {}

    @Override
    public Iterable<? extends HeaderItem> getDependencies() {
        return Arrays.asList(ExtResourcesHeaderItem.get(), CmsHeaderItem.get(), ExtWidgetRegistryHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Arrays.asList("template-composer-api-header-item");
    }

    @Override
    public void render(final Response response) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            for (JavaScriptResourceReference resourceReference : JAVASCRIPT_REFERENCES) {
                JavaScriptHeaderItem.forReference(resourceReference).render(response);
            }
        } else {
            JavaScriptHeaderItem.forReference(ALL_JAVASCRIPT).render(response);
        }
    }

}
