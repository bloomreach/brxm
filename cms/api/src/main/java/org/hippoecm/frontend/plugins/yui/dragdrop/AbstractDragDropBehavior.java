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
package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;

public abstract class AbstractDragDropBehavior extends AbstractYuiAjaxBehavior {

    private static final long serialVersionUID = 1L;

    protected final DragDropSettings settings;

    public AbstractDragDropBehavior(DragDropSettings settings) {
        super(settings);
        this.settings = settings;
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "dragdropmanager");

        context.addTemplate(new HippoTextTemplate(getHeaderContributorClass(), getHeaderContributorFilename(),
                getClientSideClassname()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public YuiObject getSettings() {
                updateAjaxSettings();
                return AbstractDragDropBehavior.this.settings;
            }
        });
        context.addOnDomLoad("YAHOO.hippo.DragDropManager.onLoad()");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
    }

    @Override
    protected CharSequence getCallbackScript(boolean onlyTargetActivePage) {
        StringBuilder buf = new StringBuilder();
        buf.append("function doCallBack").append(getComponent().getMarkupId(true)).append("(myCallbackUrl){ ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        return buf.toString();
    }

    /**
     * Return a class from the same package as the javascript file you want to load
     * @return Class from the same package as the javascript file you want to load
     */
    protected Class<? extends IBehavior> getHeaderContributorClass() {
        return AbstractDragDropBehavior.class;
    }

    /**
     * Provide the name of the javascript file that should be loaded on the client
     * @return Filename of the javascript
     */
    abstract protected String getHeaderContributorFilename();

    /**
     * Specify the clientside class that is used as the DragDropModel
     */
    abstract protected String getClientSideClassname();

}
