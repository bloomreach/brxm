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
package org.hippoecm.repository.defaultcontent.upgrade;


import java.io.InputStreamReader;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultcontentUpgrader12a implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DefaultcontentUpgrader12a.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("default-content-upgrade-v12a");
        context.registerStartTag("v12a");
        context.registerEndTag("v12a-defaultcontent");

        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "defaultcontent", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("defaultcontent.cnd"))));

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("defaultcontent:news") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                if (node.hasNode("defaultcontent_1_6:date")) {
                    Node dateNode = node.getNode("defaultcontent_1_6:date");
                    Calendar value = dateNode.getProperty("hippostd:date").getDate();
                    dateNode.remove();
                    node.setProperty("defaultcontent_1_6:date", value);
                }
            }
        });

        // re-read parts of the configuration
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("defaultcontent-types")) {
                    node.getNode("defaultcontent-types").remove();
                    node.getNode("defaultcontent-type-document").remove();
                    node.getNode("defaultcontent-type-address").remove();
                    node.getNode("defaultcontent-type-article").remove();
                    node.getNode("defaultcontent-type-event").remove();
                    node.getNode("defaultcontent-type-news").remove();
                    node.getNode("defaultcontent-type-overview").remove();
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("defaultcontent")) {
                    node.getNode("defaultcontent").remove();
                }
            }
        });
    }
}
