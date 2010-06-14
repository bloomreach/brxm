/*
 * Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.flash;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;

public abstract class ProbeFlashBehavior extends AbstractYuiAjaxBehavior {
    final static String SVN_ID = "$Id$";

    public interface IProbeFlashHandler extends IClusterable {

        void handleFlash(AjaxRequestTarget target);

        void handleJavascript(AjaxRequestTarget target);
    }

    IProbeFlashHandler handler;
    DynamicTextTemplate template;

    public ProbeFlashBehavior(IProbeFlashHandler handler) {
        this(new AjaxSettings(), handler);
    }

    public ProbeFlashBehavior(final IAjaxSettings settings, IProbeFlashHandler handler) {
        super(settings);

        this.handler = handler;

        template = new DynamicTextTemplate(new PackagedTextTemplate(ProbeFlashBehavior.class, "probe_flash.js"), settings) {

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
    }


    @Override
    protected void respond(AjaxRequestTarget target) {
        Request request = RequestCycle.get().getRequest();

        Flash flash = new Flash();

        try {
            flash.setMajorVersion(Integer.parseInt(request.getParameter("major")));
            flash.setMinorVersion(Integer.parseInt(request.getParameter("minor")));
            flash.setRevisionVersion(Integer.parseInt(request.getParameter("rev")));
        }
        catch (NumberFormatException ignored) {
        }

        if(isValid(flash)) {
            handler.handleFlash(target);
        } else {
            handler.handleJavascript(target);
        }
    }

    protected abstract boolean isValid(Flash flash);

    public static class Flash implements IClusterable {
        int major = 0;
        int minor = 0;
        int revision = 0;

        public int getMajorVersion() {
            return major;
        }

        public int getMinorVersion() {
            return minor;
        }

        public int getRevisionVersion() {
            return revision;
        }

        public void setMajorVersion(int major) {
            this.major = major;
        }
    
        public void setMinorVersion(int minor) {
            this.minor = minor;
        }

        public void setRevisionVersion(int revision) {
            this.revision = revision;
        }

        public boolean isAvailable() {
            return major > 0;
        }

        public boolean isValid(int major, int minor, int revision) {
            if (this.major < major) {
                return false;
            }
            if (this.major > major) {
                return true;
            }
            if (this.minor < minor) {
                return false;
            }
            if (this.minor > minor) {
                return true;
            }
            if (this.revision < revision) {
                return false;
            }
            return true;
        }
    }
}
