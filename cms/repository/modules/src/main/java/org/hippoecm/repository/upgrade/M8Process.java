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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class M8Process extends M8 implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public M8Process() {
    }

    public void register(final UpdaterContext context) {
        context.registerName("m8-process");
        context.registerStartTag("m8-begin");
        context.registerEndTag("m8-process");
        context.registerVisitor(new UpdaterItemVisitor.Default() {
            @Override
            public void leaving(Node node, int level) throws RepositoryException {
                if (level == 0) {
                    String[] items = new String[] {
                        "hippostd-queries",
                        "hippogallery-images",
                        "hippogallery-files",
                        "hippogallery-image",
                        "core-workflows",
                        "domain-defaultwrite",
                        "domain-defaultread",
                        "domain-hippofolders",
                        "domain-hippodocuments",
                        "domain-workflow",
                        "domain-hipporequests",
                        "cms-static",
                        "cms-editor",
                        "cms-preview",
                        "cms-dashboard",
                        "cms-reports",
                        "cms-folder-views"
                    };
                    node = node.getNode("hippo:configuration/hippo:initialize");
                    /*for(String item : items) {
                        if(node.hasNode(item)) {
                            node.getNode(item).remove();
                        }
                    }*/
                }
            }
            @Override
            public void entering(Property prop, int level) throws RepositoryException {
                if (prop.getName().equals("hippo:uri")) {
                    String currentURI = prop.getString();
                    for (NamespaceMapping mapping : mappings) {
                        if (mapping.oldNamespaceURI.equals(currentURI)) {
                            prop.setValue(mapping.newNamespaceURI);
                            return;
                        }
                    }
                }
            }
        });
    }
}
