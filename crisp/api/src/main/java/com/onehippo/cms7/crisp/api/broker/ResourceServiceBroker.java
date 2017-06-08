/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.broker;

import java.util.Map;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.SingletonService;

import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import com.onehippo.cms7.crisp.api.resource.ResourceException;
import com.onehippo.cms7.crisp.api.resource.ResourceLink;
import com.onehippo.cms7.crisp.api.resource.ResourceResolver;

/**
 * CRISP Resource Service Broker abstraction.
 * <P>
 * This interface abstracts a "Broker" pattern for various {@link Resource}s from different backends.
 * So, application codes can simply invoke operations of this "Broker" interface to retrieve or manipulate any
 * {@link Resource}s from different backends without having to worry about the details about the underlying backends.
 * </P>
 * <P>
 * Another important aspect is that this "Broker" interface takes care of caching of {@link Resource} representations
 * and cache invalidations. So, application codes do not have to worry about caching and cache invalidations in
 * application layer any more, but they can transparently invoke the operations of this interface. Then this "Broker"
 * implementation should take care of all the details including caching, cache invalidations, etc.
 * </P>
 * <P>
 * Also, this "Broker" service is normally registered through {@link HippoServiceRegistry} as a {@link SingletonService}.
 * So, applications can access this singleton "Broker" service like the following example:
 * </P>
 * <PRE>
 * ResourceServiceBroker broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
 * </PRE>
 */
@SingletonService
public interface ResourceServiceBroker {

    /**
     * Resolves a proper {@link ResourceResolver} by the specified {@code resourceSpace} and resolves single {@link Resource}
     * representation by {@code absPath}.
     * <p>{@code absPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code absPath} can be a URI path or part of URL. Or, as
     * an example, the {@code absPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @param absPath absolute path of a {@link Resource}
     * @return single {@link Resource} representation by {@code absPath}
     * @throws ResourceException if resource resolution operation fails
     */
    Resource resolve(String resourceSpace, String absPath) throws ResourceException;

    /**
     * Resolves a proper {@link ResourceResolver} by the specified {@code resourceSpace} and resolves single {@link Resource}
     * representation by {@code absPath}.
     * <p>{@code absPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code absPath} can be a URI path or part of URL. Or, as
     * an example, the {@code absPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * <p>The {@code absPath} template is expanded using the given path variables ({@code pathVariables}), if any.
     * For example, if {@code pathVariables} looks like <code>{"var1":"hello","var2":"world"}</code>
     * and {@code absPath} is <code>".../some/path/{var1}/{var2}/overview"</code>, then it is expanded to
     * <code>".../some/path/hello/world/overview"</code> by the {@code pathVariables} when making a real request
     * to the backend.</p>
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @param absPath absolute path of a {@link Resource}
     * @param pathVariables the variables to expand the template given by {@code absPath}
     * @return single {@link Resource} representation by {@code absPath}
     * @throws ResourceException if resource resolution operation fails
     */
    Resource resolve(String resourceSpace, String absPath, Map<String, Object> pathVariables)
            throws ResourceException;

    /**
     * Resolves a proper {@link ResourceResolver} by the specified {@code resourceSpace} and search {@link Resource}
     * representations from {@code baseAbsPath} and returns a parent {@link Resource} representation which contains
     * a collection of child {@link Resource} representations.
     * <p>{@code baseAbsPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code baseAbsPath} can be a URI path or part of URL. Or, as
     * an example, the {@code baseAbsPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @param baseAbsPath base absolute path of a {@link Resource}
     * @return a parent {@link Resource} representation which contains a collection of child {@link Resource} representations
     * @throws ResourceException if resource resolution operation fails
     */
    Resource findResources(String resourceSpace, String baseAbsPath) throws ResourceException;

    /**
     * Resolves a proper {@link ResourceResolver} by the specified {@code resourceSpace} and search {@link Resource}
     * representations from {@code baseAbsPath} and returns a parent {@link Resource} representation which contains
     * a collection of child {@link Resource} representations.
     * <p>{@code baseAbsPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code baseAbsPath} can be a URI path or part of URL. Or, as
     * an example, the {@code baseAbsPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * <p>The {@code baseAbsPath} template is expanded using the given path variables ({@code pathVariables}), if any.
     * For example, if {@code pathVariables} looks like <code>{"var1":"hello","var2":"world"}</code>
     * and {@code baseAbsPath} is <code>".../some/path/{var1}/{var2}/overview"</code>, then it is expanded to
     * <code>".../some/path/hello/world/overview"</code> by the {@code pathVariables} when making a real request
     * to the backend.</p>
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @param baseAbsPath base absolute path of a {@link Resource}
     * @param pathVariables the variables to expand the template given by {@code absPath}
     * @return a parent {@link Resource} representation which contains a collection of child {@link Resource} representations
     * @throws ResourceException if resource resolution operation fails
     */
    Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException;

    /**
     * Resolves a proper {@link ResourceResolver} by the specified {@code resourceSpace} and resolves a {@link ResourceLink}
     * for the given {@code resource}.
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @param resource resource representation
     * @return a {@link ResourceLink} for the given {@code resource}
     * @throws ResourceException if resource resolution operation fails
     */
    ResourceLink resolveLink(String resourceSpace, Resource resource) throws ResourceException;

    /**
     * Resolves a proper {@link ResourceResolver} by the specified {@code resourceSpace} and resolves a {@link ResourceLink}
     * for the given {@code resource} with passing {@code linkVariables} that can be used by implementation to
     * expand its internal link generation template.
     * <p>How the {@code linkVariables} is used in link generation template expansion is totally up to an implementation.</p>
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @param resource resource representation
     * @param linkVariables the variables to expand the internal link generation template
     * @return a {@link ResourceLink} for the given {@code resource}
     * @throws ResourceException if resource resolution operation fails
     */
    ResourceLink resolveLink(String resourceSpace, Resource resource, Map<String, Object> linkVariables)
            throws ResourceException;

    /**
     * Returns a proper resource cache store representation ({@link ResourceDataCache}) for the specified
     * {@code resourceSpace}.
     * <p>An implementation may return a default resource cache store representation ({@link ResourceDataCache})
     * as a fallback if the resolved {@link ResourceResolver} doesn't have its own {@link ResourceDataCache} instance
     * (in other words, if the resolved {@link ResourceResolver#getResourceDataCache()} return null).
     * @param resourceSpace Resource space name to resolve a proper {@link ResourceResolver}
     * @return a proper resource cache store representation ({@link ResourceDataCache}) for the specified
     *         {@code resourceSpace}, or a fallback {@link ResourceDataCache} if the resolved {@link ResourceResolver}
     *         doesn't have its own {@link ResourceDataCache} instance
     * @throws ResourceException if resource space is not found
     */
    ResourceDataCache getResourceDataCache(String resourceSpace) throws ResourceException;

}
