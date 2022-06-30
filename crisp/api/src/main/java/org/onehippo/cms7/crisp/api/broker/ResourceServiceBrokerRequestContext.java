/*
 * Copyright 2017-2022 Bloomreach
 */
package org.onehippo.cms7.crisp.api.broker;

import javax.servlet.ServletRequest;

/**
 * Provides an access method to the current request object if available.
 */
public class ResourceServiceBrokerRequestContext {

    private static ThreadLocal<String> tlResourceSpace = new ThreadLocal<>();
    private static ThreadLocal<ServletRequest> tlRequest = new ThreadLocal<>();

    private ResourceServiceBrokerRequestContext() {
    }

    /**
     * Return the current resource space name in the {@link ResourceServiceBroker} invocation.
     * @return the current resource space name
     */
    public static String getCurrentResourceSpace() {
        return tlResourceSpace.get();
    }

    /**
     * Set the current resource space name in the {@link ResourceServiceBroker} invocation.
     * @param resourceSpace the current resource space name
     */
    public static void setCurrentResourceSpace(String resourceSpace) {
        tlResourceSpace.set(resourceSpace);
    }

    /**
     * Return true if the current request object if available.
     * @return true if the current request object if available
     */
    public static boolean hasCurrentServletRequest() {
        return tlRequest.get() != null;
    }

    /**
     * Return the current request object if available.
     * @return the current request object
     */
    public static ServletRequest getCurrentServletRequest() {
        return tlRequest.get();
    }

    /**
     * Set the current request object if available.
     * @param servletRequest the current request object
     */
    public static void setCurrentServletRequest(ServletRequest servletRequest) {
        tlRequest.set(servletRequest);
    }

    /**
     * Clear all the attributes stored for the current request context.
     */
    public static void clear() {
        tlResourceSpace.remove();
        tlRequest.remove();
    }

}
