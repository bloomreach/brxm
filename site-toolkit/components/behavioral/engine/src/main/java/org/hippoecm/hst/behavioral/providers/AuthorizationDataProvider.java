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
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

public class AuthorizationDataProvider extends AbstractDataProvider {

    public AuthorizationDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
        this.size = 1;
        this.weight = 1l;
    }

    @Override
    protected List<String> extractTerms(HttpServletRequest request) {
        List<String> terms = new ArrayList<String>(1);
        if (request.getUserPrincipal() != null) {
            terms.add("authorized");
        }
        else {
            terms.add("unauthorized");
        }
        return terms;
    }

}
