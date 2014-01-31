/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.DefaultCopyHandler;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.PropInfo;
import org.onehippo.repository.documentworkflow.DocumentVariant;

/**
 * Custom workflow task for restoring a version to a specified target node.
 */
public class VersionRestoreToTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private Document target;
    private DocumentVariant variant;
    private Calendar historic;

    private static void restore(Node target, Node source) throws RepositoryException {
        JcrUtils.copyToChain(source, new DefaultCopyHandler(target) {
            @Override
            public void setProperty(final PropInfo prop) throws RepositoryException {
                final String name = prop.getName();
                if (name.startsWith("jcr:frozen") || name.startsWith("jcr:uuid") ||
                        name.equals(HippoNodeType.HIPPO_RELATED) ||
                        name.equals(HippoNodeType.HIPPO_COMPUTE) ||
                        name.equals(HippoNodeType.HIPPO_PATHS) ||
                        name.equals(HippoStdNodeType.HIPPOSTD_STATE)) {
                    return;
                }
                super.setProperty(prop);
            }
        });
    }

    private static void clear(Node node) throws RepositoryException {
        Set<String> immune = new TreeSet();

        if (node.hasProperty("hippostd:state")) {
            // backwards compatibility
            immune.add("hippostd:state");
        }
        immune.add(HippoNodeType.HIPPO_RELATED);
        immune.add(HippoNodeType.HIPPO_COMPUTE);
        immune.add(HippoNodeType.HIPPO_PATHS);
        for (NodeIterator childIter = node.getNodes(); childIter.hasNext(); ) {
            Node child = childIter.nextNode();
            if (child != null) {
                if (child.getDefinition().isAutoCreated()) {
                    clear(child);
                } else {
                    child.remove();
                }
            }
        }

        for (PropertyIterator propIter = node.getProperties(); propIter.hasNext(); ) {
            Property property = propIter.nextProperty();
            if (property.getName().startsWith("jcr:") || property.getDefinition().isProtected()
                    || immune.contains(property.getName())) {
                continue;
            }
            property.remove();
        }
    }

    public Document getTarget() {
        return target;
    }

    public void setTarget(Document target) {
        this.target = target;
    }

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    public Calendar getHistoric() {
        return historic;
    }

    public void setHistoric(final Calendar historic) {
        this.historic = historic;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (getVariant() == null || getVariant().getNode() == null ||
                getTarget() == null || getTarget().getIdentity() == null ||
                getHistoric() == null) {
            throw new WorkflowException("Variant, target or date not provided");
        }
        Node targetNode = getDocumentHandle().getWorkflowContext().getInternalWorkflowSession().getNodeByIdentifier(target.getIdentity());
        Node variantNode = getVariant().getNode();

        final Version version = lookupVersion(variantNode, getHistoric());
        if (version != null) {
            clear(targetNode);
            restore(targetNode, version.getFrozenNode());
            if (targetNode.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)) {
                final Calendar cal = Calendar.getInstance();
                targetNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, cal);
            }
            targetNode.save();
            return new DocumentVariant(targetNode);
        }
        return null;
    }

}
