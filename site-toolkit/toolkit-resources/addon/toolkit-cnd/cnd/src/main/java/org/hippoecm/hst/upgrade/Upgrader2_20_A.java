/*
 *  Copyright 2010 Hippo.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.ext.UpdaterItemVisitor.NamespaceVisitor;
import org.hippoecm.repository.ext.UpdaterItemVisitor.NodeTypeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader2_20_A implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static Logger log = LoggerFactory.getLogger(Upgrader2_20_A.class);

    private String oldUri;
    private String prefix;
    private Set<String> oldNodeTypes;

    public Upgrader2_20_A() {
        prefix = "hst";
        oldUri = "http://www.hippoecm.org/hst/nt/1.4";
        oldNodeTypes = new TreeSet<String>();
        oldNodeTypes.add("descriptive");
        oldNodeTypes.add("template");
        oldNodeTypes.add("templates");
        oldNodeTypes.add("component");
        oldNodeTypes.add("components");
        oldNodeTypes.add("pages");
        oldNodeTypes.add("sitemapitem");
        oldNodeTypes.add("sitemap");
        oldNodeTypes.add("sitemenuitem");
        oldNodeTypes.add("sitemenu");
        oldNodeTypes.add("sitemenus");
        oldNodeTypes.add("configuration");
        oldNodeTypes.add("site");
        oldNodeTypes.add("sites");
        oldNodeTypes.add("virtualhost");
        oldNodeTypes.add("virtualhosts");
        oldNodeTypes.add("formdata");
    }
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrader-hst2_20A");
        context.registerEndTag("hst-2_1");
        context.registerVisitor(new NamespaceVisitor(context, prefix, getClass().getClassLoader().getResourceAsStream("hst-types.cnd")));
        for (final String oldNodeType : oldNodeTypes) {
            context.registerVisitor(new NodeTypeVisitor(prefix + ":" + oldNodeType) {
                @Override
                public void leaving(Node node, int level) {
                    try {
                        context.setPrimaryNodeType(node, "{" + oldUri + "}" + oldNodeType);
                    } catch (RepositoryException ex) {
                        log.error("Exception during moving old hst configuration to new prefix : " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                }
            });
        }
    }
}
