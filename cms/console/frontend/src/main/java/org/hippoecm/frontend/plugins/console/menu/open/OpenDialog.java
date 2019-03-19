/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.console.menu.open;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutoCompleteTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodecFactory;

public class OpenDialog extends Dialog<Node> {

    private final String WICKET_ELEMENT_ID = "pathOrId";

    private static final long serialVersionUID = 1L;
    private static final IValueMap SIZE = new ValueMap("width=640,height=200").makeImmutable();

    private String pathOrId;
    private NodeModelReference modelReference;

    public OpenDialog(final NodeModelReference modelReference) {
        setTitle(Model.of("Open node by path or UUID"));
        setSize(SIZE);
        
        this.modelReference = modelReference;
        add(new Label("label", Model.of("Path or UUID")));
        add(setFocus(makeValueField(getValueCurrentlySelectedJcrNode())));
    }

    private AutoCompleteTextField<String> makeValueField(final PropertyModel<String> value) {
        return new AutoCompleteTextField<String>(
                WICKET_ELEMENT_ID, value, getAutoCompleteSettings()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                Collection<String> result = new TreeSet<>();
                addMatchingNodes(input, result);
                return result.iterator();
            }

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                final String script = "document.getElementById('" + getMarkupId() + "').focus(); "
                        + "document.getElementById('" + getMarkupId() + "').select();";
                response.render(OnDomReadyHeaderItem.forScript(script));
                response.render(CssHeaderItem.forReference(new CssResourceReference(
                        DefaultCssAutoCompleteTextField.class, "DefaultCssAutoCompleteTextField.css")));
            }
        };
    }

    private PropertyModel<String> getValueCurrentlySelectedJcrNode() {
        final PropertyModel<String> value = new PropertyModel<>(this, WICKET_ELEMENT_ID);
        final Node node = modelReference.getModel().getObject();
        try {
            value.setObject(node.getPath());
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return value;
    }

    private AutoCompleteSettings getAutoCompleteSettings() {
        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setAdjustInputWidth(false)
                .setUseSmartPositioning(true)
                .setShowCompleteListOnFocusGain(true)
                .setShowListOnEmptyInput(true);
        return settings;
    }

    private void addMatchingNodes(final String path, final Collection<String> result) {
        if (StringUtils.isBlank(path)) {
            return;
        }

        final String relPath = path.trim().substring(1);
        try {
            NodeIterator nodes = getNodesFromRelativePath(relPath);
            while (nodes.hasNext()) {
                result.add(nodes.nextNode().getPath());
            }
        } catch (PathNotFoundException e) {
            addNodesMatchingOnSubstring(relPath, result);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    private NodeIterator getNodesFromRelativePath(String relPath) throws RepositoryException {
        Node node = UserSession.get().getRootNode();
        if (!relPath.isEmpty()) {
            relPath = removeDoubleSlashes(relPath);
            Node nodeAtPath = getNodeIgnoreException(node, relPath);
            if (nodeAtPath != null) {
                node = nodeAtPath;
            } else {
                // Special characters (such as colon, eg in "/hst:hst") result in false negatives.
                String encRelPath = StringCodecFactory.ISO9075Helper.encodeLocalName(relPath);
                nodeAtPath = getNodeIgnoreException(node, encRelPath);
                if (nodeAtPath != null) {
                    node = nodeAtPath;
                } else {
                    throw new PathNotFoundException("Couldn't find node with relative path: " + relPath);
                }
            }
        }
        return node.getNodes();
    }

    private String removeDoubleSlashes(String path) {
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        return path;
    }

    private Node getNodeIgnoreException(final Node node, final String path) {
        try {
            return node.getNode(path);
        } catch (RepositoryException e) {
            //ignore
        }
        return null;
    }

    private void addNodesMatchingOnSubstring(String relPath, Collection<String> result) {
        String subPath = relPath.substring(0, relPath.lastIndexOf('/') + 1);
        String remainingQuery = relPath.substring(subPath.length());
        try {
            NodeIterator nodes = getNodesFromRelativePath(subPath);
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                if (node.getName().startsWith(remainingQuery)) {
                    result.add(node.getPath());
                }
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onOk() {
        if (StringUtils.isBlank(pathOrId)) {
            error("Enter a value.");
            return;
        }
        
        pathOrId = pathOrId.trim();
        Session jcrSession = UserSession.get().getJcrSession();
        Node selected = null;
        try {
            if (pathOrId.startsWith("/")) {
                if (jcrSession.nodeExists(pathOrId)) {
                    selected = jcrSession.getNode(pathOrId);
                }
            } else {
                final Node parentNode = modelReference.getModel().getObject();
                if (parentNode.hasNode(pathOrId)) {
                    selected = parentNode.getNode(pathOrId);
                } else {
                    selected = jcrSession.getNodeByIdentifier(pathOrId);
                }
            }
        } catch (RepositoryException e) {
            //ignore
        }

        if (selected == null) {
            error("Node was not found, please try again.");
        } else {
            modelReference.setModel(new JcrNodeModel(selected));
        }
    }
}
