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
package org.onehippo.cms7.services.context;

import java.util.EnumSet;

import javax.servlet.ServletContext;

public final class HippoWebappContext {

    public enum Type {
        PLATFORM,
        CMS,
        SITE,
        OTHER
    }

    public static final EnumSet<Type> CMS_OR_PLATFORM = EnumSet.of(Type.CMS, Type.PLATFORM);

    private final Type type;
    private final ServletContext servletContext;

    public HippoWebappContext(final Type type, final ServletContext servletContext) {
        this.type = type;
        this.servletContext = servletContext;
    }

    public Type getType() {
        return type;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public int hashCode() {
        return servletContext.getContextPath().hashCode();
    }

    public boolean equals(final Object other) {
        return other instanceof HippoWebappContext &&
                servletContext.getContextPath().equals(((HippoWebappContext)other).servletContext.getContextPath());
    }
}
