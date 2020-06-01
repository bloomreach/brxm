/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.taxonomy.api.Taxonomies;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomiesImpl implements Taxonomies {

    static Logger log = LoggerFactory.getLogger(TaxonomiesImpl.class);

    private Map<String, Taxonomy> taxonomies = new HashMap<>();

    public TaxonomiesImpl(Node taxonomiesNode) {
        NodeIterator nodes;
        try {
            nodes = taxonomiesNode.getNodes();
            while (nodes.hasNext()) {
                Node handle = nodes.nextNode();
                if (handle != null) {
                    if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                        // build taxonomy from first child node of correct type
                        for (NodeIterator children = handle.getNodes(handle.getName()); children.hasNext();) {
                            Node child = children.nextNode();
                            if (child != null) {
                                try {
                                    if (child.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY)) {
                                        TaxonomyImpl taxonomy = new TaxonomyImpl(child);
                                        // when finished with taxonomy, detach the valueproviders + childs
                                        taxonomy.closeValueProvider(true);
                                        this.taxonomies.put(taxonomy.getName(), taxonomy);
                                        break;
                                    }
                                } catch (TaxonomyException | RepositoryException e) {
                                    log.error("Error while creating taxonomy below handle " +  handle.getPath(), e);
                                }
                            }
                        }
                    } else {
                        log.warn("Skipping node {} that is not of type '{}'", handle.getPath(), HippoNodeType.NT_HANDLE);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while creating taxonomies", e);
        }
    }

    public List<Taxonomy> getRootTaxonomies() {
        return Collections.unmodifiableList(new ArrayList<>(taxonomies.values()));
    }

    public Taxonomy getTaxonomy(String name) {
        return taxonomies.get(name);
    }

}
