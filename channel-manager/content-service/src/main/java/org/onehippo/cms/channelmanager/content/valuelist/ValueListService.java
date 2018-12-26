package org.onehippo.cms.channelmanager.content.valuelist;

import java.util.Locale;

import javax.jcr.Session;

import org.onehippo.forge.selection.frontend.model.ValueList;

public interface ValueListService {

    static ValueListService get() {
        return ValueListServiceImpl.getInstance();
    }

    ValueList getValueList(String source, Locale locale, Session session);
    
    /**
     * Invalidate the value list cache
     */
    void invalidateCache();
}
