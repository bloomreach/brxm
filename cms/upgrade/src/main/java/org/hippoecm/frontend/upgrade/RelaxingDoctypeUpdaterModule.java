/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.upgrade;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public abstract class RelaxingDoctypeUpdaterModule implements UpdaterModule {

    private final String prefix;
    private final String pathToCnd;
    private String newUri;
    private String oldUri;

    public RelaxingDoctypeUpdaterModule(String prefix) {
        this(prefix, "namespaces/" + prefix + ".cnd");
    }

    public RelaxingDoctypeUpdaterModule(String prefix, String pathToCnd) {
        this.prefix = prefix;
        this.pathToCnd = pathToCnd;
    }

    @Override
    public final void register(UpdaterContext context) {
        context.registerStartTag("upgrade-doctypes-trigger");
        context.registerName(prefix + "-relaxed-doctype-upgrade");
        context.registerEndTag("upgrade-doctypes-upgraded");
        context.registerAfter("repository-upgrade-v19a");

        InputStream cndStream = getClass().getClassLoader().getResourceAsStream(pathToCnd);
        if (cndStream == null) {
            throw new RuntimeException("cnd is not available on classpath as '" + pathToCnd + "'");
        }
        try {
            CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> cndReader = new CompactNodeTypeDefReader(
                    new InputStreamReader(cndStream), prefix + ".cnd", new QDefinitionBuilderFactory());
            NamespaceMapping nsmap = cndReader.getNamespaceMapping();
            
            oldUri = context.getWorkspace().getNamespaceRegistry().getURI(prefix);
            newUri = nsmap.getURI(prefix);

            cndStream = getClass().getClassLoader().getResourceAsStream(pathToCnd);
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, prefix, cndStream));

            context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces/" + prefix) {
                @Override
                protected void leaving(Node node, int level) throws RepositoryException {
                    for (NodeIterator types = node.getNodes(); types.hasNext();) {
                        Node type = types.nextNode();
                        if (!type.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                            continue;
                        }
                        if (!type.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                            continue;
                        }

                        Node nodeType = type.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                        for (NodeIterator versions = nodeType.getNodes(); versions.hasNext();) {
                            Node version = versions.nextNode();
                            if (!version.isNodeType(HippoNodeType.NT_NODETYPE)) {
                                continue;
                            }

                            if (version.isNodeType(HippoNodeType.NT_REMODEL)) {
                                if (oldUri.equals(version.getProperty(HippoNodeType.HIPPO_URI).getString())) {
                                    version.setProperty(HippoNodeType.HIPPO_URI, newUri);
                                }
                            }
                            if (version.hasProperty(HippoNodeType.HIPPO_MIXIN)) {
                                continue;
                            }

                            if (!version.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                                version.setProperty(HippoNodeType.HIPPO_SUPERTYPE, new String[] { "hippo:compound" });
                            }
                        }

                    }
                }
            });
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
