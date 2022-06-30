/*
 * Copyright 2017-2022 Bloomreach
 */
package org.onehippo.cms7.crisp.api.exchange;

import java.util.List;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.ResourceResolver;

/**
 * Builder to help build {@link ExchangeHint} instance.
 */
public abstract class ExchangeHintBuilder {

    /**
     * Create a default {@link ExchangeHintBuilder} instance.
     * @return a default {@link ExchangeHintBuilder} instance
     */
    public static ExchangeHintBuilder create() {
        return new DefaultExchangeHintBuilder();
    }

    /**
     * Build a {@link ExchangeHint}.
     * @return
     */
    public abstract ExchangeHint build();

    /**
     * Set method hint name.
     * @param methodName method hint name.
     * @return this ExchangeHintBuilder
     */
    public abstract ExchangeHintBuilder methodName(String methodName);

    /**
     * Return method hint name
     * @return method hint name
     */
    public abstract String methodName();

    /**
     * Set request object representation or request callback instance than can be understood by the backend.
     * <P>
     * <EM>Note:</EM> It is strongly recommended to use {@link #requestHeader(String, String...)} and {@link #requestBody(Object)}
     * instead of using this deprecated method.
     * When {@link #requestHeader(String, String...)} or {@link #requestBody(Object)} is used, any invocation of
     * this deprecated method will be just ignored.
     * </P>
     * @param request request object representation or request callback instance that can be understood by the backend
     * @return request object representation or request callback instance that can be understood by the backend
     * @deprecated Use {@link #requestHeaders(Map)} and {@link #requestBody(Object)} instead.
     */
    @Deprecated
    public abstract ExchangeHintBuilder request(Object request);

    /**
     * Return request object representation or request callback instance than can be understood by the backend.
     * <P>
     * <EM>Note:</EM> It is strongly recommended to use {@link #requestHeaders()} and {@link #requestBody()}
     * instead of using this deprecated method.
     * </P>
     * @return request object representation or request callback instance that can be understood by the backend
     * @deprecated Use {@link #requestHeaders()} and {@link #requestBody()} instead.
     */
    @Deprecated
    public abstract Object request();

    /**
     * Add request headers.
     * @param headerName request header name
     * @param headerValue request header values
     * @return this builder
     */
    public abstract ExchangeHintBuilder requestHeader(String headerName, String ... headerValue);

    /**
     * Reset request headers by the given new {@code requestHeaders}.
     * @param requestHeaders request headers
     * @return this builder
     */
    public abstract ExchangeHintBuilder requestHeaders(Map<String, List<String>> requestHeaders);

    /**
     * Return request headers.
     * @return request headers
     */
    public abstract Map<String, List<String>> requestHeaders();

    /**
     * Set request body object representation or request callback instance than can be understood by the backend
     * @param requestBody request body object representation or request callback instance that can be understood by
     * the backend
     * @return this builder
     */
    public abstract ExchangeHintBuilder requestBody(Object requestBody);

    /**
     * Return request body object representation or request callback instance than can be understood by the backend.
     * @return request body object representation or request callback instance that can be understood by the backend
     */
    public abstract Object requestBody();

    /**
     * Set the flag whether or not the underlying {@link ResourceResolver} should disallow caching.
     * @param noCache flag whether or not the underlying {@link ResourceResolver} should disallow caching
     * @return this builder
     */
    public abstract ExchangeHintBuilder noCache(boolean noCache);

    /**
     * Return true if the underlying {@link ResourceResolver} should disallow caching.
     * @return true if the underlying {@link ResourceResolver} should disallow caching
     */
    public abstract boolean noCache();

}
