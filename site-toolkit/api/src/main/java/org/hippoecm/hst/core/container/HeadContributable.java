/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Interface for abstraction that allows to contribute head elements to the page.
 * <P>
 * As one of typical use cases, a custom valve placed before <code>AggregationValve</code> may set an instance
 * of this to {@link HstRequestContext} and so the <code>AggregationValve</code> will be able to gather all the
 * head elements to contribute to the page.
 * </P>
 */
public interface HeadContributable {

    /**
     * Contribute head elements to the given {@code response}.
     * @param rootResponse the root {@link HstResponse} through which this can contribute head elements to the page.
     */
    void contributeHeadElements(HstResponse rootResponse);

}
