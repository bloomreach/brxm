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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.container.PageErrorHandler;

/**
 * A <code>HstComponentConfiguration</code> specifies a (Java) componentClassName implementing the {@link
 * org.hippoecm.hst.core.component.HstComponent} interface to provide the actual behavior for content rendering and
 * (inter)action processing.
 * <p/>
 * Furthermore, a <code>HstComponentConfiguration</code> can have child <code>HstComponentConfiguration</code> elements
 * which are identified by a referenceName. This <code>referenceName</code> can be used by the {@link
 * org.hippoecm.hst.core.component.HstComponent} and its renderer to access its children request state and include their
 * rendering output within its own rendering output. This referenceName is also use to build up a unique
 * <code>referencePath</code> to identify a specific <code>HstComponent</code> within the whole tree of the
 * <code>HstComponent</code> elements within the current request. It is up to the implementation to if this
 * <code>referenceName</code> needs to be configured, or is auto-created. The constraint to the
 * <code>referenceName</code> is that is MUST be unique for sibbling <code>HstComponentConfiguration</code>'s.
 * Implementations auto-creating the <code>referenceName</code> can better use a deterministic algorithm to make sure
 * that the same configuration results in the same auto-created <code>referenceName</code>'s, to avoid the need of
 * sticky sessions in a clustered environment. Though, this is up to the implementation.
 * <p/>
 * The <code>referencePath</code> is derived automatically by prepending a <code>HstComponentConfiguration</code> its
 * parent <code>referencePath</code> to the <code>referenceName</code>,  separated by a configurable character. As a
 * <code>HstComponent</code> its referencePath is furthermore used to uniquely (prefix) request parameters which might
 * end up in an external URL, a <code>referenceName</code> is not allowed to contain any of the following characters
 * (bracket not included): <code>[.,:/\'"?& |-+#$%~]</code>
 * <p/>
 * For a <code>root HstComponentConfiguration</code> however, the HstComponent referencePath will only contain the
 * (root) ".", not its own referenceName which is simply ignored, and as such there is no restriction on which
 * characters are used for it (or even that it has a value).
 * <p/>
 * A root <code>HstComponentConfiguration</code> on the other hand is required to have an id, uniquely identifying it
 * among other root HstComponentConfiguration objects. This id is used for lookup from the {@link
 * HstComponentsConfiguration} and as reference by a {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem}.
 * <p/>
 * A HstComponentConfiguration provides access to its children through a <code>LinkedHashMap<String,
 * HstComponentConfiguration></code>, allowing the HST2 runtime to look them up by <code>referenceName</code> and/or
 * <code>referencePath</code>, as well as process them in a sorted order. As the ordering in which
 * <code>HstComponent</code> children are processed might be significant, the <code>HstComponentConfiguration</code>
 * implementation is required to use a <code>LinkedHashMap</code> implementation (like a TreeMap) which returns the
 * children in the order of the configuration/creation (not the "natural" ordering based only on the referenceName).
 * <p/>
 * A <code>HstComponentConfiguration</code> may define a <code>rendererPath</code> to a view renderer (resource) which
 * is a web application  context relative (possibly servlet) path to be dispatched to during rendering, for example
 * pointing to a JSP file, or maybe a script (like Velocity/Freemarker) to be "executed". Note: to allow repository
 * based/stored renderer scripts, a prefix might be used for indicating scripts that live in the repository. This is up
 * to the implementation to provide.
 * <p/>
 * Finally, a <code>HstComponentConfiguration</code> may have additional parameters which are meaningful for its actual
 * <code>HstComponent</code> implementation, which are provided as a simple map, accessible through {@link
 * #getParameters()}. The actual <code>HstComponent</code> will access the <code>HstComponentConfiguration</code>
 * through <code> {@link org.hippoecm.hst.core.request.ComponentConfiguration#getParameter(String,
 * org.hippoecm.hst.core.request.ResolvedSiteMapItem)} </code> that can manipulate the parameter values. For example,
 * the implementation can use some 'property placeholder' to be resolved for the request. Typically, an
 * <code>HstComponent</code> might be interested in some parameters from the matched {@link
 * org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} item. Suppose the matched <code>HstSiteMapItem</code> has a
 * parameter name <code>foo</code> with value <code>bar</code>, then a <code>HstComponentConfiguration</code> could have
 * a parameter name called <code>lux</code> with value <code>${foo}</code>. The runtime <code>HstComponent</code>
 * fetching the parameter <code>lux</code> could then get the resolved value <code>bar</code> returned from the matched
 * {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem}. Obviously, the {@link
 * org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} could also have the value <code>${1}</code> where ${1} might
 * first be substituted by the matched wildcard in the <code>HstSiteMapItem</code>. Obviously, this is all up to the
 * implementation whether to support this.
 * <p/>
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstComponentConfiguration</code> instances but should not be
 * able to modify them, implementations must make sure that through the api a <code>HstComponentConfiguration</code>
 * instance cannot be changed. Returned List and Map should be therefore unmodifiable.
 */
