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

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.JavascriptPackageResource;

public class TemplateComposerApiResourceBehavior implements IHeaderContributor {

    private static final long serialVersionUID = 1L;

    // All individual javascript files used in the template composer API. The array order determines the include order,
    // which matters. All files listed below should also be present in the aggregation section in frontend-api/pom.xml.
    private static final String[] JAVASCRIPT_FILES = {
            "PropertiesEditor.js",
            "PlainPropertiesEditor.js",
            "VariantAdder.js",
            "PlainVariantAdder.js"
    };
    private static final String ALL_JAVASCRIPT = "template-composer-api-all.js";

    @Override
    public void renderHead(final IHeaderResponse response) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            for (String jsFile : JAVASCRIPT_FILES) {
                IHeaderContributor contributor = JavascriptPackageResource.getHeaderContribution(TemplateComposerApiResourceBehavior.class, jsFile);
                contributor.renderHead(response);
            }
        } else {
            IHeaderContributor contributor =JavascriptPackageResource.getHeaderContribution(TemplateComposerApiResourceBehavior.class, ALL_JAVASCRIPT);
            contributor.renderHead(response);
        }
    }
}
