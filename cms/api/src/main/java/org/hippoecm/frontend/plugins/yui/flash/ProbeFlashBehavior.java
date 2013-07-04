/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.EmptyAjaxRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;

public abstract class ProbeFlashBehavior extends AbstractYuiAjaxBehavior {

    private final PackageTextTemplate PROBE_FLASH_TEMPLATE = new PackageTextTemplate(ProbeFlashBehavior.class, "probe_flash.js");

    private final DynamicTextTemplate template;

    static class FlashSettings extends AjaxSettings {

        public FlashSettings() {
            super(AjaxSettings.TYPE);
        }
    }

    public ProbeFlashBehavior() {
        this(new FlashSettings());
    }

    public ProbeFlashBehavior(final IAjaxSettings settings) {
        super(settings);

        template = new DynamicTextTemplate(PROBE_FLASH_TEMPLATE, settings) {

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
            flash.setMajorVersion(request.getRequestParameters().getParameterValue("major").toInt());
            flash.setMinorVersion(request.getRequestParameters().getParameterValue("minor").toInt());
            flash.setRevisionVersion(request.getRequestParameters().getParameterValue("rev").toInt());
        } catch (NumberFormatException ignored) {
        }
        handleFlash(flash);

        //Responding with a regular AJAX-response can cause feedback info to disappear immediately
        //after page-load. This is resolved forcing an empty response for this behavior
        RequestCycle.get().scheduleRequestHandlerAfterCurrent(EmptyAjaxRequestHandler.getInstance());
    }

    protected abstract void handleFlash(FlashVersion flash);

}
