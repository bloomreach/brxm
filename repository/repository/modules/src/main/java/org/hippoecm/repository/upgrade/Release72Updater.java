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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.util.TraversingItemVisitor;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Release72Updater implements UpdaterModule {
    private static final String[][] rules = {
        {"type", "hippo:remodel", "hipposysedit_1_0:remodel"},
        {"field", "hippo:uri", "hipposysedit_1_0:uri"},
        {"type", "hippo:field", "hipposysedit_1_0:field"},
        {"field", "hippo:name", "hipposysedit_1_0:name"},
        {"field", "hippo:path", "hipposysedit_1_0:path"},
        {"field", "hippo:type", "hipposysedit_1_0:type"},
        {"field", "hippo:multiple", "hipposysedit_1_0:multiple"},
        {"field", "hippo:mandatory", "hipposysedit_1_0:mandatory"},
        {"field", "hippo:ordered", "hipposysedit_1_0:ordered"},
        {"field", "hippo:primary", "hipposysedit_1_0:primary"},
        {"type", "hippo:nodetype", "hipposysedit_1_0:nodetype"},
        {"field", "hippo:type", "hipposysedit_1_0:type "},
        {"field", "hippo:supertype", "hipposysedit_1_0:supertype"},
        {"field", "hippo:node", "hipposysedit_1_0:node"},
        {"field", "hippo:mixin", "hipposysedit_1_0:mixin"},
        {"child", "hippo:field", "hipposysedit_1_0:field"},
        {"type", "hippo:templatetype", "hipposysedit_1_0:templatetype"},
        {"type", "hippo:initializeitem", "hippo_2_0:initializeitem"},
        {"type", "hippo:softdocument", "hipposys_1_0:softdocument"},
        {"field", "hippo:uuid", "hipposys_1_0:uuid"},
        {"type", "hippo:request", "hipposys_1_0:request"},
        {"type", "hippo:implementation", "hipposys_1_0:implementation"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:serialver", "hipposys_1_0:serialver"},
        {"type", "hippo:type", "hipposys_1_0:type"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"type", "hippo:types", "hipposys_1_0:types"},
        {"type", "hippo:workflow", "hipposys_1_0:workflow"},
        {"field", "hippo:workflow", "hipposys_1_0:classname"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"child", "hippo:config", "hipposys_1_0:config"},
        {"type", "hippo:workflowcategory", "hipposys_1_0:workflowcategory"},
        {"type", "hippo:workflowfolder", "hipposys_1_0:workflowfolder"},
        {"type", "hippo:ocmquery", "hipposys_1_0:ocmquery"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"type", "hippo:ocmqueryfolder", "hipposys_1_0:ocmqueryfolder"},
        {"type", "hippo:queryfolder", "hipposys_1_0:queryfolder"},
        {"type", "hippo:basequeryfolder", "hipposys_1_0:basequeryfolder"},
        {"type", "hippo:propertyreference", "hipposys_1_0:propertyreference"},
        {"type", "hippo:relativepropertyreference", "hipposys_1_0:relativepropertyreference"},
        {"field", "hippo:relPath", "hipposys_1_0:relPath"},
        {"type", "hippo:resolvepropertyreference", "hipposys_1_0:resolvepropertyreference"},
        {"field", "hippo:relPath", "hipposys_1_0:relPath"},
        {"type", "hippo:builtinpropertyreference", "hipposys_1_0:builtinpropertyreference"},
        {"field", "hippo:method", "hipposys_1_0:method"},
        {"type", "hippo:propertyreferences", "hipposys_1_0:propertyreferences"},
        {"type", "hippo:deriveddefinition", "hipposys_1_0:deriveddefinition"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:serialver", "hipposys_1_0:serialver"},
        {"child", "hippo:accessed", "hipposys_1_0:accessed"},
        {"child", "hippo:derived", "hipposys_1_0:derived"},
        {"type", "hippo:derivativesfolder", "hipposys_1_0:derivativesfolder"},
        {"type", "hippo:initializefolder", "hippo_2_0:initializefolder"},
        {"type", "hippo:temporaryfolder", "hipposys_1_0:temporaryfolder"},
        {"type", "hippo:applicationfolder", "hipposys_1_0:applicationfolder"},
        {"type", "hippo:configuration", "hipposys_1_0:configuration"},
        {"type", "hippo:accessmanager", "hipposys_1_0:accessmanager"},
        {"field", "hippo:permissioncachesize", "hipposys_1_0:permissioncachesize"},
        {"type", "hippo:user", "hipposys_1_0:user"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:active", "hipposys_1_0:active"},
        {"field", "hippo:password", "hipposys_1_0:password"},
        {"field", "hippo:passkey", "hipposys_1_0:passkey"},
        {"field", "hippo:lastlogin", "hipposys_1_0:lastlogin"},
        {"type", "hippo:externaluser", "hipposys_1_0:externaluser"},
        {"field", "hippo:lastsync", "hipposys_1_0:lastsync"},
        {"type", "hippo:group", "hipposys_1_0:group"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:members", "hipposys_1_0:members"},
        {"field", "hippo:groups", "hipposys_1_0:groups"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:externalgroup", "hipposys_1_0:externalgroup"},
        {"field", "hippo:syncdate", "hipposys_1_0:syncdate"},
        {"type", "hippo:role", "hipposys_1_0:role"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"field", "hippo:roles", "hipposys_1_0:roles"},
        {"field", "hippo:jcrread", "hipposys_1_0:jcrread"},
        {"field", "hippo:jcrwrite", "hipposys_1_0:jcrwrite"},
        {"field", "hippo:jcrremove", "hipposys_1_0:jcrremove"},
        {"type", "hippo:externalrole", "hipposys_1_0:externalrole"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"type", "hippo:authrole", "hipposys_1_0:authrole"},
        {"field", "hippo:users", "hipposys_1_0:users"},
        {"field", "hippo:groups", "hipposys_1_0:groups"},
        {"field", "hippo:role", "hipposys_1_0:role"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:facetrule", "hipposys_1_0:facetrule"},
        {"field", "hippo:facet", "hipposys_1_0:facet"},
        {"field", "hippo:value", "hipposys_1_0:value"},
        {"field", "hippo:type", "hipposys_1_0:type"},
        {"field", "hippo:equals", "hipposys_1_0:equals"},
        {"field", "hippo:filter", "hipposys_1_0:filter"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:domainrule", "hipposys_1_0:domainrule"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:domain", "hipposys_1_0:domain"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:userprovider", "hipposys_1_0:userprovider"},
        {"field", "hippo:dirlevels", "hipposys_1_0:dirlevels"},
        {"type", "hippo:groupprovider", "hipposys_1_0:groupprovider"},
        {"field", "hippo:dirlevels", "hipposys_1_0:dirlevels"},
        {"type", "hippo:roleprovider", "hipposys_1_0:roleprovider"},
        {"type", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:userprovider", "hipposys_1_0:userprovider"},
        {"child", "hippo:groupprovider", "hipposys_1_0:groupprovider"},
        {"child", "hippo:roleprovider", "hipposys_1_0:roleprovider"},
        {"type", "hippo:userfolder", "hipposys_1_0:userfolder"},
        {"type", "hippo:groupfolder", "hipposys_1_0:groupfolder"},
        {"type", "hippo:rolefolder", "hipposys_1_0:rolefolder"},
        {"type", "hippo:domainfolder", "hipposys_1_0:domainfolder"},
        {"type", "hippo:securityfolder", "hipposys_1_0:securityfolder"},
        {"field", "hippo:userspath", "hipposys_1_0:userspath"},
        {"field", "hippo:groupspath", "hipposys_1_0:groupspath"},
        {"field", "hippo:rolespath", "hipposys_1_0:rolespath"},
        {"field", "hippo:domainspath", "hipposys_1_0:domainspath"},
        {"child", "hippo:accessmanager", "hipposys_1_0:accessmanager"},
        {"type", "hippo:namespace", "hipposysedit_1_0:namespace"},
        {"type", "hippo:namespacefolder", "hipposysedit_1_0:namespacefolder"},
        {"type", "hippo:resource", "hippo_2_0:resource"},
        {"type", "hippo:query", "hippo_2_0:query"},
        {"type", "hippo:derived", "hippo_2_0:derived"},
        {"type", "hippo:document", "hippo_2_0:document"},
        {"type", "hippo:handle", "hippo_2_0:handle"},
        {"type", "hippo:hardhandle", "hippo_2_0:hardhandle"},
        {"type", "hippo:harddocument", "hippo_2_0:harddocument"},
        {"type", "hippo:facetresult", "hippo_2_0:facetresult"},
        {"type", "hippo:facetbasesearch", "hippo_2_0:facetbasesearch"},
        {"type", "hippo:facetsearch", "hippo_2_0:facetsearch"},
        {"type", "hippo:facetselect", "hippo_2_0:facetselect"},
        {"type", "hippo:mirror", "hippo_2_0:mirror"},
        //{"type","hippo:translation","hipponew:translation"},
        //{"type","hippo:translated","hipponew:translated"},

        {"type", "frontend:workflow", "frontend_2_0:workflow"},
        {"field", "hippo:workflow", "hipposys_1_0:classname"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"child", "hippo:config", "hipposys_1_0:config"},
        //{"type", "frontend:user", "frontend:user2"},
        //{"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        //{"field", "hippo:active", "hipposys_1_0:active"},
        //{"field", "hippo:password", "hipposys_1_0:password"},
        //{"field", "hippo:passkey", "hipposys_1_0:passkey"},
        //{"field", "hippo:lastlogin", "hipposys_1_0:lastlogin"},

        {"type", "hippo:implementation", "hipposys_1_0:implementation"},
        {"field", "hippo:classname", "hipposys_1_0:classname"}
    };

    public void register(final UpdaterContext context) {
        context.registerName("upgrade");
        context.registerStartTag("m13");
        context.registerEndTag("tag209");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                node.getNode("content").remove();
                node.getNode("hippo:log").remove();
                node.getNode("hippo:namespaces").remove();
                node.getNode("live").remove();
                node.getNode("preview").remove();
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:configuration") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                for (String[] delete : new String[][] {
                            {"hippo:derivatives", "hippo:corederivatives"},
                            {"hippo:temporary"},
                            {"hippo:documents", "embedded", "root"},
                            {"hippo:queries"},
                            {"hippo:workflows"},
                            {"hippo:initialize"},
                            {"hippo:frontend"},
                            {"hippo:roles", "admin"},
                            {"hippo:groups", "admin"},
                            {"hippo:users", "admin", "workflowuser"}
                        }) {
                    for (NodeIterator it = node.getNode(delete[0]).getNodes(); it.hasNext();) {
                        Node child = it.nextNode();
                        boolean keep = false;
                        for (int i = 1; i < delete.length; i++) {
                            if (child.getName().equals(delete[i]))
                                keep = true;
                        }
                        if (!keep)
                            child.remove();
                    }
                }
                node.accept(new TraversingItemVisitor.Default(true) {
                    @Override
                    public void entering(final Node node, int level) throws RepositoryException {
                        convert(node, context);
                    }
                });
            }
        });
        try {
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposys", "hipposys.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposys.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposysedit", "hipposysedit.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposysedit.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippo", "hippo.cnd", new InputStreamReader(Class.forName("org.hippoecm.repository.LocalHippoRepository").getResourceAsStream("repository.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostd", "hippostd.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippostd.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippogallery", "hippogallery.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippogallery.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "frontend", "frontend.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("frontend.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippolog", "hippolog.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippolog.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "reporting", "reporting.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("reporting.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippohtmlcleaner", "hippohtmlcleaner.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippohtmlcleaner.cnd"))));
            context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "defaultcontent", "defaultcontent.cnd", new InputStreamReader(getClass().getClassLoader().getResourceAsStream("defaultcontent.cnd"))));
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
    }

    private void convert(Node node, UpdaterContext context) throws RepositoryException {
        for (int i = 0; i < rules.length; i++) {
            String[] rule = rules[i];
            if ("type".equals(rule[0])) {
                boolean typeMatch = false;
                int typeMatchIndex = 0;
                for (NodeType nodeType : context.getNodeTypes(node)) {
                    if (nodeType.getName().equals(rule[1])) {
                        typeMatch = true;
                        if (typeMatchIndex == 0) {
                            context.setPrimaryNodeType(node, rule[2]);
                        } else {
                            node.removeMixin(rule[1]);
                            node.addMixin(rule[2]);
                        }
                    }
                    ++typeMatchIndex;
                }
                if (typeMatch) {
                    if (rule[2].equals("hipposysedit:templatetype")) {
                        for (NodeIterator iter = node.getNodes("hippo:nodetype"); iter.hasNext();) {
                            Node child = iter.nextNode();
                            for (NodeIterator documentsIter = child.getNodes(child.getName()); documentsIter.hasNext();) {
                                context.setName(documentsIter.nextNode(), "hipposysedit:nodetype");
                            }
                            context.setName(child, "hipposysedit:nodetype");
                            context.setPrimaryNodeType(child, "hipposysedit:handle");
                        }
                        for (NodeIterator iter = node.getNodes("hippo:template"); iter.hasNext();) {
                            Node child = iter.nextNode();
                            for (NodeIterator documentsIter = child.getNodes(child.getName()); documentsIter.hasNext();) {
                                context.setName(documentsIter.nextNode(), "hipposysedit:template");
                            }
                            context.setName(child, "hipposysedit:template");
                            context.setPrimaryNodeType(child, "hipposysedit:handle");
                        }
                        for (NodeIterator iter = node.getNodes("hippo:prototype"); iter.hasNext();) {
                            Node child = iter.nextNode();
                            for (NodeIterator documentsIter = child.getNodes(child.getName()); documentsIter.hasNext();) {
                                context.setName(documentsIter.nextNode(), "hipposysedit:prototype");
                            }
                            context.setName(child, "hipposysedit:prototype");
                            context.setPrimaryNodeType(child, "hipposysedit:handle");
                        }
                    }
                    int j;
                    for (j = i + 1; j < rules.length; j++) {
                        rule = rules[j];
                        if ("field".equals(rule[0])) {
                            if (node.hasProperty(rule[1])) {
                                if (context.isMultiple(node.getProperty(rule[1]))) {
                                    node.setProperty(rule[2], node.getProperty(rule[1]).getValues());
                                } else {
                                    if (rule[2].equals("hipposys:value")) {
                                        String value = node.getProperty(rule[1]).getString();
                                        for (int k = 0; k < rules.length; k++) {
                                            if (rules[k][0].equals("type") && rules[k][1].equals(value)) {
                                                value = rules[k][2];
                                            }
                                            node.setProperty(rule[2], value);
                                        }
                                    } else {
                                        node.setProperty(rule[2], node.getProperty(rule[1]).getValue());
                                    }
                                }
                                node.getProperty(rule[1]).remove();
                            }
                        } else if ("child".equals(rule[0])) {
                            for (NodeIterator iter = node.getNodes(rule[1]); iter.hasNext();) {
                                Node child = iter.nextNode();
                                context.setName(child, rule[2]);
                                if (rule.length > 3) {
                                    context.setPrimaryNodeType(child, rule[3]);
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    i = j - 1;
                } else {
                    int j;
                    for (j = i + 1; j < rules.length; j++) {
                        rule = rules[j];
                        if (!"field".equals(rule[0]) && !"child".equals(rule[0])) {
                            break;
                        }
                    }
                    i = j - 1;
                }
            } else if ("field".equals(rule[0])) {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            } else if ("child".equals(rule[0])) {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            } else {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            }
        }
    }
}
