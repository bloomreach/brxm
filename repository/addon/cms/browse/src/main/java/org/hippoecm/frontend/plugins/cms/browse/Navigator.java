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
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionManagerBehavior;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionSettings;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class Navigator extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    BrowseService browseService;

    private DocumentCollectionView docView;

    private BrowserSectionAccordion accordion;

    public Navigator(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browseService = new BrowseService(context, config, new JcrNodeModel(config.getString("model.folder.root", "/"))) {
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
        AccordionSettings settings = new AccordionSettings(config.getPluginConfig(AccordionSettings.CONFIG_KEY));
        accordion = new BrowserSectionAccordion("sections", sections,
                new AccordionManagerBehavior(YuiPluginHelper.getManager(context), settings), this) {
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
