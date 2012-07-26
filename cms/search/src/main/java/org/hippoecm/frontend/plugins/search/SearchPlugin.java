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
package org.hippoecm.frontend.plugins.search;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.search.yui.SearchBehavior;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchPlugin extends RenderPlugin implements IBrowseService<IModel> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchPlugin.class);

    public SearchPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        TextField tx = new TextField("searchBox");
        tx.add(new SearchBehavior(config, this));
        add(tx);
    }

    public void browse(IModel model) {
        String browserId = getPluginConfig().getString("browser.id");
        IBrowseService<IModel> browseService = getPluginContext().getService(browserId, IBrowseService.class);
        if (browseService != null && model instanceof JcrNodeModel) {
            browseService.browse(model);
        }
        IRenderService browserRenderer = getPluginContext().getService(browserId, IRenderService.class);
        if (browserRenderer != null) {
            browserRenderer.focus(null);
        } else {
            log.warn("no focus service found");
        }
    }
}
