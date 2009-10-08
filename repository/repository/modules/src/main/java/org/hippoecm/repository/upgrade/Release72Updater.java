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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Release72Updater implements UpdaterModule {
    private static final String[][] rules = {
        {"type", "hippo:remodel", "hipposysedit:remodel"},
        {"field", "hippo:uri", "hipposysedit:uri"},
        {"type", "hippo:field", "hipposysedit:field"},
        {"field", "hippo:name", "hipposysedit:name"},
        {"field", "hippo:path", "hipposysedit:path"},
        {"field", "hippo:type", "hipposysedit:type"},
        {"field", "hippo:multiple", "hipposysedit:multiple"},
        {"field", "hippo:mandatory", "hipposysedit:mandatory"},
        {"field", "hippo:ordered", "hipposysedit:ordered"},
        {"field", "hippo:primary", "hipposysedit:primary"},
        {"type", "hippo:nodetype", "hipposysedit:nodetype"},
        {"field", "hippo:type", "hipposysedit:type "},
        {"field", "hippo:supertype", "hipposysedit:supertype"},
        {"field", "hippo:node", "hipposysedit:node"},
        {"field", "hippo:mixin", "hipposysedit:mixin"},
        {"child", "hippo:field", "hipposysedit:field"},
        {"type", "hippo:templatetype", "hipposysedit:templatetype"},
        {"type", "hippo:initializeitem", "hipponew:initializeitem"},
        {"type", "hippo:softdocument", "hipposys:softdocument"},
        {"field", "hippo:uuid", "hipposys:uuid"},
        {"type", "hippo:request", "hipposys:request"},
        {"type", "hippo:implementation", "hipposys:implementation"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"field", "hippo:serialver", "hipposys:serialver"},
        {"type", "hippo:type", "hipposys:type"},
        {"field", "hippo:nodetype", "hipposys:nodetype"},
        {"field", "hippo:display", "hipposys:display"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"type", "hippo:types", "hipposys:types"},
        {"type", "hippo:workflow", "hipposys:workflow"},
        {"field", "hippo:workflow", "hipposys:classname"},
        {"field", "hippo:privileges", "hipposys:privileges"},
        {"field", "hippo:nodetype", "hipposys:nodetype"},
        {"field", "hippo:display", "hipposys:display"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"child", "hippo:types", "hipposys:types"},
        {"child", "hippo:config", "hipposys:config"},
        {"type", "hippo:workflowcategory", "hipposys:workflowcategory"},
        {"type", "hippo:workflowfolder", "hipposys:workflowfolder"},
        {"type", "hippo:ocmquery", "hipposys:ocmquery"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"child", "hippo:types", "hipposys:types"},
        {"type", "hippo:ocmqueryfolder", "hipposys:ocmqueryfolder"},
        {"type", "hippo:queryfolder", "hipposys:queryfolder"},
        {"type", "hippo:basequeryfolder", "hipposys:basequeryfolder"},
        {"type", "hippo:propertyreference", "hipposys:propertyreference"},
        {"type", "hippo:relativepropertyreference", "hipposys:relativepropertyreference"},
        {"field", "hippo:relPath", "hipposys:relPath"},
        {"type", "hippo:resolvepropertyreference", "hipposys:resolvepropertyreference"},
        {"field", "hippo:relPath", "hipposys:relPath"},
        {"type", "hippo:builtinpropertyreference", "hipposys:builtinpropertyreference"},
        {"field", "hippo:method", "hipposys:method"},
        {"type", "hippo:propertyreferences", "hipposys:propertyreferences"},
        {"type", "hippo:deriveddefinition", "hipposys:deriveddefinition"},
        {"field", "hippo:nodetype", "hipposys:nodetype"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"field", "hippo:serialver", "hipposys:serialver"},
        {"child", "hippo:accessed", "hipposys:accessed"},
        {"child", "hippo:derived", "hipposys:derived"},
        {"type", "hippo:derivativesfolder", "hipposys:derivativesfolder"},
        {"type", "hippo:initializefolder", "hipponew:initializefolder"},
        {"type", "hippo:temporaryfolder", "hipposys:temporaryfolder"},
        {"type", "hippo:applicationfolder", "hipposys:applicationfolder"},
        {"type", "hippo:configuration", "hipposys:configuration"},
        {"type", "hippo:accessmanager", "hipposys:accessmanager"},
        {"field", "hippo:permissioncachesize", "hipposys:permissioncachesize"},
        {"type", "hippo:user", "hipposys:user"},
        {"field", "hippo:securityprovider", "hipposys:securityprovider"},
        {"field", "hippo:active", "hipposys:active"},
        {"field", "hippo:password", "hipposys:password"},
        {"field", "hippo:passkey", "hipposys:passkey"},
        {"field", "hippo:lastlogin", "hipposys:lastlogin"},
        {"type", "hippo:externaluser", "hipposys:externaluser"},
        {"field", "hippo:lastsync", "hipposys:lastsync"},
        {"type", "hippo:group", "hipposys:group"},
        {"field", "hippo:securityprovider", "hipposys:securityprovider"},
        {"field", "hippo:members", "hipposys:members"},
        {"field", "hippo:groups", "hipposys:groups"},
        {"field", "hippo:description", "hipposys:description"},
        {"type", "hippo:externalgroup", "hipposys:externalgroup"},
        {"field", "hippo:syncdate", "hipposys:syncdate"},
        {"type", "hippo:role", "hipposys:role"},
        {"field", "hippo:privileges", "hipposys:privileges"},
        {"field", "hippo:roles", "hipposys:roles"},
        {"field", "hippo:jcrread", "hipposys:jcrread"},
        {"field", "hippo:jcrwrite", "hipposys:jcrwrite"},
        {"field", "hippo:jcrremove", "hipposys:jcrremove"},
        {"type", "hippo:externalrole", "hipposys:externalrole"},
        {"field", "hippo:securityprovider", "hipposys:securityprovider"},
        {"type", "hippo:authrole", "hipposys:authrole"},
        {"field", "hippo:users", "hipposys:users"},
        {"field", "hippo:groups", "hipposys:groups"},
        {"field", "hippo:role", "hipposys:role"},
        {"field", "hippo:description", "hipposys:description"},
        {"type", "hippo:facetrule", "hipposys:facetrule"},
        {"field", "hippo:facet", "hipposys:facet"},
        {"field", "hippo:value", "hipposys:value"},
        {"field", "hippo:type", "hipposys:type"},
        {"field", "hippo:equals", "hipposys:equals"},
        {"field", "hippo:filter", "hipposys:filter"},
        {"field", "hippo:description", "hipposys:description"},
        {"type", "hippo:domainrule", "hipposys:domainrule"},
        {"field", "hippo:description", "hipposys:description"},
        {"type", "hippo:domain", "hipposys:domain"},
        {"field", "hippo:description", "hipposys:description"},
        {"type", "hippo:userprovider", "hipposys:userprovider"},
        {"field", "hippo:dirlevels", "hipposys:dirlevels"},
        {"type", "hippo:groupprovider", "hipposys:groupprovider"},
        {"field", "hippo:dirlevels", "hipposys:dirlevels"},
        {"type", "hippo:roleprovider", "hipposys:roleprovider"},
        {"type", "hippo:securityprovider", "hipposys:securityprovider"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"child", "hippo:userprovider", "hipposys:userprovider"},
        {"child", "hippo:groupprovider", "hipposys:groupprovider"},
        {"child", "hippo:roleprovider", "hipposys:roleprovider"},
        {"type", "hippo:userfolder", "hipposys:userfolder"},
        {"type", "hippo:groupfolder", "hipposys:groupfolder"},
        {"type", "hippo:rolefolder", "hipposys:rolefolder"},
        {"type", "hippo:domainfolder", "hipposys:domainfolder"},
        {"type", "hippo:securityfolder", "hipposys:securityfolder"},
        {"field", "hippo:userspath", "hipposys:userspath"},
        {"field", "hippo:groupspath", "hipposys:groupspath"},
        {"field", "hippo:rolespath", "hipposys:rolespath"},
        {"field", "hippo:domainspath", "hipposys:domainspath"},
        {"child", "hippo:accessmanager", "hipposys:accessmanager"},
        {"type", "hippo:namespace", "hipposysedit:namespace"},
        {"type", "hippo:namespacefolder", "hipposysedit:namespacefolder"},
        {"type", "hippo:resource", "hipponew:resource"},
        {"type", "hippo:query", "hipponew:query"},
        {"type", "hippo:derived", "hipponew:derived"},
        {"type", "hippo:document", "hipponew:document"},
        {"type", "hippo:handle", "hipponew:handle"},
        {"type", "hippo:hardhandle", "hipponew:hardhandle"},
        {"type", "hippo:harddocument", "hipponew:harddocument"},
        {"type", "hippo:facetresult", "hipponew:facetresult"},
        {"type", "hippo:facetbasesearch", "hipponew:facetbasesearch"},
        {"type", "hippo:facetsearch", "hipponew:facetsearch"},
        {"type", "hippo:facetselect", "hipponew:facetselect"},
        {"type", "hippo:mirror", "hipponew:mirror"},
        //{"type","hippo:translation","hipponew:translation"},
        //{"type","hippo:translated","hipponew:translated"},

        {"type", "frontend:workflow", "frontend:workflow2"},
        {"field", "hippo:workflow", "hipposys:classname"},
        {"field", "hippo:nodetype", "hipposys:nodetype"},
        {"field", "hippo:display", "hipposys:display"},
        {"field", "hippo:classname", "hipposys:classname"},
        {"field", "hippo:privileges", "hipposys:privileges"},
        {"child", "hippo:types", "hipposys:types"},
        {"child", "hippo:config", "hipposys:config"},
        {"type", "frontend:user", "frontend:user2"},
        {"field", "hippo:securityprovider", "hipposys:securityprovider"},
        {"field", "hippo:active", "hipposys:active"},
        {"field", "hippo:password", "hipposys:password"},
        {"field", "hippo:passkey", "hipposys:passkey"},
        {"field", "hippo:lastlogin", "hipposys:lastlogin"}
    };

    public void register(final UpdaterContext context) {
        context.registerStartTag("m13");
        context.registerEndTag("v72");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:configuration") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                node.accept(new TraversingItemVisitor.Default(true) {
                    @Override
                    public void entering(final Node node, int level) throws RepositoryException {
                        convert(node, context);
                    }
                });
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippo", /* "http://www.hippoecm.org/nt/1.3", "http://www.onehippo.org/jcr/hippo/nt/2.0", */ "hippo.cnd", null));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostd", /* "http://www.hippoecm.org/hippostd/nt/1.3", "http://www.onehippo.org/jcr/hippostd/nt/2.0", */ "hippostd.cnd", null));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippogallery", /* "http://www.hippoecm.org/hippogallery/nt/1.3", "http://www.onehippo.org/jcr/hippogallery/nt/2.0", */ "hippogallery.cnd", null));
    }

    private void convert(Node node, UpdaterContext context) throws RepositoryException {
        for (int i = 0; i < rules.length; i++) {
            String[] rule = rules[i];
            if ("type".equals(rule[0])) {
                if (node.getProperty("jcr:primaryType").getString().equals(rule[1])) {
                    context.setPrimaryNodeType(node, rule[2]);
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
