/**
  * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
  *
  * Licensed under the Apache License, Version 2.0 (the  "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
*/
package org.onehippo.cms7.channelmanager.hstconfig;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;

public class HstConfigEditorResourceBehaviour extends AbstractYuiBehavior {

    public static final String HST_EXTWIREFRAME_JS = "ExtWireframe.js";
    public static final String HST_CONFIG_EDITOR_JS = "HstConfigEditor.js";
    private static final long serialVersionUID = 1L;

    @Override
    public void bind(final Component component) {
        super.bind(component);
        component.add(JavascriptPackageResource.getHeaderContribution(HstConfigEditor.class, HST_CONFIG_EDITOR_JS));
    }

    @Override
    public void addHeaderContribution(final IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addJavascriptReference(new JavascriptResourceReference(HstConfigEditor.class, HST_EXTWIREFRAME_JS));
    }

}
