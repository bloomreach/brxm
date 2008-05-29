package org.hippoecm.frontend.plugins.cms.management;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;

public interface FlushableSortableDataProvider extends ISortableDataProvider {
    
    void flush();
}
