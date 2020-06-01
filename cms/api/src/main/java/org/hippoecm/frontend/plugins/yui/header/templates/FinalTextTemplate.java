/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.header.templates;

import java.util.Map;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.util.template.PackageTextTemplate;

public class FinalTextTemplate implements IHeaderContributor {

    private static final long serialVersionUID = 1L;

    private final String script;

    public FinalTextTemplate(PackageTextTemplate template, Map<String, Object> parameters) {
        this.script = template.interpolate(parameters).getString();
    }

    public FinalTextTemplate(Class<?> clazz, String filename, Map<String, Object> parameters) {
        this(new PackageTextTemplate(clazz, filename), parameters);
    }

    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript(script));
    }

}