public interface HstComponentConfiguration extends HstComponentInfo {

    /**
     * The delimiter that is used between the parametername and the parameterprefix when there is a prefix value
     */
    static final char PARAMETER_PREFIX_NAME_DELIMITER = '\uFFFF';

    /**
     * A {@link HstComponentConfiguration} comes in three different main types. The {@link Type} enum describes the
     * possible values. This {@link Type} is similar to the {@link HstComponentConfiguration#getComponentType()} which
     * is the {@link String} representation
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
     * Return the referenceName of this <code>HstComponentConfiguration</code>. It <strong>must</strong> be unique
     * amongst sibling <code>HstComponentConfiguration</code>'s. The value returned by this method, is the value that
     * will occur as part of the <code>referencePath</code> in request parameter names
     *
     * @return the referenceName this HstComponentConfiguration, unique amongst its siblings
     */
    String getReferenceName();

    /**
     * @return the location of the view renderer. Returns <code>null</code> when {@link #getNamedRenderer()} does not
     * return <code>null</code>
     * @see #getNamedRenderer()
     */
    String getRenderPath();

    /**
     * @return the name of the renderer, when using named servlet. Returns <code>null</code> when {@link
     * #getRenderPath()} does not return <code>null</code>
     * @see #getRenderPath()
     */
    String getNamedRenderer();

    /**
     * @return return the servletpath of the servlet that must serve the resources for this <code>HstComponent</code>
     */
    String getServeResourcePath();

    /**
     * @return the name of the resource server, when using named servlet. Returns <code>null</code> when {@link
     * #getServeResourcePath()} does not return <code>null</code>
     * @see #getServeResourcePath()
     */
    String getNamedResourceServer();

    /**
     * @return the fully classified className of the class implementing {@link PageErrorHandler} or <code>null</code>
     * when not configured
     */
    String getPageErrorHandlerClassName();

    /**
     * Returns the parameter value for the parameter <code>name</code> and <code>null</code> if not present. Note that
     * from the <code>HstComponentConfiguration</code> always 'raw' parameters are returned. 'Raw' as in unresolved with
     * respect to property placeholders. So, a value might be ${year} or ${1}. In a {@link
     * org.hippoecm.hst.core.component.HstComponent} instance, the implementation might have implemented some resolving
     * for these values.
     * <p/>
     * Parameters for components are inherited from ancestor configurations. In case the component configured a parameter
     * also present on an ancestor, the parameter from the ancestor is ignored
     *
     * @param name the name of the parameter
     * @return the configured parameter value for this <code>name</code> and <code>null</code> if not existing
     */
    String getParameter(String name);

