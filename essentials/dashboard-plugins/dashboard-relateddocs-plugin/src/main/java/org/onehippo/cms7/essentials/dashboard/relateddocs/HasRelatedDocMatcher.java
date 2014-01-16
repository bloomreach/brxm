package org.onehippo.cms7.essentials.dashboard.relateddocs;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;

/**
 * @version "$Id$"
 */
public class HasRelatedDocMatcher implements JcrMatcher {

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
                    if (node.hasProperty("plugin.class")) {
                        if (node.getProperty("plugin.class").getString().equals("org.onehippo.forge.relateddocs.editor.RelatedDocsPlugin")
                                ||
                                node.getProperty("plugin.class").getString().equals("org.onehippo.forge.relateddocs.editor.RelatedDocsSuggestPlugin")
                                ) {
                            i++;
                        }
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
