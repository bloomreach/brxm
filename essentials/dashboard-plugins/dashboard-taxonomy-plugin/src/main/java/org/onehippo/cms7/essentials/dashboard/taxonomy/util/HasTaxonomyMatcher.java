package org.onehippo.cms7.essentials.dashboard.taxonomy.util;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class HasTaxonomyMatcher implements JcrMatcher {

    private static Logger log = LoggerFactory.getLogger(HasTaxonomyMatcher.class);

    @Override
    public boolean matches(final Node typeNode) throws RepositoryException {
        if (typeNode.getName().equals("hipposysedit:prototype")) {
            final Node parent = typeNode.getParent().getParent();
            if (parent.hasNode("editor:templates/_default_")) {
                final Node template = parent.getNode("editor:templates/_default_");
                final NodeIterator it = template.getNodes();
                while (it.hasNext()) {
                    final Node node = it.nextNode();
                    if (node.hasProperty("plugin.class")) {
                        if (node.getProperty("plugin.class").getString().equals("org.onehippo.taxonomy.plugin.TaxonomyPickerPlugin")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
