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
package org.hippoecm.frontend.i18n;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeNameModel extends LoadableDetachableModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(NodeNameModel.class);

    private static final long serialVersionUID = 1L;

    private JcrNodeModel nodeModel;

    public NodeNameModel(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    @Override
    protected Object load() {
        Node node = nodeModel.getNode();
        String name = "node name";
        if (node != null) {
            try {
                name = ISO9075Helper.decodeLocalName(node.getName());
                if (node.isNodeType("hippo:translated")) {
                    Locale locale = Session.get().getLocale();
                    NodeIterator nodes = node.getNodes("hippo:translation");
                    while (nodes.hasNext()) {
                        Node child = nodes.nextNode();
                        if (child.isNodeType("hippo:translation")) {
                            String language = child.getProperty("hippo:language").getString();
                            if (locale.getLanguage().equals(language)) {
                                return child.getProperty("hippo:message").getString();
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return name;
    }

    @Override
    public void detach() {
        super.detach();
        nodeModel.detach();
    }

}
