/*
 *  Copyright 2008 Hippo.
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

import java.util.Map;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.container.PageErrorHandler;

/**
 * A <code>HstComponentConfiguration</code> specifies a (Java) componentClassName implementing the {@link org.hippoecm.hst.core.component.HstComponent} 
 * interface to provide the actual behavior for content rendering and (inter)action processing.
 * <p/>
 * Furthermore, a <code>HstComponentConfiguration</code> can have child <code>HstComponentConfiguration</code> elements which are identified by a referenceName.
 * This <code>referenceName</code> can be used by the {@link org.hippoecm.hst.core.component.HstComponent} and its renderer to access its children 
 * request state and include their rendering output within its own rendering output. This referenceName is also use to build up a unique 
 * <code>referencePath</code> to identify a specific <code>HstComponent</code> within the whole tree of the <code>HstComponent</code> elements within 
 * the current request. It is up to the implementation to if this <code>referenceName</code> needs to be configured, or is auto-created. The 
 * constraint to the <code>referenceName</code> is that is MUST be unique for sibbling <code>HstComponentConfiguration</code>'s. 
 * Implementations auto-creating the <code>referenceName</code> can better use a deterministic algorithm to make sure that the same 
 * configuration results in the same auto-created <code>referenceName</code>'s, to avoid the need of sticky sessions in a clustered environment. 
 * Though, this is up to the implementation.
 * <p/>
 * The <code>referencePath</code> is derived automatically by prepending a <code>HstComponentConfiguration</code> its parent 
 * <code>referencePath</code> to the <code>referenceName</code>,  separated by a configurable character. 
 * As a <code>HstComponent</code> its referencePath is furthermore used to uniquely (prefix) request parameters which might end up
 * in an external URL, a <code>referenceName</code> is not allowed to contain any of the following characters (bracket not included): 
 * <code>[.,:/\'"?& |-+#$%~]</code>
 * <p/>
 * For a <code>root HstComponentConfiguration</code> however, the HstComponent referencePath will only contain the (root) ".", not 
 * its own referenceName which is simply ignored, and as such there is no restriction on which characters are used for 
 * it (or even that it has a value).
 * <p/>
 * A root <code>HstComponentConfiguration</code> on the other hand is required to have an id, uniquely identifying it among other 
 * root HstComponentConfiguration objects. This id is used for lookup from the {@link HstComponentsConfiguration} and as reference 
 * by a {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem}.
 * <p/>
 * A HstComponentConfiguration provides access to its children through a <code>LinkedHashMap<String, HstComponentConfiguration></code>, allowing 
 * the HST2 runtime to look them up by <code>referenceName</code> and/or <code>referencePath</code>, as well as process them in a sorted order. 
 * As the ordering in which <code>HstComponent</code> children are processed might be significant, the <code>HstComponentConfiguration</code> 
 * implementation is required to use a <code>LinkedHashMap</code> implementation (like a TreeMap) which returns the children in the order 
 * of the configuration/creation (not the "natural" ordering based only on the referenceName).
 * <p/>
 * A <code>HstComponentConfiguration</code> may define a <code>rendererPath</code> to a view renderer (resource) which is a 
 * web application  context relative (possibly servlet) path to be dispatched to during rendering, for example pointing to a JSP file, 
 * or maybe a script (like Velocity/Freemarker) to be "executed".
 * Note: to allow repository based/stored renderer scripts, a prefix might be used for indicating scripts that live in the repository. 
 * This is up to the implementation to provide.
 * <p/>
 * Finally, a <code>HstComponentConfiguration</code> may have additional parameters which are meaningful for its actual <code>HstComponent</code> 
 * implementation, which are provided as a simple map, accessible through {@link #getParameters()}. The actual <code>HstComponent</code> 
 * will access the <code>HstComponentConfiguration</code> through <code>
 * {@link org.hippoecm.hst.core.request.ComponentConfiguration#getParameter(String, org.hippoecm.hst.core.request.ResolvedSiteMapItem)}
 * </code> that can manipulate the parameter values. For example, the implementation can use some 'property placeholder' to be resolved 
 * for the request. Typically, an <code>HstComponent</code> might be interested in some parameters from the matched 
 * {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} item. Suppose the matched <code>HstSiteMapItem</code> has a parameter name 
 * <code>foo</code> with value <code>bar</code>, then a <code>HstComponentConfiguration</code> could have a parameter name called <code>lux</code>
 * with value <code>${foo}</code>. The runtime <code>HstComponent</code> fetching the parameter <code>lux</code> could then get the resolved
 * value <code>bar</code> returned from the matched {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem}. Obviously, the {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem}
 * could also have the value <code>${1}</code> where ${1} might first be substituted by the matched wildcard in the <code>HstSiteMapItem</code>.
 * Obviously, this is all up to the implementation whether to support this.
 *  
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstComponentConfiguration</code> instances but should not be able to modify them, 
 * implementations must make sure that through the api a <code>HstComponentConfiguration</code> instance cannot be changed. Returned List and Map
 * should be therefor unmodifiable.  
 * 
 */
