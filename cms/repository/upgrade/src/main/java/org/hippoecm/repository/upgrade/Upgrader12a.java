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
package org.hippoecm.repository.upgrade;

import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Upgrader12a implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v12a");
        context.registerStartTag("v20902");
        context.registerEndTag("v12a");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                /*
                 * The removal of the entire /hippo:log tree seems to be appropriate.  This is relatively volatile data as
                 * this is a sliding log file with the oldest entries being removed automatically.  Combine this with the
                 * fact that old entries might not contain the same information and the effort of converting data which is
                 * going to be removed quickly is unnecessary.
                 */
                if (node.hasNode("hippo:log")) {
                    for(NodeIterator iter=node.getNode("hippo:log").getNodes(); iter.hasNext(); ) {
                        iter.nextNode().remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostd:html") {
            Pattern pattern = Pattern.compile("(<img[^>]*src=\")([^/]*)/([^/]*)/([^\"]*\")", Pattern.MULTILINE);
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                Property htmlProperty = node.getProperty("hippostd:content");
                Matcher matcher = pattern.matcher(htmlProperty.getString());
                StringBuffer sb = new StringBuffer();
                while(matcher.find()) {
                    matcher.appendReplacement(sb, "$1$2/{_document}/$4");
                }
                matcher.appendTail(sb);
                htmlProperty.setValue(new String(sb));
              }
        });
        for(String path : new String[] {
            "/hippo:configuration/hippo:queries/hippo:templates/Template Editor Namespace",
            "/hippo:configuration/hippo:queries/hippo:templates/new-type",
            "/hippo:configuration/hippo:initialize/templateeditor-namespace.xml",
            "/hippo:configuration/hippo:initialize/templateeditor-type-query.xml"
        }) {
            context.registerVisitor(new UpdaterItemVisitor.PathVisitor(path) {
                @Override
                public void leaving(final Node node, int level) throws RepositoryException {
                    node.remove();
                }
            });
        }
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hipposysedit:nodetype") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                context.setName(node, "hipposysedit_1_1:nodetype");
                context.setPrimaryNodeType(node, "hipposysedit_1_1:nodetype");
                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                    Node field = iter.nextNode();
                    context.setPrimaryNodeType(field, "hipposysedit_1_1:field");
                    if(field.hasProperty("hipposysedit_1_1:name")) {
                        Property nameProperty = field.getProperty("hipposysedit_1_1:name");
                        if(field.getName().equals("hipposysedit:field")) {
                            context.setName(field, nameProperty.getString());
                        }
                        nameProperty.remove();
                    }
                    if(field.hasProperty("hipposysedit_1_1:mandatory")) {
                        field.getProperty("hipposysedit_1_1:mandatory").remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostdpubwf", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippostdpubwf.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostd:publishable") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                if(node.isNodeType("hippo:harddocument") && !node.isNodeType("hippostdpubwf:document")) {
                    node.addMixin("hippostdpubwf:document");
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposysedit", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposysedit.cnd"))));
    }
}
