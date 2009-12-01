package org.hippoecm.editor.template;

import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;

public interface ITemplateLocator extends IClusterable {
    
    IClusterConfig getTemplate(Map<String, Object> criteria) throws StoreException;

}
