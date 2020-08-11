/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.visit;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.service.FacetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetVisitor extends NodeVisitor {

    public static final Logger log = LoggerFactory.getLogger(FacetVisitor.class);

    private final List<FacetTagProcessor> processors;
    private FacetService facetService;

    protected FacetVisitor(final Model<Node> nodeModel, final FacetTagProcessor... processors) {
        super(nodeModel);
        this.processors = Arrays.asList(processors);
    }

    @Override
    public void before() {
        facetService = new FacetService(getNode());
    }

    @Override
    public void after() {
        facetService.removeUnmarkedFacets();
        facetService = null;
    }

    @Override
    public void onRead(final Tag parent, final Tag tag) throws RepositoryException {
        if (facetService == null) {
            throw new NullPointerException("FacetService is null");
        }
        processors.forEach(tagProcessor -> tagProcessor.onRead(tag, facetService));
    }

    @Override
    public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {
        if (facetService == null) {
            throw new NullPointerException("FacetService is null");
        }
        processors.forEach(tagProcessor -> tagProcessor.onWrite(tag, facetService));
    }
}
