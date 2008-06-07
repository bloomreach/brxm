package org.hippoecm.frontend.plugins.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

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
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportModel extends NodeModelWrapper implements IDataProvider, IPluginModel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReportModel.class);

    public ReportModel(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    // IDataProvider

    public Iterator iterator(int first, int count) {
        return resultSet().iterator();
    }

    public IModel model(Object object) {
        return (JcrNodeModel) object;
    }

    public int size() {
        return resultSet().size();
    }

    // privates

    private List<JcrNodeModel> resultSet() {
        List<JcrNodeModel> resultSet = new ArrayList<JcrNodeModel>();
        try {
            Node reportNode = nodeModel.getNode();
            if (reportNode.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                Node queryNode = reportNode.getNode(ReportingNodeTypes.QUERY);
                QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                HippoQuery query = (HippoQuery) queryManager.getQuery(queryNode);

                Map<String, String> arguments = new HashMap<String, String>();
                if (reportNode.hasProperty(ReportingNodeTypes.PARAMETER_NAMES)) {
                    Value[] parameterNames = reportNode.getProperty(ReportingNodeTypes.PARAMETER_NAMES).getValues();
                    Value[] parameterValues = reportNode.getProperty(ReportingNodeTypes.PARAMETER_VALUES).getValues();
                    if (parameterNames.length == parameterValues.length) {
                        for (int i = 0; i < parameterNames.length; i++) {
                            arguments.put(parameterNames[i].getString(), parameterValues[i].getString());
                        }
                    }
                }
                if (reportNode.hasProperty(ReportingNodeTypes.LIMIT)) {
                    query.setLimit(reportNode.getProperty(ReportingNodeTypes.LIMIT).getLong());
                }
                if (reportNode.hasProperty(ReportingNodeTypes.OFFSET)) {
                    query.setOffset(reportNode.getProperty(ReportingNodeTypes.OFFSET).getLong());
                }

                javax.jcr.Session session = ((UserSession)Session.get()).getJcrSession();
                session.refresh(true);
                QueryResult result;
                if (arguments.isEmpty()) {
                    result = query.execute();
                } else {
                    result = query.execute(arguments);
                }

                NodeIterator it = result.getNodes();
                while (it.hasNext()) {
                    resultSet.add(new JcrNodeModel(it.nextNode()));
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (PatternSyntaxException e) {
            //This occurs if there is a mismatch between 
            //supplied parameters and number of parameter placeholders in statement
            //Should probably be a RepositoryException
            log.error(e.getMessage());
        }
        return resultSet;
    }

    // IDetachable

    public void detach() {
        // nope
    }

    // Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("reportNode", nodeModel.toString())
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
        return new EqualsBuilder().append(nodeModel, reportModel.nodeModel).isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 131).append(nodeModel).toHashCode();
    }

}
