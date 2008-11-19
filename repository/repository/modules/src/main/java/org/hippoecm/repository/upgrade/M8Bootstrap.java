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
    }

    public void register(final UpdaterContext context) {
        System.err.println("BERRY REGISTER M8START MODULE");

        context.registerName("m8-bootstrap");
        context.registerEndTag("m7");
        context.registerVisitor(new UpdaterItemVisitor.Default() {
            @Override
            protected void entering(Node node, int level) throws RepositoryException {
                System.err.println("BERRY ENTER " + node.getPath() + " " + level);
                if (level == 0) {
dump(System.err, context, node);
                    NamespaceRegistry nsReg = node.getSession().getWorkspace().getNamespaceRegistry();
                    for(NamespaceMapping mapping : mappings) {
                        System.err.println("BERRY REGISTER "+mapping.prefix+"_"+mapping.newVersion+"  "+mapping.newNamespaceURI);
                        nsReg.registerNamespace(mapping.prefix+"_"+mapping.newVersion, mapping.newNamespaceURI);
                        loadNodeTypes(node.getSession().getWorkspace(), mapping.cndName, mapping.cndStream);
                    }
                }
                String nodetype = node.getProperty("jcr:primaryType").getString();
                if(isPrefix(nodetype)) {
                    node.setProperty("jcr:primaryType", rename(nodetype));                    
                }
                if (node.hasProperty("jcr:mixinTypes")) {
                    Value[] mixintypes = node.getProperty("jcr:mixinTypes").getValues();
                    boolean changed = false;
                    for (int i = 0; i < mixintypes.length; i++) {
                        if (isPrefix(mixintypes[i].getString())) {
                            changed = true;
                            mixintypes[i] = node.getSession().getValueFactory().createValue(rename(mixintypes[i].getString()));
                        }
                    }
                    if (changed) {
                        node.setProperty("jcr:mixinTypes", mixintypes);
                    }
                }
                if (isPrefix(node.getName())) {
                    context.setName(node, rename(node.getName()));
                }
            }            

            @Override
            protected void entering(Property prop, int level) throws RepositoryException {
                if (isPrefix(prop.getName())) {
                    context.setName(prop, rename(prop.getName()));
                }
            }
        });
    }
}
