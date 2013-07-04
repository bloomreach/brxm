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

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;

public class HstConfigEditorResourceBehaviour extends AbstractYuiBehavior {

    private static final long serialVersionUID = 1L;

    private static final JavaScriptResourceReference EXTWIREFRAME_JS = new JavaScriptResourceReference(HstConfigEditor.class, "ExtWireframe.js");
    private static final JavaScriptResourceReference HST_CONFIG_EDITOR_JS = new JavaScriptResourceReference(HstConfigEditor.class, "HstConfigEditor.js");

    @Override
    public void onRenderHead(IHeaderResponse response) {
        response.render(JavaScriptHeaderItem.forReference(HST_CONFIG_EDITOR_JS));
    }

    @Override
    public void addHeaderContribution(final IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addJavascriptReference(EXTWIREFRAME_JS);
    }

}
