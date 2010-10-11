package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.Iterator;
import java.util.List;
import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModifiedDocumentsProvider implements ISortableDataProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ReferringDocumentsProvider.java 23894 2010-09-09 16:09:43Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ModifiedDocumentsProvider.class);

    private SortState state = new SortState();
    private List<JcrNodeModel> changedTabs;

    public ModifiedDocumentsProvider(List<JcrNodeModel> changedTabs) {
        this.changedTabs = changedTabs;
    }


    public Iterator iterator(int first, int count) {
        return changedTabs.subList(first, first + count).iterator();
    }


    public int size() {
        return changedTabs.size();
    }

    public IModel model(Object object) {
        return (IModel) object;
    }


    public ISortState getSortState() {
        return state;
    }

    public void setSortState(ISortState state) {
        this.state = (SortState) state;
    }

    public void detach() {
    }
}
