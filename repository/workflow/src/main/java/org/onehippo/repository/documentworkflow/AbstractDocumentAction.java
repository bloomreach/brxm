/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.CopyHandler;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeInfo;
import org.hippoecm.repository.util.OverwritingCopyHandler;
import org.hippoecm.repository.util.PropInfo;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.util.JcrConstants;

/**
 * AbstractDocumentAction
 */
public abstract class AbstractDocumentAction extends Action {

    private static final long serialVersionUID = 1L;

    protected static final String[] PROTECTED_MIXINS = new String[]{
        JcrConstants.MIX_VERSIONABLE,
        JcrConstants.MIX_REFERENCEABLE,
        HippoNodeType.NT_HARDDOCUMENT,
        HippoStdNodeType.NT_PUBLISHABLE,
        HippoStdNodeType.NT_PUBLISHABLESUMMARY,
        HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT,
        HippoNodeType.NT_SKIPINDEX
    };
    protected static final String[] PROTECTED_PROPERTIES = new String[]{
            HippoNodeType.HIPPO_AVAILABILITY,
            HippoNodeType.HIPPO_RELATED,
            HippoNodeType.HIPPO_PATHS,
            HippoStdNodeType.HIPPOSTD_STATE,
            HippoStdNodeType.HIPPOSTD_HOLDER,
            HippoStdNodeType.HIPPOSTD_STATESUMMARY
    };
    static {
        Arrays.sort(PROTECTED_PROPERTIES);
        Arrays.sort(PROTECTED_MIXINS);
    }

    @Override
    public final void execute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {
        try {
            doExecute(evtDispatcher, errRep, scInstance, appLog, derivedEvents);
        } catch (RepositoryException e) {
            throw new ModelException(e);
        }
    }

    abstract protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException, RepositoryException;

    /**
     * Returns the context object by the name.
     * @param scInstance
     * @param name
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     */
    @SuppressWarnings("unchecked")
    protected <T> T getContextAttribute(SCInstance scInstance, String name) throws ModelException {
        Context ctx = scInstance.getContext(getParentTransitionTarget());
        return (T) ctx.get(name);
    }

    /**
     * Evaluates the expression and returns the last evaluated value.
     * @param scInstance
     * @param expr
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    @SuppressWarnings("unchecked")
    protected <T> T eval(SCInstance scInstance, String expr) throws ModelException, SCXMLExpressionException {
        Context ctx = scInstance.getContext(getParentTransitionTarget());
        return (T) scInstance.getEvaluator().eval(ctx, expr);
    }

    /**
     * Returns the current workflow context instance.
     * @param scInstance
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     */
    protected WorkflowContext getWorkflowContext(SCInstance scInstance) throws ModelException {
        return getContextAttribute(scInstance, "workflowContext");
    }

    /**
     * Returns the document handle object from the current SCXML execution context.
     * @param scInstance
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    protected DocumentHandle getDocumentHandle(SCInstance scInstance) throws ModelException {
        return getContextAttribute(scInstance, "handle");
    }

    protected Node cloneDocumentNode(Node srcNode) throws RepositoryException {
        final Node parent = srcNode.getParent();
        JcrUtils.ensureIsCheckedOut(parent);

        Node destNode = parent.addNode(srcNode.getName(), srcNode.getPrimaryNodeType().getName());
        if (!destNode.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)) {
            destNode.addMixin(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        }
        if (srcNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY) && !destNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
            destNode.addMixin(HippoStdNodeType.NT_PUBLISHABLESUMMARY);
        }
        return copyTo(srcNode, destNode);
    }

    protected Node copyTo(final Node srcNode, Node destNode) throws RepositoryException {
        final CopyHandler chain = new OverwritingCopyHandler(destNode) {

            @Override
            public void startNode(final NodeInfo nodeInfo) throws RepositoryException {
                String[] oldMixins = nodeInfo.getMixinNames();
                Set<String> mixins = new HashSet<>();
                for (String mixin : oldMixins) {
                    if (Arrays.binarySearch(PROTECTED_MIXINS, mixin) >= 0) {
                        continue;
                    }
                    mixins.add(mixin);
                }
                String[] newMixins = mixins.toArray(new String[mixins.size()]);
                final NodeInfo newInfo = new NodeInfo(nodeInfo.getName(), nodeInfo.getIndex(), nodeInfo.getNodeTypeName(), newMixins);
                super.startNode(newInfo);
            }

            @Override
            protected void removeProperties(final Node node) throws RepositoryException {
                for (Property property : new PropertyIterable(node.getProperties())) {
                    if (property.getDefinition().isProtected()) {
                        continue;
                    }
                    String name = property.getName();
                    if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                        continue;
                    }
                    property.remove();
                }
            }

            @Override
            protected void replaceMixins(final Node node, final NodeInfo nodeInfo) throws RepositoryException {
                Set<String> mixinSet = new TreeSet<>();
                Collections.addAll(mixinSet, nodeInfo.getMixinNames());
                for (NodeType nodeType : node.getMixinNodeTypes()) {
                    final String mixinName = nodeType.getName();
                    if (!mixinSet.contains(mixinName)) {
                        if (Arrays.binarySearch(PROTECTED_MIXINS, mixinName) < 0) {
                            node.removeMixin(mixinName);
                        }
                    } else {
                        mixinSet.remove(mixinName);
                    }
                }
                for (String mixinName : mixinSet) {
                    node.addMixin(mixinName);
                }
            }

            @Override
            public void setProperty(final PropInfo propInfo) throws RepositoryException {
                String name = propInfo.getName();
                if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                    return;
                }
                super.setProperty(propInfo);
            }
        };
        JcrUtils.copyTo(srcNode, chain);

        return destNode;
    }

}
