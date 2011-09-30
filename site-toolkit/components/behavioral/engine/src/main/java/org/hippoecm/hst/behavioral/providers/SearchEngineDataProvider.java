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
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchEngineDataProvider extends BehavioralRefererDataProvider {
    
    private static final Logger log = LoggerFactory.getLogger(SearchEngineDataProvider.class);
    
    public SearchEngineDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
    }

    @Override
    protected List<String> extractRefererTerms(String referer) {
        try {
            URI uri = new URI(referer);
            String query = uri.getQuery();
            String host = uri.getHost();
            String queryParameter = null;
            if (host.contains("google")) {
                queryParameter = "q";
            }
            else if (host.contains("yahoo")) {
                queryParameter = "p";
            }
            else if (host.contains("mail.ru")) {
                queryParameter = "q";
            }
            else if (host.contains("baidu")) {
                queryParameter = "wd";
            }
            else if (host.contains("zoek.nl")) {
                queryParameter = "q";
            }
            else {
                return null;
            }
            String[] parameters = query.split("&");
            for (String parameter : parameters) {
                int indexOfEquals = parameter.indexOf('=');
                if (indexOfEquals != -1) {
                    String parameterName = parameter.substring(0, indexOfEquals);
                    if (parameterName.equals(queryParameter)) {
                        String parameterValue = parameter.substring(indexOfEquals + 1);
                        String[] values = parameterValue.split("\\+");
                        return Arrays.asList(values);
                    }
                }
            }
        } catch (URISyntaxException e) {
            log.debug("referer not a URI: " + referer);
            return null;
        }
        return null;
    }

}
