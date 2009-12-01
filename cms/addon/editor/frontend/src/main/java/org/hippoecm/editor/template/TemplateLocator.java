package org.hippoecm.editor.template;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;

public class TemplateLocator implements ITemplateLocator {
    private static final long serialVersionUID = 1L;

    private IStore[] stores;

    public TemplateLocator(IStore[] stores) {
        this.stores = stores;
    }

    public IClusterConfig getTemplate(Map<String, Object> criteria) throws StoreException {
        Map<String, IClusterConfig> templates = new LinkedHashMap<String, IClusterConfig>();
        for (int i = 0; i < stores.length; i++) {
            Iterator<IClusterConfig> iter = stores[i].find(criteria);
            if (iter.hasNext()) {
                return iter.next();
            }
        }
        throw new StoreException("Could not find template");
    }

}
