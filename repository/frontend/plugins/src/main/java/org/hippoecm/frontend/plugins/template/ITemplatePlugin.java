package org.hippoecm.frontend.plugins.template;

import org.hippoecm.frontend.plugins.template.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.TemplateEngine;

public interface ITemplatePlugin {

    void initTemplatePlugin(FieldDescriptor descriptor, TemplateEngine engine);
}
