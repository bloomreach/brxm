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
package org.hippoecm.frontend.plugins.cms.browse;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.service.BrowseService;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionConfiguration;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionManagerBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Navigator extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Navigator.class);

    private BrowseService browseService;
    private DocumentCollectionView docView;
    private BrowserSectionAccordion accordion;

    public Navigator(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browseService = new BrowseService(context, config,
                new JcrNodeModel(config.getString("model.default.path", "/"))) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onBrowse() {
                focus(null);
            }
        };

        IModel<DocumentCollection> collectionModel = browseService.getCollectionModel();
        docView = new DocumentCollectionView("documents", context, config, collectionModel, this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.list");
            }
        };
        add(docView);

        final BrowserSections sections = browseService.getSections();
        AccordionConfiguration accordionConfig = new AccordionConfiguration();
        if (config.containsKey("yui.config.accordion")) {
            try {
                PluginConfigMapper.populate(accordionConfig, config.getPluginConfig("yui.config.accordion"));
            } catch (MappingException e) {
                log.warn(e.getMessage());
            }
        }
        accordion = new BrowserSectionAccordion("sections", sections, new AccordionManagerBehavior(accordionConfig),
                this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelect(String name) {
                sections.setActiveSection(name);
            }
        };
        sections.addListener(new IChangeListener() {
            private static final long serialVersionUID = 1L;

            public void onChange() {
                accordion.select(sections.getActiveSection());
            }

        });
        add(accordion);
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        docView.render(target);
        accordion.render(target);
    }

}
