/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.behavioral.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainRefererDataProvider extends BehavioralRefererDataProvider {

    private static final Logger log = LoggerFactory.getLogger(DomainRefererDataProvider.class);
    
    public DomainRefererDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
    }

    @Override
    protected List<String> extractRefererTerms(String referer) {
        try {
            URI uri = new URI(referer);
            List<String> terms = new ArrayList<String>(1);
            terms.add(uri.getHost());
            return terms;
        } catch (URISyntaxException e) {
            if (log.isDebugEnabled()) {
                log.debug("referer not a URI: " + referer);
            }
            return null;
        }
    }

}
