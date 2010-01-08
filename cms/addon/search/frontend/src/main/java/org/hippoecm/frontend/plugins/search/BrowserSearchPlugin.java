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
package org.hippoecm.frontend.plugins.search;

import java.util.Iterator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.search.TextSearchBuilder;
import org.hippoecm.frontend.plugins.standards.search.TextSearchResultModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserSearchPlugin extends RenderPlugin<Void> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserSearchPlugin.class);

    private String query;
    private TextSearchBuilder sb;

    public BrowserSearchPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        sb = new TextSearchBuilder(new String[] { "/" });
        final IModelReference folderModelService = context.getService(config.getString("model.folder"),
                IModelReference.class);
        if (folderModelService != null) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return folderModelService;
                }

                public void onEvent(Iterator events) {
                    String scope = "/";
                    if (folderModelService != null) {
                        JcrNodeModel nodeModel = (JcrNodeModel) folderModelService.getModel();
                        scope = nodeModel.getItemModel().getPath();
                    }
                    sb = new TextSearchBuilder(new String[] { scope });
                }

            }, IObserver.class.getName());
        } else {
            log.warn("no folder model found, using root as search scope");
        }

        final IModelReference<BrowserSearchResult> searchModelService = context.getService(config
                .getString("model.search"), IModelReference.class);
        if (searchModelService != null) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return searchModelService;
                }

                public void onEvent(Iterator events) {
                    query = null;
                    IModel<BrowserSearchResult> model = searchModelService.getModel();
                    if (model != null) {
                        if (model instanceof TextSearchResultModel) {
                            query = ((TextSearchResultModel) model).getQueryString();
                        }
                    }
                }

            }, IObserver.class.getName());
        } else {
            log.error("no search model service found");
        }

        TextField tx = new TextField("searchBox", new PropertyModel(this, "query"));
        tx.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (query == null || "".equals(query)) {
                    searchModelService.setModel(new Model(null));
                } else {
                    searchModelService.setModel(sb.search(query));
                }
            }
        });
        add(tx);
    }

}
