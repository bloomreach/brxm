package org.onehippo.cms7.channelmanager.templatecomposer;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;

public class TemplateComposerApiResourceBehavior extends AbstractBehavior {

    private static final long serialVersionUID = 1L;

    // All individual javascript files used in the template composer API. The array order determines the include order,
    // which matters. All files listed below should also be present in the aggregation section in frontend-api/pom.xml.
    private static final String[] JAVASCRIPT_FILES = {
            "PropertiesEditor.js",
            "PlainPropertiesEditor.js",
            "VariantAdder.js",
            "PlainVariantAdder.js"
    };
    private static final String ALL_JAVASCRIPT = "template-composer-api-all.js";

    @Override
    public void bind(Component component) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            for (String jsFile : JAVASCRIPT_FILES) {
                component.add(JavascriptPackageResource.getHeaderContribution(TemplateComposerApiResourceBehavior.class, jsFile));
            }
        } else {
            component.add(JavascriptPackageResource.getHeaderContribution(TemplateComposerApiResourceBehavior.class, ALL_JAVASCRIPT));
        }
    }

}
