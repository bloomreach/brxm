/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.frontend.plugins.yui.autocomplete;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper.JsConfig;

public abstract class AutoCompleteBehavior extends AbstractDefaultAjaxBehavior {
    private static final long serialVersionUID = 1L;

    private static final PackagedTextTemplate INIT_AUTOCOMPLETE = new PackagedTextTemplate(AutoCompleteBehavior.class,
            "init_autocomplete.js");

    protected final HeaderContributorHelper contribHelper = new HeaderContributorHelper();
    protected final AutoCompleteSettings settings;

    //convenience constructor
    //TODO: remove?
    public AutoCompleteBehavior(final String containerId) {
        this(new AutoCompleteSettings().setContainerId(containerId));
    }

    public AutoCompleteBehavior(final AutoCompleteSettings settings) {
        this.settings = settings;

        contribHelper.addModule(HippoNamespace.NS, "autocompletemanager");
        contribHelper.addTemplate(contribHelper.new HippoTemplate(INIT_AUTOCOMPLETE, getModuleClass()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public JsConfig getJsConfig() {
                return AutoCompleteBehavior.this.getJsConfig();
            }

        });
        contribHelper.addOnload("YAHOO.hippo.AutoCompleteManager.onLoad()");
    }

    protected JsConfig getJsConfig() {
        JsConfig conf = settings.getJsConfig();
        StringBuffer buf = new StringBuffer();
        buf.append("function doCallBack").append(getComponent().getMarkupId(true)).append("(myCallbackUrl){ ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        conf.put("callbackMethod", buf.toString(), false);
        conf.put("callbackUrl", getCallbackUrl().toString());
        return conf;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        contribHelper.renderHead(response);
    }

    /**
     * Determines which javascript class is used
     * @return
     */
    protected String getModuleClass() {
        return "YAHOO.hippo.HippoAutoComplete";
    }

}
