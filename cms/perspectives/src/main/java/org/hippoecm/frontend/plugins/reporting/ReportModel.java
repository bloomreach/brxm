/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrFrontendListener;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportModel extends NodeModelWrapper<Void> implements IDataProvider, IObservable {

    private static final long serialVersionUID = 1L;

    public static final int UNKNOWN_SIZE = -1;

    private static final Logger log = LoggerFactory.getLogger(ReportModel.class);

    private JcrFrontendListener listener;
    private IObservationContext obContext;
    private transient boolean attached = false;
    private transient QueryResult resultSet;

    public ReportModel(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    // IDataProvider

    @Override
    public Iterator iterator(long first, long count) {
        load();
        if (resultSet != null) {
            try {
                final NodeIterator nodeIterator = resultSet.getNodes();
                return new Iterator<IModel>() {
                    public boolean hasNext() {
                        return nodeIterator.hasNext();
                    }
                    public IModel next() {
                        return new JcrNodeModel(nodeIterator.nextNode());
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } catch (RepositoryException ex) {
                log.error("Failed to obtain nodes from query result");
            }
        }
        return new ArrayList(0).iterator();
    }

    @Override
    public IModel model(Object object) {
        if (object instanceof JcrNodeModel) {
            return (JcrNodeModel) object;
        } else {
            return new JcrNodeModel((Node)object);
        }
    }

    @Override
    public long size() {
        return UNKNOWN_SIZE;
    }

    // privates

    private void load() {
        if (!attached) {
            attached = true;
            try {
                Node reportNode = nodeModel.getObject();
                if (reportNode.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                    Node queryNode = reportNode.getNode(ReportingNodeTypes.QUERY);
                    QueryManager queryManager = UserSession.get().getQueryManager();
                    HippoQuery query = (HippoQuery) queryManager.getQuery(queryNode);

                    Map<String, String> arguments = new HashMap<String, String>();
                    if (reportNode.hasProperty(ReportingNodeTypes.PARAMETER_NAMES)) {
                        Value[] parameterNames = reportNode.getProperty(ReportingNodeTypes.PARAMETER_NAMES).getValues();
                        Value[] parameterValues = reportNode.getProperty(ReportingNodeTypes.PARAMETER_VALUES)
                                .getValues();
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

                    if (arguments.isEmpty()) {
                        resultSet = query.execute();
                    } else {
                        resultSet = query.execute(arguments);
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
        }
    }

    // IDetachable

    @Override
    public void detach() {
        attached = false;
        resultSet = null;
        if (listener != null) {
            listener.detach();
        }
        super.detach();
    }

    // IObservable

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
        try {
            Node node = getChainedModel().getObject();
            Node listenerNode = node.getNode(ReportingNodeTypes.LISTENER);
            listener = new JcrFrontendListener(obContext, new JcrNodeModel(listenerNode));
            listener.start();
        } catch (RepositoryException e) {
            log.error(e.toString());
        }
    }

    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

    // Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("reportNode", nodeModel.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ReportModel)) {
            return false;
        }
        ReportModel reportModel = (ReportModel) object;
        return new EqualsBuilder().append(nodeModel, reportModel.nodeModel).isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 131).append(nodeModel).toHashCode();
    }

}
