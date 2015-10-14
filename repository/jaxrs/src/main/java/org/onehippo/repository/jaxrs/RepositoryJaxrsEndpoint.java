/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.jaxrs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * Class to construct a JAX-RS application endpoint using a fluent API.
 *
 * <p>A JAX-RS application endpoint should contain either a singleton or a root class.</p>
 *
 * <p>For more detailed documentation and example usage, see the
 * <a href="http://www.onehippo.org/library/concepts/hippo-services/repository-jaxrs-service.html">online
 * documentation</a>.</p>
 */
public class RepositoryJaxrsEndpoint {

    private String address;
    private String authorizationNodePath;
    private String authorizationPermission;
    private Application application;
    private Set<Class<?>> classes;
    private Set<Object> singletons;

    public static String qualifiedAddress(String address) {
        if (address == null || address.isEmpty()) {
            address = "/";
        }
        if (address.charAt(0) != '/') {
            address = "/" +address;
        }
        return address;
    }

    public RepositoryJaxrsEndpoint(String address) {
        address(address);
    }

    public RepositoryJaxrsEndpoint address(String address) {
        this.address = qualifiedAddress(address);
        return this;
    }

    public String getAddress() {
        return address;
    }

    /**
     * Configures the application endpoint to use repository authorization to check whether the authenticated user
     * has sufficient permissions to call this application endpoint.
     *
     * @param authorizationNodePath the node path on which the permission is checked
     * @param authorizationPermission the permission that is checked
     * @return
     * @see RepositoryJaxrsService#HIPPO_REST_PERMISSION
     */
    public RepositoryJaxrsEndpoint authorized(String authorizationNodePath, String authorizationPermission) {
        this.authorizationNodePath = authorizationNodePath;
        this.authorizationPermission = authorizationPermission;
        return this;
    }

    public String getAuthorizationNodePath() {
        return authorizationNodePath;
    }

    public String getAuthorizationPermission() {
        return authorizationPermission;
    }

    public RepositoryJaxrsEndpoint app(Application app) {
        if (classes != null || singletons != null) {
            throw new IllegalStateException("Root class(es) or singleton(s) already set.");
        }
        this.application = app;
        return this;
    }

    public Application getApplication() {
        return application;
    }

    public RepositoryJaxrsEndpoint rootClass(Class<?> rootClass) {
        if (application != null) {
            throw new IllegalStateException("Application already set");
        }
        if (classes == null) {
            classes = new HashSet<>();
        }
        classes.add(rootClass);
        return this;
    }

    public Set<Class<?>> getClasses() {
        return classes != null ? classes : Collections.EMPTY_SET;
    }

    public RepositoryJaxrsEndpoint singleton(Object singleton) {
        if (application != null) {
            throw new IllegalStateException("Application already set");
        }
        if (singletons == null) {
            singletons = new HashSet<>();
        }
        singletons.add(singleton);
        return this;
    }

    public Set<Object> getSingletons() {
        return singletons != null ? singletons : Collections.EMPTY_SET;
    }
}
