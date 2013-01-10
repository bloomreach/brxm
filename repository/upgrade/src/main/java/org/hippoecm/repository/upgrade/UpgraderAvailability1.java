/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.ext.UpdaterItemVisitor.PathVisitor;
import org.hippoecm.repository.util.Utilities;
import org.hippoecm.repository.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgraderAvailability1 implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(UpgraderAvailability1.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v13a-availability");
        context.registerStartTag("v13a-availability-v0");
        context.registerEndTag("v13a-availability-v1");

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:document") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (!node.hasProperty("hippo:availability")) {
                    node.setProperty("hippo:availability", new String[] {"live", "preview"});
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostdpubwf:document") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                boolean isPublished = false;
                boolean isUnpublished = false;
                for (NodeIterator iter = node.getParent().getNodes(); iter.hasNext();) {
                    Node sibling = iter.nextNode();
                    if (!sibling.getName().equals(node.getName()))
                        continue;
                    String siblingState = sibling.getProperty("hippostd:state").getString();
                    if (siblingState.equals("published")) {
                        isPublished = true;
                    }
                    if (siblingState.equals("unpublished")) {
                        isUnpublished = true;
                    }
                }
                String state = node.getProperty("hippostd:state").getString();
                if (state.equals("published")) {
                    if (isUnpublished) {
                        node.setProperty("hippo:availability", new String[] {"live"});
                    } else {
                        node.setProperty("hippo:availability", new String[] {"live", "preview"});
                    }
                } else if (state.equals("unpublished")) {
                    node.setProperty("hippo:availability", new String[] {"preview"});
                } else {
                    node.setProperty("hippo:availability", new String[0]);
                }
            }
        });
    }
}
