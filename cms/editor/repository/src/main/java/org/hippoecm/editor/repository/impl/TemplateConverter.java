/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.editor.repository.impl;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.standardworkflow.Change;
import org.hippoecm.repository.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateConverter implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(TemplateConverter.class);

    private String prefix;

    public TemplateConverter(Map<String, List<Change>> changes) throws RepositoryException {
        Iterator<String> iter = changes.keySet().iterator();
        if (iter.hasNext()) {
            prefix = iter.next();
            prefix = prefix.substring(0, prefix.indexOf(":"));
        } else {
            prefix = null;
        }
    }

    String rename(String newVersion, String oldName) {
        if (oldName.startsWith(prefix + ":"))
            return prefix + "_" + newVersion.replaceAll("\\.", "_") + ":" + oldName.substring(oldName.indexOf(":") + 1);
        else
            return oldName;
    }

    public void register(final UpdaterContext context) {
        try {
            final String oldUri = context.getWorkspace().getNamespaceRegistry().getURI(prefix);
            if (prefix != null) {
                final String parentPath = "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix;
                context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor(HippoNodeType.NT_TEMPLATETYPE) {
                    @Override
                    protected void leaving(Node node, int level) throws RepositoryException {
                        NamespaceRegistry nsReg = context.getWorkspace().getNamespaceRegistry();
                        NodeTypeManager ntMgr = context.getWorkspace().getNodeTypeManager();
                        if (node.getParent().getPath().equals(parentPath)) {
                            Node draft = null, current = null;
                            String newUri = nsReg.getURI(prefix);
                            if (log.isDebugEnabled()) {
                                log.debug("Converting " + node.getPath() + " from uri " + oldUri + " to " + newUri);
                            }
                            if (node.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                                for (NodeIterator iter = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(
                                        HippoNodeType.HIPPOSYSEDIT_NODETYPE); iter.hasNext();) {
                                    Node candidate = iter.nextNode();
                                    if (candidate.isNodeType(HippoNodeType.NT_REMODEL)) {
                                        if (candidate.getProperty(HippoNodeType.HIPPO_URI).getString().equals(oldUri)) {
                                            current = candidate;
                                            if (draft == null)
                                                draft = candidate;
                                        }
                                    } else {
                                        draft = candidate;
                                    }
                                }
                            }

                            String newVersion = VersionNumber.versionFromURI(newUri).toString();
                            String newTypeName = rename(newVersion, prefix + ":" + node.getName());
                            final String oldVersion = VersionNumber.versionFromURI(oldUri).toString();
                            if (log.isDebugEnabled()) {
                                log.debug("Old version: " + oldVersion + ", new: " + newVersion);
                            }

                            try {
                                /* NodeType newType = */ntMgr.getNodeType(newTypeName);

                                if (draft != null) {
                                    if (draft.isNodeType(HippoNodeType.NT_REMODEL)) {
                                        if (log.isDebugEnabled()) {
                                            log.debug("Creating new nodetype revision by copying old one "
                                                    + draft.getPath());
                                        }
                                        draft = ((HippoSession) draft.getSession()).copy(draft, draft.getPath());
                                    }
                                    draft.addMixin(HippoNodeType.NT_REMODEL);
                                    draft.setProperty(HippoNodeType.HIPPO_URI, newUri);
                                }
                                if (current != null) {
                                    renameItems(current, context, oldVersion);
                                }
                                if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                                    Node draftPrototype = null;
                                    for (NodeIterator iter = node.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes(); iter
                                            .hasNext();) {
                                        Node prototype = iter.nextNode();
                                        if (prototype.isNodeType("nt:unstructured")) {
                                            draftPrototype = prototype;
                                            break;
                                        }
                                    }
                                    if (draftPrototype != null) {
                                        for (NodeIterator iter = node.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes(
                                                HippoNodeType.HIPPO_PROTOTYPE); iter.hasNext();) {
                                            Node prototype = iter.nextNode();
                                            if (!prototype.isNodeType("nt:unstructured")) {
                                                prototype.remove();
                                            }
                                        }
                                        for (NodeIterator childIter = draftPrototype.getNodes(); childIter.hasNext();) {
                                            Node child = childIter.nextNode();
                                            context.setName(child, rename(newVersion, child.getName()));
                                        }
                                        for (PropertyIterator childIter = draftPrototype.getProperties(); childIter
                                                .hasNext();) {
                                            Property child = childIter.nextProperty();
                                            context.setName(child, rename(newVersion, child.getName()));
                                        }
                                        context.setPrimaryNodeType(draftPrototype, newTypeName);
                                        if (draftPrototype.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                            draftPrototype.addMixin(HippoNodeType.NT_HARDDOCUMENT);
                                            draftPrototype.setProperty(HippoNodeType.HIPPO_PATHS, new Value[] {});
                                        } else if (draftPrototype.isNodeType(HippoNodeType.NT_REQUEST)) {
                                            draftPrototype.addMixin("mix:referenceable");
                                        }
                                        createMandatoryProperties(draftPrototype, newTypeName, ntMgr);
                                    }
                                }
                            } catch (NoSuchNodeTypeException ex) {
                                if (draft != null && !draft.isNodeType(HippoNodeType.NT_REMODEL)) {
                                    draft.remove();
                                }
                                if (current != null) {
                                    renameItems(current, context, oldVersion);
                                }
                                if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                                    node.getNode(HippoNodeType.HIPPO_PROTOTYPES).remove();
                                }
                            }

                        }
                    }

                    private void renameItems(Node current, final UpdaterContext context, final String oldVersion)
                            throws RepositoryException {
                        current.accept(new ItemVisitor() {
                            public void visit(Node node) throws RepositoryException {
                                Node canonical = node;
                                if (node instanceof HippoNode) {
                                    canonical = ((HippoNode) node).getCanonicalNode();
                                }
                                if (node == canonical) {
                                    for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                                        Node child = iter.nextNode();
                                        if (child != null) {
                                            visit(child);
                                        }
                                    }
                                    for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                                        Property property = iter.nextProperty();
                                        if (property != null) {
                                            visit(property);
                                        }
                                    }
                                    if (node.getName().startsWith(prefix + ":")) {
                                        context.setName(node, rename(oldVersion, node.getName()));
                                    }
                                }
                            }

                            public void visit(Property property) throws RepositoryException {
                                if (property.getName().startsWith(prefix + ":")) {
                                    context.setName(property, rename(oldVersion, property.getName()));
                                }
                                if (context.isMultiple(property)) {
                                    String[] values = new String[property.getValues().length];
                                    int i = 0;
                                    for (Value value : property.getValues()) {
                                        values[i++] = rename(oldVersion, value.getString());
                                    }
                                    property.setValue(values);
                                } else {
                                    String value = property.getString();
                                    if (value.startsWith(prefix + ":")) {
                                        property.setValue(rename(oldVersion, value));
                                    }
                                }
                            }
                        });
                    }

                    private void createMandatoryProperties(Node draftPrototype, String newTypeName,
                            NodeTypeManager ntMgr) throws NoSuchNodeTypeException, RepositoryException,
                            ValueFormatException, VersionException, LockException, ConstraintViolationException {
                        NodeType nt = ntMgr.getNodeType(newTypeName);
                        NodeType[] supers = nt.getSupertypes();
                        NodeType[] all = new NodeType[supers.length + 1];
                        System.arraycopy(supers, 0, all, 0, supers.length);
                        all[supers.length] = nt;
                        for (NodeType type : all) {
                            for (PropertyDefinition pdef : type.getPropertyDefinitions()) {
                                if (pdef.getDeclaringNodeType() == type) {
                                    if (pdef.isMandatory() && !pdef.isProtected() && !"*".equals(pdef.getName())
                                            && !draftPrototype.hasProperty(pdef.getName())) {
                                        if (pdef.isMultiple()) {
                                            draftPrototype.setProperty(pdef.getName(), new Value[0]);
                                        } else {
                                            switch (pdef.getRequiredType()) {
                                            case PropertyType.LONG:
                                                draftPrototype.setProperty(pdef.getName(), 0);
                                                break;
                                            case PropertyType.DOUBLE:
                                                draftPrototype.setProperty(pdef.getName(), 0.0f);
                                                break;
                                            case PropertyType.DATE:
                                                draftPrototype.setProperty(pdef.getName(), Calendar.getInstance());
                                                break;
                                            case PropertyType.REFERENCE:
                                                draftPrototype.setProperty(pdef.getName(), draftPrototype.getSession()
                                                        .getRootNode());
                                                break;
                                            case PropertyType.STRING:
                                                String[] constraints = pdef.getValueConstraints();
                                                if (constraints != null && constraints.length > 0) {
                                                    draftPrototype.setProperty(pdef.getName(), constraints[0]);
                                                    break;
                                                }
                                            default:
                                                draftPrototype.setProperty(pdef.getName(), "");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
            }
        } catch (RepositoryException ex) {
            log.error("Error converting", ex);
        }
    }
}
