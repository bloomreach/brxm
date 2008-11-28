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
package org.hippoecm.repository.upgrade;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class M8Bootstrap extends M8 implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public M8Bootstrap() {
        initialize();
    }

    public void register(final UpdaterContext context) {
        context.registerName("m8-bootstrap");
        context.registerEndTag("m7");
        context.registerVisitor(new UpdaterItemVisitor.Default() {
            @Override
            protected void entering(Node node, int level) throws RepositoryException {
                if(level == 0)
                    return;
                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                    Node child = iter.nextNode();
                    if(child.getName().equals("jcr:system")) {
                        child.remove();
                    } else if(child.isNodeType("hippo:facetselect")) {
                        child.remove();
                    } else if(child.isNodeType("hippo:facetsearch")) {
                        child.remove();
                    }
                }
            }
        });
    }
}
