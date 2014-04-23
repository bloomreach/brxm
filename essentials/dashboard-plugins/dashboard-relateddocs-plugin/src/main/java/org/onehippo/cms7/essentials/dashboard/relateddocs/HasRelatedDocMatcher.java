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

package org.onehippo.cms7.essentials.dashboard.relateddocs;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;

/**
 * @version "$Id$"
 */
public class HasRelatedDocMatcher implements JcrMatcher {

    public static final String PLUGIN_CLASS = "plugin.class";

    @Override
    public boolean matches(final Node typeNode) throws RepositoryException {
        if (typeNode.getName().equals("hipposysedit:prototype")) {
            final Node parent = typeNode.getParent().getParent();
            int i = 0;
            if (parent.hasNode("editor:templates/_default_")) {
                final Node template = parent.getNode("editor:templates/_default_");
                final NodeIterator it = template.getNodes();
                while (it.hasNext()) {
                    final Node node = it.nextNode();
                    if (node.hasProperty(PLUGIN_CLASS)
                        && (node.getProperty(PLUGIN_CLASS).getString().equals("org.onehippo.forge.relateddocs.editor.RelatedDocsPlugin")
                                ||
                                node.getProperty(PLUGIN_CLASS).getString().equals("org.onehippo.forge.relateddocs.editor.RelatedDocsSuggestPlugin")
                                )) {
                            i++;

                    }
                }
            }
            if (i == 2) {
                return true;
            }
        }
        return false;
    }
}
