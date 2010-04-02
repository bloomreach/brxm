/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Factory for Node {@link IEditor}s.  Use this class to create an editor for any Node.
 * <p>
 * This will likely be turned into a service at some point.  
 */
public class EditorFactory implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginConfig config;
    private IPluginContext context;

    public EditorFactory(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
    }

    public IEditor<Node> newEditor(IEditorContext manager, IModel<Node> nodeModel, IEditor.Mode mode)
            throws EditorException {
        Node node = nodeModel.getObject();
        AbstractCmsEditor<Node> editor;
        try {
            if (isNodeType(node, HippoNodeType.NT_HANDLE)) {
                Set<Node> docs = getDocuments(node);
                if (docs.size() == 0) {
                    throw new EditorException("Document has been deleted");
                }
                Node doc = docs.iterator().next();
                if (isNodeType(doc, HippoStdNodeType.NT_PUBLISHABLE)) {
                    editor = new HippostdPublishableEditor(manager, context, config, nodeModel);
                } else {
                    editor = new DefaultCmsEditor(manager, context, config, nodeModel, mode);
                }
            } else if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                editor = new TemplateTypeEditor(manager, context, config, nodeModel, mode);
            } else {
                editor = new DefaultCmsEditor(manager, context, config, nodeModel, mode);
            }
        } catch (RepositoryException e) {
            throw new EditorException("Could not determine type of editor required", e);
        }
        editor.start();
        return editor;
    }

    static Set<Node> getDocuments(Node handle) throws RepositoryException {
        Set<Node> variants = new HashSet<Node>();
        if (handle.isNodeType("nt:version")) {
            Calendar date = handle.getProperty("jcr:created").getDate();
            Node frozen = handle.getNode("jcr:frozenNode");
            NodeIterator variantHistoryReferences = frozen.getNodes();
            while (variantHistoryReferences.hasNext()) {
                Node variant = variantHistoryReferences.nextNode();
                if (!variant.isNodeType("nt:versionedChild")) {
                    continue;
                }
                Calendar latestDate = null;
                Node latestVariant = null;
                Node history = variant.getProperty("jcr:childVersionHistory").getNode();
                NodeIterator variantHistoryIter = history.getNodes("*");
                while (variantHistoryIter.hasNext()) {
                    Node variantVersion = variantHistoryIter.nextNode();
                    if (!variantVersion.isNodeType("nt:version")) {
                        continue;
                    }
                    Calendar variantVersionDate = variantVersion.getProperty("jcr:created").getDate();
                    if (variantVersionDate.compareTo(date) <= 0
                            && (latestDate == null || variantVersionDate.compareTo(latestDate) > 0)) {
                        latestDate = variantVersionDate;
                        latestVariant = variantVersion;
                    }
                }
                if (latestVariant != null) {
                    variants.add(latestVariant);
                }
            }
        } else {
            for (NodeIterator iter = handle.getNodes(handle.getName()); iter.hasNext();) {
                Node variant = iter.nextNode();
                variants.add(variant);
            }
        }
        return variants;
    }

    static boolean isNodeType(Node node, String type) throws RepositoryException {
        if (node.isNodeType("nt:version")) {
            Node frozen = node.getNode("jcr:frozenNode");
            String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
            NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
            if (ntMgr.getNodeType(primary).isNodeType(type)) {
                return true;
            }
            if (frozen.hasProperty("jcr:frozenMixinTypes")) {
                for (Value values : frozen.getProperty("jcr:frozenMixinTypes").getValues()) {
                    if (ntMgr.getNodeType(values.getString()).isNodeType(type)) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        return node.isNodeType(type);
    }
}
