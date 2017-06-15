/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.api.resource;

import java.util.Map;

/**
 * Responsible for resolving {@link Resource}(s).
 */
public interface ResourceResolver extends ResourceCacheResolvable {

    /**
     * Resolves single {@link Resource} representation by {@code absPath}.
     * <p>{@code absPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code absPath} can be a URI path or part of URL. Or, as
     * an example, the {@code absPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * @param absPath absolute path of a {@link Resource}
     * @return single {@link Resource} representation by {@code absPath}
     * @throws ResourceException if resource resolution operation fails
     */
    Resource resolve(String absPath) throws ResourceException;

    /**
     * Resolves single {@link Resource} representation by {@code absPath}.
     * <p>{@code absPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code absPath} can be a URI path or part of URL. Or, as
     * an example, the {@code absPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * <p>The {@code absPath} template is expanded using the given path variables ({@code pathVariables}), if any.
     * For example, if {@code pathVariables} looks like <code>{"var1":"hello","var2":"world"}</code>
     * and {@code absPath} is <code>".../some/path/{var1}/{var2}/overview"</code>, then it is expanded to
     * <code>".../some/path/hello/world/overview"</code> by the {@code pathVariables} when making a real request
     * to the backend.</p>
     * @param absPath absolute path of a {@link Resource}
     * @param pathVariables the variables to expand the template given by {@code absPath}
     * @return single {@link Resource} representation by {@code absPath}
     * @throws ResourceException if resource resolution operation fails
     */
    Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException;

    /**
     * Search {@link Resource} representations from {@code baseAbsPath} and returns a parent {@link Resource} representation
     * which contains a collection of child {@link Resource} representations.
     * <p>{@code baseAbsPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code baseAbsPath} can be a URI path or part of URL. Or, as
     * an example, the {@code baseAbsPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * @param baseAbsPath base absolute path of a {@link Resource}
     * @return a parent {@link Resource} representation which contains a collection of child {@link Resource} representations
     * @throws ResourceException if resource resolution operation fails
     */
    Resource findResources(String baseAbsPath) throws ResourceException;

    /**
     * Search {@link Resource} representations from {@code baseAbsPath} and returns a parent {@link Resource} representation
     * which contains a collection of child {@link Resource} representations.
     * <p>{@code baseAbsPath} is a domain-specific path template that should be meaningful to the backend.
     * For example, if the backend is a REST API, then {@code baseAbsPath} can be a URI path or part of URL. Or, as
     * an example, the {@code baseAbsPath} can be an index name of a search index, table name of databases or node
     * path in JCR, totally depending on {@code ResourceResolver} implementations.</p>
     * <p>The {@code baseAbsPath} template is expanded using the given path variables ({@code pathVariables}), if any.
     * For example, if {@code pathVariables} looks like <code>{"var1":"hello","var2":"world"}</code>
     * and {@code baseAbsPath} is <code>".../some/path/{var1}/{var2}/overview"</code>, then it is expanded to
     * <code>".../some/path/hello/world/overview"</code> by the {@code pathVariables} when making a real request
     * to the backend.</p>
     * @param baseAbsPath base absolute path of a {@link Resource}
     * @param pathVariables the variables to expand the template given by {@code absPath}
     * @return a parent {@link Resource} representation which contains a collection of child {@link Resource} representations
     * @throws ResourceException if resource resolution operation fails
     */
    Resource findResources(String baseAbsPath, Map<String, Object> pathVariables) throws ResourceException;

    /**
     * Returns true if the {@link ResourceResolver} service for a backend is still alive. Returns true by default.
     * <p>An implementation may choose to implement this method to check if the underlying connection with the
     * backend is still alive in this method. In REST API based implementation, it is likely to simply return true
     * in this method. However, for example, an implementation with JCR backend may use <code>javax.jcr.Session#isLive()</code>
     * method in its implementation.</p>
     * @return true if the {@link ResourceResolver} service for a backend is still alive. true by default
     * @throws ResourceException if resource resolution operation fails
     */
    boolean isLive() throws ResourceException;

    /**
     * Refreshes any local state changes, if any.
     * <p>An implementation may choose to implement this method to give a chance to refresh local states. In REST
     * API based implementation, it is likely to simply do nothing in this method. However, for example, an implementation
     * with JCR backend may use <code>javax.jcr.Session#refresh(boolean)</code> method in its implementation.</p>
     * @throws ResourceException if resource resolution operation fails
     */
    void refresh() throws ResourceException;

    /**
     * Closes any resources used in this {@link ResourceResolver} instance with the backend.
     * <p>An implementation may choose to implement this method to give a chance to close any resources used in
     * this implementation. In REST API based implementation, it is likely to simply do nothing in this method
     * if the REST API is stateless. However, for example, an implementation with a stateful connection may close
     * its connection to the backend in this method.</p>
     * @throws ResourceException if resource resolution operation fails
     */
    void close() throws ResourceException;

    /**
     * Returns a domain-specific {@link ResourceLinkResolver} for a {@link Resource} representation generated by
     * this {@link ResourceResolver} implementation.
     * @return a domain-specific {@link ResourceLinkResolver} for a {@link Resource} representation generated by
     *         this {@link ResourceResolver} implementation
     */
    ResourceLinkResolver getResourceLinkResolver();

}
