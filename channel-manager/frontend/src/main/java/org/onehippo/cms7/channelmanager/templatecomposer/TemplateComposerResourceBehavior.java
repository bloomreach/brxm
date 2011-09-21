package org.onehippo.cms7.channelmanager.templatecomposer;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.onehippo.cms7.channelmanager.templatecomposer.pageeditor.PageEditorBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.plugins.PluginsBundle;

public class TemplateComposerResourceBehavior extends AbstractBehavior {

    @Override
    public void bind(Component component) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.MI_FRAME));
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.MI_FRAME_MSG));
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.FLOATING_WINDOW));
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.BASE_GRID));
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.COLOR_FIELD));
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.JSONP));

            component.add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "globals.js"));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PROPERTIES_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PAGE_MODEL));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.DRAGDROPONE));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.MSG));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.REST_STORE));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.TOOLKIT_STORE));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PAGE_MODEL_STORE));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PAGE_EDITOR));
        } else {
            component.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.ALL));
            component.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.ALL));
        }
    }

}
