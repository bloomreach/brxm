package org.onehippo.cms7.essentials.components.gui.panel.provider;

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableBiMap;

import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.onehippo.cms7.essentials.dashboard.model.CatalogObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ComponentProvider extends ListDataProvider<CatalogObject> { //implements IDataProvider<CatalogObject> {

    private static Logger log = LoggerFactory.getLogger(ComponentProvider.class);

    public static final ImmutableBiMap<String, CatalogObject> COMPONENTS_MAPPING = new ImmutableBiMap.Builder<String, CatalogObject>()
            .put("Document Component",
                    new CatalogObject("essentials-component-document", "Essentials Document Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsDocumentComponent")
                            .setIconPath("images/essentials/essentials-component-document.png")
                            .setType("HST.Item")
                            .setTemplate("essentials-component-document.jsp")
                            .setDetail(true)

            )
            .put("Events Component",
                    new CatalogObject("essentials-component-events", "Essentials Events Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsEventsComponent")
                            .setIconPath("images/essentials/essentials-component-events.png")
                            .setType("HST.Item")
                            .setTemplate("essentials-component-events.jsp")

            ).put("List Component",
                    new CatalogObject("essentials-component-list", "Essentials List Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsListComponent")
                            .setIconPath("images/essentials/essentials-component-list.png")
                            .setType("HST.Item")
                            .setTemplate("essentials-component-list.jsp")

            ).put("News Component",
                    new CatalogObject("essentials-component-news", "Essentials News Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsNewsComponent")
                            .setIconPath("images/essentials/essentials-component-news.png")
                            .setType("HST.Item")
                            .setTemplate("essentials-component-news.jsp")

            )
            .build();

    public ComponentProvider() {
        super(COMPONENTS_MAPPING.values().asList());
    }

    //keyset
    public Collection<CatalogObject> values() {
        return COMPONENTS_MAPPING.values();
    }

    //values
    public Collection<String> keys() {
        return COMPONENTS_MAPPING.keySet();
    }

    public static CatalogObject get(@Nullable final Object key) {
        return COMPONENTS_MAPPING.get(key);
    }

    public static boolean containsKey(@Nullable final Object key) {
        return COMPONENTS_MAPPING.containsKey(key);
    }


}
