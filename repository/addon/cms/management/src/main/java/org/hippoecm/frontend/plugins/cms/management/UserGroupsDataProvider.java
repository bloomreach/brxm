package org.hippoecm.frontend.plugins.cms.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.cms.browse.list.JcrNodeModelComparator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;

public class UserGroupsDataProvider extends SortableDataProvider {
    private static final long serialVersionUID = 1L;

    private JcrNodeModel parentNodeModel;
    private List<JcrNodeModel> nodes;
    
    public UserGroupsDataProvider(JcrNodeModel parentModel) {
        setSort("name", true);
        this.parentNodeModel = parentModel;
        nodes = getNodes();
    }
    
    public Iterator<JcrNodeModel> iterator(int first, int count) {
        sortNodes();
        List<JcrNodeModel> list = Collections.unmodifiableList(nodes.subList(first, first + count));
        return list.iterator();
    }

    public IModel model(Object object) {
        return (JcrNodeModel)object;
    }

    public int size() {
        return nodes.size();
    }
    
    private List<JcrNodeModel> getNodes() {
        List<JcrNodeModel> list = new ArrayList<JcrNodeModel>();
        try {
            String username = parentNodeModel.getNode().getName();
            
            QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
            HippoQuery query = (HippoQuery) queryManager.createQuery("//element(*, hippo:group)[jcr:contains(@hippo:members, '" + username + "')]", "xpath");

            javax.jcr.Session session = ((UserSession)Session.get()).getJcrSession();
            session.refresh(true);
            QueryResult result;
            result = query.execute();

            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                JcrNodeModel modcheck = new JcrNodeModel(it.nextNode()); 
                list.add(modcheck);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        } 
        return list;
    }
    
    private void sortNodes() {
        JcrNodeModelComparator jcrNodeModelComparator = new JcrNodeModelComparator(getSort().getProperty());
        Collections.sort(nodes, jcrNodeModelComparator);       
        if (getSort().isAscending() == false) {
            Collections.reverse(nodes);
        }
    }
    
    public void refresh() {
        nodes = getNodes();
        sortNodes();
    }

}
