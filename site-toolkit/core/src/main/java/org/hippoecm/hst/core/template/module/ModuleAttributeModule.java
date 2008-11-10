package org.hippoecm.hst.core.template.module;

import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jreijn
 * The ModuleAttributeModule returns the value of a property
 * defined on a HST sitemap module node or from the module node itself.
 */

public class ModuleAttributeModule extends ModuleBase {

    private static final Logger log = LoggerFactory.getLogger(ModuleAttributeModule.class);
    private static final String MODULE_ATTRIBUTE_NAME = "propertyName";

    @Override
    public final void render(final PageContext pageContext) throws TemplateException {
        String propertyName = null;
        String propertyValue = null;

        if (moduleParameters != null) {
            if (moduleParameters.containsKey(MODULE_ATTRIBUTE_NAME)) {
                propertyName = moduleParameters.get(MODULE_ATTRIBUTE_NAME);
            }
        }

        try {
            propertyValue = getPropertyValueFromModuleNode(propertyName);
        } catch (TemplateException e) {
            log.error("Cannot get property " + propertyName, e);
        }
        pageContext.setAttribute(getVar(), propertyValue);
    }

}
