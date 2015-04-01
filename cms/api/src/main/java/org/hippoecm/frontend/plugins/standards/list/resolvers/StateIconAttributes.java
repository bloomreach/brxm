/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
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
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard attributes of a hippostd:publishable document. Figures out what CSS classes, summary
 * and icon should be used to represent the state. Can be used with handles, documents and (document)
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
    private transient Icon[] icons;

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

    public Icon[] getIcons() {
        load();
        return icons;
    }

    public void detach() {
        loaded = false;

        summary = null;
        cssClass = null;
        icons = null;

        nodeModel.detach();
        observable.detach();
    }

    void load() {
        if (!loaded) {
            observable.setTarget(null);
            try {
                final Node node = nodeModel.getNode();
                if (node != null) {
                    loadAttributes(node);
                }
            } catch (RepositoryException ex) {
                log.error("Unable to obtain state properties", ex);
            }
            loaded = true;
        }
    }

    private void loadAttributes(final Node node) throws RepositoryException {
        Node document = null;
        NodeType primaryType = null;
        boolean isHistoric = false;
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            NodeIterator docs = node.getNodes(node.getName());
            while (docs.hasNext()) {
                document = docs.nextNode();
                primaryType = document.getPrimaryNodeType();
                if (document.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                    String state = document.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                    if ("unpublished".equals(state)) {
                        break;
                    }
                }
            }
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            document = node;
            primaryType = document.getPrimaryNodeType();
        } else if (node.isNodeType("nt:version")) {
            isHistoric = true;
            Node frozen = node.getNode("jcr:frozenNode");
            String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
            NodeTypeManager ntMgr = frozen.getSession().getWorkspace().getNodeTypeManager();
            primaryType = ntMgr.getNodeType(primary);
            if (primaryType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                document = frozen;
            }
        }
        if (document != null) {
            if (primaryType.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)
                    || document.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {

                final Property stateProperty = document.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY);

                final String state = stateProperty.getString();
                cssClass = StateIconAttributeModifier.PREFIX + (isHistoric ? "prev-" : "") + state;

                final JcrPropertyModel stateSummaryModel = new JcrPropertyModel(stateProperty);
                final IModel<String> stateModel = new JcrPropertyValueModel<>(stateSummaryModel);
                final JcrNodeTypeModel nodeTypeModel = new JcrNodeTypeModel(HippoStdNodeType.NT_PUBLISHABLESUMMARY);
                final TypeTranslator typeTranslator = new TypeTranslator(nodeTypeModel);
                summary = typeTranslator.getValueName(HippoStdNodeType.HIPPOSTD_STATESUMMARY, stateModel).getObject();

                icons = getStateIcons(state);

                observable.setTarget(new JcrNodeModel(document));
            }
        }
    }

    private Icon[] getStateIcons(final String state) {
        switch(state) {
            case "new":
                return new Icon[] {Icon.MINUS_CIRCLE, Icon.EMPTY};
            case "live":
                return new Icon[] {Icon.CHECK_CIRCLE, Icon.EMPTY};
            case "changed":
                return new Icon[] {Icon.CHECK_CIRCLE, Icon.EXCLAMATION_TRIANGLE};
            default:
                log.info("No icon available for document state '{}'", state);
                return null;
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