    /**
     * Returns the parameter value <b>without inheritance</b> for the parameter <code>name</code> and <code>null</code>
     * if not present. It returns the parameters configured directly on this HstComponentConfiguration, without the
     * merged parameters from parent components (which have precedence, see {@link #getParameter(String)})
     *
     * @param name the name of the parameter
     * @return the configured parameter value for this <code>name</code> and <code>null</code> if not existing
     */
    String getLocalParameter(String name);

    /**
     * Returns the map of all parameters. Also see {@link #getParameter(String)}. Implementations should return an
     * unmodifiable map, for example {@link java.util.Collections#unmodifiableMap} to avoid client code changing
     * configuration
     * <p/>
     * Parameters are inherited from ancestor configurations. Parameters that are configured in an ancestor override
     * parameters configured in this component. Ancestors have precedence. Note that this is opposite to {@link
     * HstSiteMapItem#getParameters()}
     *
     * @return the map of all configured parameters, and an empty map if no parameters present
     */
    Map<String, String> getParameters();

    /**
     * Returns the list of all named and residual component parameters
     * @return the list of all named and residual component parameters, and an empty list if no parameters present
     */
    default List<DynamicParameter> getDynamicComponentParameters() {
        return Collections.emptyList();
    }

    /**
     * Returns the list of a component's field groups
     * @return
     */
    List<DynamicFieldGroup> getFieldGroups();

    /**
     * Returns a reference to a catalog item
     * @return a reference to a catalog item
     */
    String getComponentDefinition();

    /**
     * Returns an optional of the component parameter(named or residual) that has the specified name
     * @return an optional of the component parameter(named or residual) that has the specified name, and an empty optional if nothing found
     */
    default Optional<DynamicParameter> getDynamicComponentParameter(String name) {
        return Optional.empty();
    }
    
    /**
     * Parameters can have prefixes (variant). If there are prefixes in used, this method returns the (possibly
     * unmodifiable) {@link Set} of prefixes / variants in use. Only the prefixes/variants on the current component are
     * returned, thus not for descendant components like {@link #getVariants()} does.
     *
     * @return the {@link Set} of available prefixes for all available parameters and if no prefixes in use, returns an
     * Empty {@link Set}
     */
    Set<String> getParameterPrefixes();


    /**
     * This method returns {@link #getParameterPrefixes()} for the entire composite tree of descendant components
     * including the current component
     *
     * @return the List of all unique variants for this  {@link HstComponentConfiguration} <b>plus</b> all the variants
     * of all its descendant {@link HstComponentConfiguration}s. If no variants are present, and EMPTY List is
     * returned.
     */
    List<String> getVariants();

    /**
     * This method returns all {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration#getParameterPrefixes()}
     * for the entire {@link org.hippoecm.hst.configuration.hosting.Mount} combined
     *
     * @return the List of all unique variants for this  {@link org.hippoecm.hst.configuration.hosting.Mount}. If no
     * variants are present, an empty list is returned.
     *
     * For XPage document based {@link HstComponentConfiguration} instances, this returns all the 'mount variants' PLUS
     * possibly the variants for the XPage Document container items if they are present
     */
    List<String> getMountVariants();

    /**
     * see {@link #getParameter(String)}, but now only parameters directly present on the HstConfigurationItem are
     * returned. Thus, no inheritance by parents involved
     *
     * @return the map of all configured parameters, and an empty map if no parameters present
     */
    Map<String, String> getLocalParameters();

    /**
     * Implementations should return an unmodifiable linked map, for example {@link
     * java.util.Collections#unmodifiableMap} to avoid client code changing configuration
     *
     * @return all <code>HstComponentConfiguration</code> children in order they were added, and an empty Map if no
     * children present
     */
    Map<String, HstComponentConfiguration> getChildren();

    /**
     * Returns the child HstComponentConfiguration by its name, or null if it doens't exist
     *
     * @param name the name of the child HstComponentConfiguration
     */
    HstComponentConfiguration getChildByName(String name);

    /**
     * Returns the canonical (real physical) location of the stored configuration of this HstComponentConfiguration
     *
     * @return the canonical location where the configuration is stored
     */
    String getCanonicalStoredLocation();


