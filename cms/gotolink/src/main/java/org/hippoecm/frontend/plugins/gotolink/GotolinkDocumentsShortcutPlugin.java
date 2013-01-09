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
package org.hippoecm.frontend.plugins.gotolink;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GotolinkDocumentsShortcutPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(GotolinkDocumentsShortcutPlugin.class);

    private static final long serialVersionUID = 1L;

    public GotolinkDocumentsShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String browserId = config.getString("browser.id");
                IBrowseService browseService = context.getService(browserId, IBrowseService.class);
                if (browseService != null) {
                    browseService.browse(new JcrNodeModel(config.getString("option.location", "/content")));
                } else {
                    log.warn("no browser service found");
                }
                IRenderService browserRenderer = context.getService(browserId, IRenderService.class);
                if (browserRenderer != null) {
                    browserRenderer.focus(null);
                } else {
                    log.warn("no focus service found");
                }
            }
        };
        add(link);

        add(CSSPackageResource.getHeaderContribution(GotolinkDocumentsShortcutPlugin.class, "gotolink.css"));
    }
}
