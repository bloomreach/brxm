/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class DocumentTemplateUtils {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(":");
    private static Logger log = LoggerFactory.getLogger(DocumentTemplateUtils.class);

    public static final String HIPPOSYSEDIT_SUPERTYPE = "hipposysedit:supertype";

    /**
     * Adds mixin type to document template if one doesn't exists.
     *
     * @param context      plugin context
     * @param documentName name of the document it can be either {@code docname} or {@code projectprefix:docname}
     * @param mixinName    name of the mixin
     */
    public static void addMixinType(final PluginContext context, final String documentName, final String mixinName) throws RepositoryException {
        final String namespace = context.getProjectNamespacePrefix();
        final String document = documentName.indexOf(':') == -1 ? documentName : NAMESPACE_PATTERN.split(documentName)[1];

        final Session session = context.createSession();
        try {
            final String nodeTypePath = "/hippo:namespaces/" + namespace + '/' + document + "/hipposysedit:nodetype/hipposysedit:nodetype";
            final Node nodeTypeNode = session.getNode(nodeTypePath);
            final Property property = nodeTypeNode.getProperty(HIPPOSYSEDIT_SUPERTYPE);
            final Value[] myValues = property.getValues();
            final Set<String> newValueSet = prepareValues(myValues, mixinName);
            final String[] newValues = newValueSet.toArray(new String[newValueSet.size()]);
            property.setValue(newValues);
            // add mixin:
            ///hippo:namespaces/appstore/huh/hipposysedit:prototypes/hipposysedit:prototype
            final String sysEditPath = "/hippo:namespaces/" + namespace + '/' + document + "/hipposysedit:prototypes/hipposysedit:prototype";
            final Node node = session.getNode(sysEditPath);
            final NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
            boolean hasMixin = false;
            for (NodeType mixinNodeType : mixinNodeTypes) {
                if (mixinNodeType.getName().equals(mixinName)) {
                    hasMixin = true;
                    break;
                }
            }
            if(!hasMixin) {
                node.addMixin(mixinName);
            }

            session.save();
        } finally {
            GlobalUtils.cleanupSession(session);
        }


    }


    //############################################
    // UTILS
    //############################################




    private static Set<String> prepareValues(final Value[] values, final String value) throws RepositoryException {
        final Set<String> myValues = new HashSet<>();
        for (Value v : values) {
            myValues.add(v.getString());
        }
        if (myValues.contains(value)) {
            return myValues;
        }

        myValues.add(value);
        return myValues;
    }

}
