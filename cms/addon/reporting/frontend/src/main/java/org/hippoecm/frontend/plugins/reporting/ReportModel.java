package org.hippoecm.frontend.plugins.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportModel extends NodeModelWrapper implements IDataProvider, IPluginModel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReportModel.class);

    private List<JcrNodeModel> resultSet;

    public ReportModel(JcrNodeModel nodeModel) {
        super(nodeModel);
        resultSet = new ArrayList<JcrNodeModel>();

        try {
            Node reportNode = nodeModel.getNode();
            if (reportNode.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                NodeIterator it = reportNode.getNodes();
                Node queryNode = null;
                while (it.hasNext()) {
                    queryNode = it.nextNode();
                    if (queryNode.isNodeType(ReportingNodeTypes.NT_QUERY)) {
                        break;
                    }
                }

                if (queryNode != null) {
                    QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                    HippoQuery query = (HippoQuery) queryManager.getQuery(queryNode);

                    QueryResult result;
                    if (reportNode.hasProperty(ReportingNodeTypes.PARAMETER_VALUES)) {
                        Value[] parameterValues = reportNode.getProperty(ReportingNodeTypes.PARAMETER_VALUES)
                                .getValues();
                        String[] values = new String[parameterValues.length];
                        for (int i = 0; i < values.length; i++) {
                            values[i] = parameterValues[i].getString();
                        }
                        result = query.execute(values);
                    } else {
                        result = query.execute();
                    }

                    it = result.getNodes();
                    while (it.hasNext()) {
                        resultSet.add(new JcrNodeModel(it.nextNode()));
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    public List<JcrNodeModel> getResultSet() {
        return resultSet;
    }

    // IDataProvider

    public Iterator iterator(int first, int count) {
        List<Node> result = new ArrayList<Node>();
        for (JcrNodeModel model : resultSet) {
            result.add(model.getNode());
        }
        return result.iterator();
    }

    public IModel model(Object object) {
        return new JcrNodeModel((Node) object);
    }

    public int size() {
        return resultSet.size();
    }

    // IPluginModel

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            map.put("node", getNodeModel().getNode().getPath());

            List<String> resultSetPaths = new ArrayList<String>();
            for (JcrNodeModel model : resultSet) {
                resultSetPaths.add(model.getNode().getPath());
            }
            map.put("resultSet", resultSetPaths);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return map;
    }

    // IDetachable

    public void detach() {
        // nope
    }
    
    // Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("reportNode", nodeModel.toString())
            .append("resultSet", resultSet.toString())
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ReportModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        ReportModel reportModel = (ReportModel) object;
        return new EqualsBuilder()
            .append(nodeModel, reportModel.nodeModel)
            .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 131)
            .append(nodeModel)
            .toHashCode();
    }

}
