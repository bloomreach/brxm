/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.parameters;

/**
 * Provides a resolved ParametersInfo object for HstComponents or JAX-RS service components.
 */
public interface ParametersInfoProvider {

    /**
     * Returns a ParametersInfo object which resolves parameter from the underlying parameters stored in
     * <code>HstComponentConfiguration</code>, <code>HstSiteMapItem</code> or <code>Mount</code>, depending on
     * which 'component' is in the context.
     * <p>
     * For example, an <code>HstComponent</code> could get a ParametersInfo object of which the parameters are backed
     * by an <code>HstComponentConfiguration</code>. Or a JAX-RS service component could be a ParametersInfo object
     * of which the parameters are backed by either <code>HstSiteMapItem</code> or <code>Mount</code>
     * </p>
     * <p>
     * The parameter resolution means that possible property placeholders like ${1} or ${year}, where the
     * first refers to the first wildcard matcher in a resolved sitemap item, and the latter to a resolved parameter in
     * the resolved HstSiteMapItem.
     * </p>
     * <p>
     * <EM>NOTE: Because the returned ParametersInfo proxy instance is bound to the current request, you MUST NOT store
     * the returned object in a member variable or session. You should retrieve that per request.</EM> </P>
     * </p>
     * @return a ParametersInfo object which resolves parameter from the underlying parameters stored in
     * <code>HstComponentConfiguration</code>, <code>HstSiteMapItem</code> or <code>Mount</code>, depending on
     * which 'component' is in the context. Null if nothing is available.
     */
    <T> T getParametersInfo();

}
