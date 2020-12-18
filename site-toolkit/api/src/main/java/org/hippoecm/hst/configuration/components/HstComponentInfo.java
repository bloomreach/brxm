/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.components;

import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstURL;


/**
 * {@link HstComponentConfiguration}.
 * <P>
 * Basic information interface for component configuration.
 * </P>
 * 
 * @see HstComponentConfiguration
 * @version $Id$
 */
public interface HstComponentInfo {
    
    /**
     * Returns the id for this component configuration. 
     * The id must be unique within the container {@link HstComponentsConfiguration}, 
     * or <code>null</code> if it is not needed to be directly accessed by the
     * <code>HstComponentsConfiguration</code> through {@link HstComponentsConfiguration#getComponentConfiguration(String)}. 
     * Every <code>HstComponentConfiguration</code> that can be referred to from within a 
     * {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} must have an id.
     * 
     * @return the id of this component configuration or <code>null</code> if no id set
     */
    String getId();
    
    /**
     * Return the name of this component configuration. It <strong>must</strong> be unique amongst siblings.
     * The value returned by this method, is the value that must be used in rendering code (jsp/velocity/freemarker) to include the output
     * of a child <code>HstComponent</code> instance.
     * 
     * @return the logical name this component configuration, unique amongst its siblings
     */
    String getName();
    
    /**
     * @return the fully-qualified class name of the class implementing the {@link org.hippoecm.hst.core.component.HstComponent} interface
     */
    String getComponentClassName();

    /**
     * @return if configured, the fully-qualified class name of the interface representing <code>ParametersInfo</code>
     * for a component, and otherwise {@code null}.
     */
    String getParametersInfoClassName();

    /**
     * @return <code>true</code> when this {@link HstComponentConfiguration} is configured to be rendered standalone in case of {@link HstURL#COMPONENT_RENDERING_TYPE}
     */
    boolean isStandalone();

    /**
     * Rendering asynchronous is very useful for hst components that are uncacheable, depend on external services, or take long to render.
     * @return <code>true</code> when this {@link HstComponentConfiguration} is configured to be rendered asynchronous.
     */
    boolean isAsync();

    /**
     * Optional mode parameter to determine which technology should used for rendering asynchronous component. e.g., 'ajax', 'esi', etc.
     */
    String getAsyncMode();

    /**
     * @return <code>true</code> if rendering / resource requests can have their entire page http responses cached. Note that
     * a {@link HstComponentConfiguration} by default is cacheable unless configured not to be cacheable. A {@link HstComponentConfiguration}
     * is only cacheable if and only if <b>all</b> its descendant {@link HstComponentConfiguration}s for the request are cacheable : <b>Note</b>
     * explicitly for 'the request', thus {@link HstComponentConfiguration} that are {@link HstComponentConfiguration#isAsync()}
     * and its descendants can be uncacheable while an ancestor {@link HstComponentConfiguration} can stay cacheable
     */
    boolean isCompositeCacheable();

    /**
     * When the {@link HstResponseState HstComponentWindow#getResponseState()} of this {@link HstComponentInfo} is not flushed
     * by its parent rendering, a warning message about possible waste detection can be logged. If you want to
     * suppress this message, {@link #isSuppressWasteMessage()} should return {@code true}
     * @return {@code true} when possible waste messages about this component should be suppressed
     */
    boolean isSuppressWasteMessage();

    /**
     * @return the label if present and <code>null</code> otherwise
     */
    String getLabel();

    /**
     * @return the {@link HstComponentConfiguration.Type} of this component
     */
    HstComponentConfiguration.Type getComponentType();

    /**
     * when a component does have children which do not get flushed to the parent component, a waste message is logged
     * by default on WARN level. However, this can be logged for every request flooding the logs. Instead, better to
     * log it only once if the log level is WARN and only log it for every request if the log level is set to DEBUG
     *
     * Note that on the first get, the boolean is flipped to true : this method can be invoked concurrently and also
     * modifies a boolean in the shared underlying Hst Model
     */
    boolean getAndSetLogWasteMessageProcessed();
}