public interface HstComponentConfiguration extends HstComponentInfo {
    
    /**
     * A {@link HstComponentConfiguration} comes in three different main types. The {@link Type} enum describes the possible
     * values. This {@link Type} is similar to the {@link HstComponentConfiguration#getComponentType()} which is the {@link String} representation
     */
     enum Type {
        COMPONENT,
        CONTAINER_COMPONENT,
        CONTAINER_ITEM_COMPONENT,
    }
    
    /**
     * Returns the parent <code>HstComponentConfiguration</code> for this this component or null if a root component.
     */
    HstComponentConfiguration getParent();

    /**
     * Return the referenceName of this <code>HstComponentConfiguration</code>. It <strong>must</strong> be unique amongst sibling <code>HstComponentConfiguration</code>'s.
     * The value returned by this method, is the value that will occur as part of the <code>referencePath</code> in request parameter names
     * 
     * @return the referenceName this HstComponentConfiguration, unique amongst its siblings
     */
    String getReferenceName();

    /**
     * @return the location of the view renderer
     */
    String getRenderPath();
    
    /**
     * @return return the servletpath of the servlet that must serve the resources for this <code>HstComponent</code>
     */
    String getServeResourcePath();
    
    /**
     * @return the fully classified className of the class implementing {@link PageErrorHandler} or <code>null</code> when not configured
     */
    String getPageErrorHandlerClassName();

    /**
     * Returns the parameter value for the parameter <code>name</code> and <code>null</code> if not present. Note that
     * from the <code>HstComponentConfiguration</code> always 'raw' parameters are returned. 'Raw' as in unresolved with respect
     * to property placeholders. So, a value might be ${year} or ${1}. In a {@link org.hippoecm.hst.core.component.HstComponent} 
     * instance, the implementation might have implemented some resolving for these values.
     * 
     * Parameters are inherited from ancestor configurations. Parameters that are configured in an ancestor override parameters
     * configured in this component. Ancestors have precedence. Note that this is opposite to {@link HstSiteMapItem#getParameter(String)}
     *  
     * @param name the name of the parameter
     * @return the configured parameter value for this <code>name</code> and <code>null</code> if not existing
     */
    String getParameter(String name);
    
    /**
     * Returns the parameter value <b>without inheritance</b> for the parameter <code>name</code> and <code>null</code> if not present.
     * It returns the parameters configured directly on this HstComponentConfiguration, without the merged parameters from parent
     * components (which have precedence, see {@link #getParameter(String)})
     *
     * @param name the name of the parameter
     * @return the configured parameter value for this <code>name</code> and <code>null</code> if not existing
     */
    String getLocalParameter(String name);
    
    /**
     * Returns the map of all parameters. Also see {@link #getParameter(String)}.
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configuration
     * 
     * Parameters are inherited from ancestor configurations. Parameters that are configured in an ancestor override parameters
     * configured in this component. Ancestors have precedence. Note that this is opposite to {@link HstSiteMapItem#getParameters()}
     * 
     * @return the map of all configured parameters, and an empty map if no parameters present
     */
    Map<String, String> getParameters();
    
    /**
     * see {@link #getParameter(String)}, but now only parameters directly present on the HstConfigurationItem are returned. Thus, 
     * no inheritance by parents involved
     * @return the map of all configured parameters, and an empty map if no parameters present
     */
    Map<String, String> getLocalParameters();
    
    /**
     * Implementations should return an unmodifiable linked map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configuration
     * @return all <code>HstComponentConfiguration</code> children in order they were added, and an empty Map if no children present
     */
     Map<String, HstComponentConfiguration> getChildren();

    /**
     * Returns the child HstComponentConfiguration by its name, or null if it doens't exist
     * @param name the name of the child HstComponentConfiguration
     */
    HstComponentConfiguration getChildByName(String name);
    
    /**
     * Returns the canonical (real physical) location of the stored configuration of this HstComponentConfiguration
     * @return the canonical location where the configuration is stored
     */
    String getCanonicalStoredLocation();


    /**
     * Returns the identifier of the backing stored component configuration. Note that multiple <code>HstComponentConfiguration</code>'s can share the same
     * canonical identifier due to inheritance. Also, multiple subsites can share the same backing configuration, and thus share the same canonical identifiers 
     * 
     * @return the identifier of the backing stored component configuration
     */
    String getCanonicalIdentifier();
    
    /**
     * @return the container type of this ComponentConfiguration and <code>null</code> if the component is not a container component
     */
    String getContainerType();
    
    /**
     * 
     * @return the {@link Type} of this component
     */
    Type getComponentType();
    
}
