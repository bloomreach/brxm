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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.standardworkflow.Change;

public class TemplateConverter implements UpdaterModule {
    
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

    public void register(final UpdaterContext context) {
        if (prefix != null) {
            context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor(HippoNodeType.NT_TEMPLATETYPE) {
                @Override
                protected void entering(Node node, int level) throws RepositoryException {
                    if (node.getParent().getName().equals(prefix)) {
                        Node draft = null;
                        String uri = node.getSession().getNamespaceURI(prefix);
                        if (node.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                            for (NodeIterator iter = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE); iter.hasNext();) {
                                Node candidate = iter.nextNode();
                                if (candidate.isNodeType(HippoNodeType.NT_REMODEL)) {
                                    if (candidate.getProperty(HippoNodeType.HIPPO_URI).getString().equals(node.getSession().getNamespaceURI(prefix))) {
                                        draft = candidate;
                                    }
                                } else {
                                    draft = candidate;
                                    break;
                                }
                            }
                        }
                        if (draft != null) {
                            if (draft.isNodeType(HippoNodeType.NT_REMODEL)) {
                                draft = ((HippoSession)draft.getSession()).copy(draft, draft.getPath());
                            } else {
                                Node n = ((HippoSession)draft.getSession()).copy(draft, draft.getPath());
                                draft.remove();
                                draft = n;
                                draft.addMixin(HippoNodeType.NT_REMODEL);
                            }
                            draft.setProperty(HippoNodeType.HIPPO_URI, uri.substring(0, uri.lastIndexOf("/") + 1) + VersionNumber.versionFromURI(uri).next().toString());
                        }
                        if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                            for (NodeIterator iter = node.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes(HippoNodeType.HIPPO_PROTOTYPES); iter.hasNext();) {
                                Node prototype = iter.nextNode();
                                if (prototype.isNodeType("nt:unstructured")) {
                                    context.setPrimaryNodeType(prototype, prefix + ":" + node.getName());
                                    prototype.addMixin(HippoNodeType.NT_HARDDOCUMENT);
                                } else {
                                    prototype.remove();
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
