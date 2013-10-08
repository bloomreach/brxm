package org.onehippo.cms7.essentials.components.gui.panel.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.onehippo.cms7.essentials.components.gui.panel.provider.ComponentProvider;
import org.onehippo.cms7.essentials.dashboard.model.CatalogObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ComponentChoiceRenderer implements IChoiceRenderer<CatalogObject> {

    private static Logger log = LoggerFactory.getLogger(ComponentChoiceRenderer.class);

    private Map<String, CatalogObject> map = new HashMap<>();
    public static final String HIPPOESSENTIALS_PREFIX = "Essentials ";

    public ComponentChoiceRenderer(ComponentProvider provider) {
        final Iterator<? extends CatalogObject> iterator = provider.iterator(0, provider.size());
        while (iterator.hasNext()) {
            final CatalogObject next = iterator.next();
            map.put(next.getLabel().replace(HIPPOESSENTIALS_PREFIX, ""), next);
        }
    }

    @Override
    public Object getDisplayValue(final CatalogObject object) {
        return object.getLabel();
    }

    @Override
    public String getIdValue(final CatalogObject object, final int index) {
        return object.getName();
    }

    public CatalogObject getCatalogObject(final String type) {
        if (!map.isEmpty() && map.containsKey(type)) {
            return map.get(type);
        }
        return null;
    }
}
