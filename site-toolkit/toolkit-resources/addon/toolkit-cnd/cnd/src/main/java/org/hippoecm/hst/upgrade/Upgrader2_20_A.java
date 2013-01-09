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
package org.hippoecm.hst.upgrade;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor.NamespaceVisitor;
import org.hippoecm.repository.ext.UpdaterItemVisitor.NodeTypeVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader2_20_A implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static Logger log = LoggerFactory.getLogger(Upgrader2_20_A.class);

    /* This controls the mode of operation.  Currently only removing all
     * existing HST configuration is working.
     */
    private static boolean removeAll = true;

    private String oldUri;
    private String prefix;
    private Set<String> oldPrimaryNodeTypes;
    private Set<String> oldMixinNodeTypes;

    public Upgrader2_20_A() {
        prefix = "hst";
        oldUri = "http://www.hippoecm.org/hst/nt/1.4";
        oldPrimaryNodeTypes = new TreeSet<String>();
        oldMixinNodeTypes = new TreeSet<String>();
        oldPrimaryNodeTypes.add("template");
        oldPrimaryNodeTypes.add("templates");
        oldPrimaryNodeTypes.add("component");
        oldPrimaryNodeTypes.add("components");
        oldPrimaryNodeTypes.add("pages");
        oldPrimaryNodeTypes.add("sitemapitem");
        oldPrimaryNodeTypes.add("sitemap");
        oldPrimaryNodeTypes.add("sitemenuitem");
        oldPrimaryNodeTypes.add("sitemenu");
        oldPrimaryNodeTypes.add("sitemenus");
        oldPrimaryNodeTypes.add("configuration");
        oldPrimaryNodeTypes.add("site");
        oldPrimaryNodeTypes.add("sites");
        oldPrimaryNodeTypes.add("virtualhost");
        oldPrimaryNodeTypes.add("virtualhosts");
        oldPrimaryNodeTypes.add("formdata");
        oldMixinNodeTypes.add("descriptive");
        oldMixinNodeTypes.add("primarydomain");
        oldMixinNodeTypes.add("redirectdomain");
    }
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrader-hst2_20A");
        context.registerEndTag("hst-2_1");
        context.registerVisitor(new NamespaceVisitor(context, prefix, getClass().getClassLoader().getResourceAsStream("hst-types.cnd")));
        for (final String oldNodeType : oldPrimaryNodeTypes) {
            context.registerVisitor(new NodeTypeVisitor(prefix + ":" + oldNodeType) {
                    @Override
                    public void leaving(Node node, int level) {
                        try {
                            if (removeAll) {
                                node.remove();
                            } else {
                                if (node.getName().startsWith("hst:")) {
                                    context.setName(node, "{"+oldUri+"}"+node.getName().substring(node.getName().indexOf(":")+1));
                                }
                                context.setPrimaryNodeType(node, "{" + oldUri + "}" + oldNodeType);
                                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                                    Node child = iter.nextNode();
                                    String childName = child.getName();
                                    if (childName.startsWith("hst:")) {
                                        context.setName(child, "{"+oldUri+"}"+childName.substring(childName.indexOf(":")+1));
                                    }
                                }
                            }
                        } catch (RepositoryException ex) {
                            log.error("Exception during moving old hst configuration to new prefix : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        }
                    }
                });
        }
        for (final String oldNodeType : oldMixinNodeTypes) {
            context.registerVisitor(new NodeTypeVisitor(prefix + ":" + oldNodeType) {
                    @Override
                    public void leaving(Node node, int level) {
                        try {
                            if (removeAll) {
                                node.remove();
                            } else {
                                Set<String> currentMixins = new TreeSet<String>();
                                if (node.hasProperty("jcr:mixinTypes")) {
                                    for(Value value : node.getProperty("jcr:mixinTypes").getValues()) {
                                        currentMixins.add(value.getString());
                                    }
                                }
                                if (currentMixins.contains(prefix + ":" + oldNodeType)) {
                                    node.removeMixin(prefix + ":" + oldNodeType);
                                    node.addMixin("{" + oldUri + "}" + oldNodeType);
                                }
                            }
                        } catch (ItemNotFoundException ex) {
                            if (!removeAll) {
                                log.error("Exception during moving old hst configuration to new prefix : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                            }
                        } catch (RepositoryException ex) {
                            log.error("Exception during moving old hst configuration to new prefix : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        }
                    }
                });
        }
        context.registerVisitor(new NodeTypeVisitor("rep:root") {
                    @Override
                    public void leaving(Node node, int level) {
                        try {
                            if (node.hasNode("hst_1_4:configuration")) {
                                node.getNode("hst_1_4:configuration").remove();
                            }
                            if (node.hasNode("hst:configuration")) {
                                Node configuration = node.getNode("hst:configuration");
                                if (!configuration.getNodes().hasNext()) {
                                    configuration.remove();
                                }
                            }
                        } catch (RepositoryException ex) {
                            log.error("Exception during moving old hst configuration to new prefix : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        }
                    }
            });
    }
}
