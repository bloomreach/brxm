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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.behavioral.BehavioralNodeTypes;


public class BehavioralSearchDataProvider extends AbstractDataProvider {
    
    private static final String QUERY_PARAMETER_PROPERTY_NAME = BehavioralNodeTypes.BEHAVIORAL_PROVIDER_QUERY_PARAMETER_PROPERTY;
    private static final String DEFAULT_QUERY_PARAMETER = "query";
    
    private String queryParameter = DEFAULT_QUERY_PARAMETER;
    
    public BehavioralSearchDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
        if (node.hasProperty(QUERY_PARAMETER_PROPERTY_NAME)) {
            Property prop = node.getProperty(QUERY_PARAMETER_PROPERTY_NAME);
            if(prop.getType() == PropertyType.STRING) {
                queryParameter = prop.getString();
            }
        }
    }

    @Override
    protected List<String> extractTerms(HttpServletRequest request) {
        final String query = request.getParameter(queryParameter);
        
        if(query == null) {
            return null;
        }
        List<String> terms = new ArrayList<String>();
        String[] queryTerms = query.split("\\s");
        for (String term : queryTerms) {
            terms.add(term);
        }
        return terms;
    }
    
}
