/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * <p> Interface to implement as a developer to add custom {@link HstComponentWindow} filtering for some requestContext.
 * You can decorate the <code>window</code> in {@link #doFilter(HstRequestContext, HstComponentConfiguration,
 * HstComponentWindow)} and return a decorated / enhanced version, or, return <code>null</code> when the
 * <code>window</code> should be completely removed. The root {@link HstComponentWindow} is not decorated through this
 * {@link #doFilter(HstRequestContext, HstComponentConfiguration, HstComponentWindow)} method. </p> <p> The {@link
 * #doFilter(HstRequestContext, HstComponentConfiguration, HstComponentWindow)} is called on every <b>non root</b>
 * {@link HstComponentWindow} <b>after</b> all {@link HstComponentWindow}s for the current {@link HstRequestContext}
 * have been created by {@link HstComponentWindowFactory#create(HstContainerConfig, HstRequestContext,
 * HstComponentConfiguration, HstComponentFactory, HstComponentWindow)} </p>
 * <p/>
 * When you want a <code>window</code> to be invisible (doBeforeRender and render skipped, but still processed as window
 * in an AggregationValve) you typically implement this interface and in {@link #doFilter(HstRequestContext,
 * HstComponentConfiguration, HstComponentWindow)} return the <code>window</code> after invoking {@link
 * HstComponentWindow#setVisible(boolean)} with argument <code>true</code>, thus HstComponentWindow#setVisible(true)
 * <p/>
 */
public interface HstComponentWindowFilter {

    /**
     * @param requestContext
     * @param compConfig     the {@link HstComponentConfiguration} from which <code>window</code> is created
     * @param window         The {@link HstComponentWindow} to decorate
     * @return A {@link HstComponentWindow} instance which can be an enhanced or decorated version of the
     *         <code>window</code>. If the <code>window</code> should be entirely disabled/skipped, <code>null</code>
     *         should be returned
     * @throws HstComponentException
     */
    HstComponentWindow doFilter(HstRequestContext requestContext,
                                HstComponentConfiguration compConfig,
                                HstComponentWindow window) throws HstComponentException;

}
