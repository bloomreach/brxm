/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.yui.flash;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.target.basic.EmptyAjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;

public abstract class ProbeFlashBehavior extends AbstractYuiAjaxBehavior {

    DynamicTextTemplate template;

    public ProbeFlashBehavior() {
        this(new AjaxSettings());
    }

    public ProbeFlashBehavior(final IAjaxSettings settings) {
        super(settings);

        template = new DynamicTextTemplate(new PackagedTextTemplate(ProbeFlashBehavior.class, "probe_flash.js"),
                settings) {

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "flash");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.Flash.probe();");
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        Request request = RequestCycle.get().getRequest();

        FlashVersion flash = new FlashVersion();
        try {
            flash.setMajorVersion(Integer.parseInt(request.getParameter("major")));
            flash.setMinorVersion(Integer.parseInt(request.getParameter("minor")));
            flash.setRevisionVersion(Integer.parseInt(request.getParameter("rev")));
        } catch (NumberFormatException ignored) {
        }
        handleFlash(flash);

        //Responding with a regular AJAX-response can cause feedback info to disappear immediately
        //after page-load. This is resolved forcing an empty response for this behavior
        RequestCycle.get().setRequestTarget(EmptyAjaxRequestTarget.getInstance());
    }

    protected abstract void handleFlash(FlashVersion flash);

}