    /**
     * Returns the identifier of the backing stored component configuration. Note that multiple
     * <code>HstComponentConfiguration</code>'s can share the same canonical identifier due to inheritance. Also,
     * multiple subsites can share the same backing configuration, and thus share the same canonical identifiers
     *
     * @return the identifier of the backing stored component configuration
     */
    String getCanonicalIdentifier();

    /**
     * @return the xtype of this ComponentConfiguration and <code>null</code> if the component does not have an xtype
     */
    String getXType();

    /**
     * Allows to 'map' a specific component (catalog item type) to their implementation logic.
     */
    String getCType();

    /**
     * The filter tag (see {@link org.hippoecm.hst.core.request.HstRequestContext#getComponentFilterTags}) for this
     * component.
     *
     * @return the filter tag of this component, or null if no tag is available.
     */
    String getComponentFilterTag();

    /**
     * @return <code>true</code> when the backing provider ({@link HstNode}) of this {@link HstComponentConfiguration}
     * is inherited, aka an {@link HstNode} belonging to a different hst:configuration tree than this
     * {@link HstComponentConfiguration}
     */
    boolean isInherited();


    /**
     * @return {@code true} when the backing provider ({@link HstNode}) of this {@link HstComponentConfiguration} is
     * most likely / can be shared with other {@link HstComponentConfiguration} instances
     */
    boolean isShared();

    /**
     * @return <code>true</code> when this {@link HstComponentConfiguration} can be used as a prototype to create other
     * {@link HstComponentConfiguration}s with
     */
    boolean isPrototype();

    /**
     * @return the icon path if present and <code>null</code> otherwise. The iconPath should be a path relative to the
     * site webapp
     */
    String getIconPath();

    /**
     * @return if {@link Calendar} time this component got changed for the last time or <code>null</code> if not
     * available
     */
    Calendar getLastModified();

    /**
     * @return the hst template property if available
     */
    String getHstTemplate();

    /**
     * <code>true</code> when this {@link HstComponentConfiguration} is marked as deleted. An {@link HstComponentConfiguration} that
     * has an ancestor that is marked as deleted will itself also be marked as deleted. A marked deleted component item will
     * be part of the HST model via {@link HstComponentsConfiguration#getComponentConfigurations()} but should be skipped
     * during execution.
     * @return <code>true</code> when this {@link HstComponentConfiguration} is marked as deleted.
     */
    boolean isMarkedDeleted();

    /**
     * <p>
     *     In case the component has a hippo:identifier, it is returned. Typically, an hst:containercomponent will have
     *     an autocreated hippo:identifier with as value a uuid: This can be used a stable identifier across different (versions) of
     *     the same node, for example for an HST Config branch it can have the same value (opposed to the uuid of the node)
     *     and across unpublished/published versions below a document variant the xpage containers can have a stable uuid
     * </p>
     * @return the hippo identifier if available for this {@link HstComponentConfiguration}, otherwise {@code null}
     */
    String getHippoIdentifier();

    /**
     * @return {@code true} if this component is part of a component STORED below an experience page document variant
     */
    boolean isExperiencePageComponent();

    /**
     * @return {@code true} if this is a container component AND is part of a request based XPage config but comes
     * from an XPage Layout since the XPage document does not have a representation of the container : in that case,
     * very specific Channel Mgr behavior and page-composer behavior is needed: when an item is added to the container,
     * the container should be CREATED in the XPage document
     *
     */
    boolean isUnresolvedXpageLayoutContainer();

    /**
     * {@code true} if this catalog item should be hidden in channel manager
     */
    boolean isHidden();

    /**
     * @return a depth-first stream of this {@link HstComponentConfiguration} plus its descendants
     */
    default Stream<HstComponentConfiguration> flattened() {
        return Stream.concat(Stream.of(this), getChildren().values().stream().flatMap(HstComponentConfiguration::flattened));
    }
}
