/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.domain;

import javax.jcr.PathNotFoundException;

import org.apache.jackrabbit.spi.Name;

public class FacetRuleReferenceNotFoundException extends RuntimeException {

    private final Name facetName;
    private final boolean equals;

    public FacetRuleReferenceNotFoundException(final Name facetName, final boolean equals, final String message, final PathNotFoundException cause) {
        super(message, cause);
        this.facetName = facetName;
        this.equals = equals;
    }

    /**
     * @return the {@link Name} of the {@link QFacetRule} that triggered the exception
     */
    public Name getFacetName() {
        return facetName;
    }

    /**
     * @return whether the {@link QFacetRule} that triggered the exception has hipposys:equals <code>true</code> or
     * <code>false</code>
     */
    public boolean isEquals() {
        return equals;
    }
}
