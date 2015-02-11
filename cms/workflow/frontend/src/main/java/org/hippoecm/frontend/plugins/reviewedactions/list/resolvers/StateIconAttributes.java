/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.list.resolvers;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observable;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.NT_PUBLISHABLESUMMARY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;

/**
 * Standard attributes of a hippostdpubwf:document document.  Figures out what css classes
 * should be used to represent the state.  Can be used with handles, documents and (document)
 * versions.
 */
public class StateIconAttributes implements IObservable, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(StateIconAttributes.class);

    private JcrNodeModel nodeModel;
    private Observable observable;
    private transient boolean loaded = false;

    private transient String cssClass;
    private transient String summary;

    private transient String createdBy;
    private transient Calendar creationDate;
    private transient String lastModifiedBy;
    private transient Calendar lastModifiedDate;
    private transient Calendar publicationDate;

    public StateIconAttributes(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        observable = new Observable(nodeModel);
    }

    public String getSummary() {
        load();
        return summary;
    }

    public String getCssClass() {
        load();
        return cssClass;
    }

    public String getCreatedBy() {
        load();
        return createdBy;
    }

    public Calendar getCreationDate() {
        load();
        return creationDate;
    }

    public String getLastModifiedBy() {
        load();
        return lastModifiedBy;
    }

    public Calendar getLastModifiedDate() {
        load();
        return lastModifiedDate;
    }

    public Calendar getPublicationDate() {
        load();
        return publicationDate;
    }

    public void detach() {
        loaded = false;

        summary = null;
        cssClass = null;

        createdBy = null;
        creationDate = null;
        lastModifiedBy = null;
        lastModifiedDate = null;
        publicationDate = null;

        nodeModel.detach();
        observable.detach();
    }

    void load() {
        if (loaded) {
            return;
        }

        observable.setTarget(null);
        final Node node = nodeModel.getNode();
        if (node == null) {
            return;
        }

        try {
            Node document = null;
            NodeType primaryType = null;
            boolean isHistoric = false;
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                document = retrieveProperties(node.getNodes(node.getName()));
                primaryType = document.getPrimaryNodeType();
            } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                document = node;
                primaryType = document.getPrimaryNodeType();
                retrieveProperties(document);
            } else if (node.isNodeType("nt:version")) {
                isHistoric = true;
                Node frozen = node.getNode("jcr:frozenNode");
                String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
                NodeTypeManager ntMgr = frozen.getSession().getWorkspace().getNodeTypeManager();
                primaryType = ntMgr.getNodeType(primary);
                if (primaryType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    document = frozen;
                }
                retrieveProperties(document);
            }
            if (document != null
                    && (primaryType.isNodeType(NT_PUBLISHABLESUMMARY)
                    || document.isNodeType(NT_PUBLISHABLESUMMARY))) {
                cssClass = StateIconAttributeModifier.PREFIX
                        + (isHistoric ? "prev-" : "")
                        + document.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                IModel<String> stateModel = new JcrPropertyValueModel<>(new JcrPropertyModel(document
                        .getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)));
                summary = new TypeTranslator(new JcrNodeTypeModel(
                        NT_PUBLISHABLESUMMARY)).getValueName(
                        HippoStdNodeType.HIPPOSTD_STATESUMMARY, stateModel).getObject();

                observable.setTarget(new JcrNodeModel(document));
            }
        } catch (RepositoryException repositoryException) {
            try {
                log.error("Unable to obtain state properties, nodeModel path: " + node.getPath(), repositoryException);
            } catch (RepositoryException nodeModelPathException) {
                log.error("Unable to obtain state properties", repositoryException);
                log.error("Unable to get path of node model", nodeModelPathException);
            }
        }
        loaded = true;
    }

    private Node retrieveProperties(final NodeIterator variants) throws RepositoryException {
        Node variant = null;

        while (variants.hasNext()) {
            variant = variants.nextNode();
            if(variant.hasProperty(HIPPOSTD_STATESUMMARY) && variant.hasProperty(HIPPOSTD_STATE)) {
                final String stateSummary = variant.getProperty(HIPPOSTD_STATESUMMARY).getString();
                final String state = variant.getProperty(HIPPOSTD_STATE).getString();

                // set publication date from the published node when statesummary = live or changed
                if(variant.hasProperty(HIPPOSTDPUBWF_PUBLICATION_DATE)) {
                    if(PUBLISHED.equals(state) && ("live".equals(stateSummary)) || "changed".equals(stateSummary)) {
                        publicationDate = variant.getProperty(HIPPOSTDPUBWF_PUBLICATION_DATE).getDate();
                    }
                }

                // last modified date & its user from published node when stateSummary = live, but from unpublished node
                // when stateSummery = changed or new
                if(variant.hasProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY)
                        && variant.hasProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE)) {
                    if ((PUBLISHED.equals(state) && ("live".equals(stateSummary)))
                            || (UNPUBLISHED.equals(state) && (("changed".equals(stateSummary)) || ("new".equals(stateSummary))))) {
                        lastModifiedBy = variant.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString();
                        lastModifiedDate = variant.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE).getDate();
                    }
                }
            }
        }

        // return any variant from which the stateSummary is used later on, which is the same for all variants
        return variant;
    }

    private void retrieveProperties(Node document) throws RepositoryException {
        if (document == null) {
            return;
        }

        if (document.hasProperty(HIPPOSTDPUBWF_PUBLICATION_DATE)) {
            publicationDate = document.getProperty(HIPPOSTDPUBWF_PUBLICATION_DATE).getDate();
        }
        if (document.hasProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE)) {
            lastModifiedDate = document.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE).getDate();
        }
        if (document.hasProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY)) {
            lastModifiedBy = document.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString();
        }
        if (document.hasProperty(HIPPOSTDPUBWF_CREATION_DATE)) {
            creationDate = document.getProperty(HIPPOSTDPUBWF_CREATION_DATE).getDate();
        }
        if (document.hasProperty(HIPPOSTDPUBWF_CREATED_BY)) {
            createdBy = document.getProperty(HIPPOSTDPUBWF_CREATED_BY).getString();
        }

    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        observable.setObservationContext(context);
    }

    public void startObservation() {
        observable.startObservation();
    }

    public void stopObservation() {
        observable.stopObservation();
    }

}
