package org.onehippo.cms7.channelmanager.templatecomposer;

import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;

/**
 *  A {@link BreadCrumbPanel} that wraps the {@link org.onehippo.cms7.channelmanager.templatecomposer.PageEditor} for
 *  the ChannelManager usage.
 */
public class TemplateComposerPanel extends BreadCrumbPanel {

    public TemplateComposerPanel(String id, IBreadCrumbModel breadCrumbModel, IPluginConfig config) {
        super(id, breadCrumbModel);
        add(new PageEditor("template-composer-panel", config));
    }

    @Override
    public String getTitle() {
        return "Template Composer";
    }
}
