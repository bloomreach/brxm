package org.onehippo.cms7.essentials.dashboard.taxonomy.model;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class TaxonomyModel extends Model<String> {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TaxonomyModel.class);

    private String name;
    private List<String> locales;

    public TaxonomyModel(Node node) {
        try {
            if (node.isNodeType("hippo:handle") && node.hasNodes()) {
                final NodeIterator nodes = node.getNodes();
                final Node taxonomyNode = nodes.nextNode();
                setName(taxonomyNode.getName());
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }

    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<String> getLocales() {
        return locales;
    }

    public void setLocales(final List<String> locales) {
        this.locales = locales;
    }
}
