/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.platform.configuration.site;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.hippoecm.hst.configuration.hosting.Mount;

/**
 * <p>
 * Detaches all configuration from a Mount to this separate object (which can also function as cachekey because of it hashcode and equals impl)
 * </p>
 * <p>
 * Note that this class should have as <strong>LITTLE</strong> as possible that makes a HstSiteMap service unique: Thus for example,
 * if two Mounts have all the configuration below the same, they can <strong>REUSE</strong> the HstSiteMap services instances.
 * </p>
 * <p>
 * Thus make sure that never something like the Mount path or ID gets included, as that would result in useless cachekeys
 * </p>
 */
public class MountSiteMapConfiguration {

    private final Map<String,String> parameters;
    private final String[] defaultSiteMapItemHandlerIds;
    private final String locale;
    private final String namedPipeline;
    private final boolean finalPipeline;
    private final boolean cacheable;
    private final String scheme;
    private final boolean schemeAgnostic;
    private final int schemeNotMatchingResponseCode;
    private final String[] defaultResourceBundleIds;
    private int hashCode;
    private final String mountContentPath;
    private final String mountContextPath;
    private final Map<String, String> responseHeaders;

    public MountSiteMapConfiguration(Mount mount) {
        parameters = mount.getParameters();
        defaultSiteMapItemHandlerIds = mount.getDefaultSiteMapItemHandlerIds();
        locale = mount.getLocale();
        namedPipeline = mount.getNamedPipeline();
        finalPipeline = mount.isFinalPipeline();
        cacheable = mount.isCacheable();
        scheme = mount.getScheme();
        schemeAgnostic = mount.isSchemeAgnostic();
        schemeNotMatchingResponseCode = mount.getSchemeNotMatchingResponseCode();
        defaultResourceBundleIds = mount.getDefaultResourceBundleIds();
        hashCode = computeHashCode();
        mountContentPath = mount.getContentPath();
        mountContextPath = mount.getContextPath();
        responseHeaders = mount.getResponseHeaders();
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String[] getDefaultSiteMapItemHandlerIds() {
        return defaultSiteMapItemHandlerIds;
    }

    public String getLocale() {
        return locale;
    }

    public String getNamedPipeline() {
        return namedPipeline;
    }

    public boolean isFinalPipeline() {
        return finalPipeline;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public String getScheme() {
        return scheme;
    }

    public boolean isSchemeAgnostic() {
        return schemeAgnostic;
    }

    public int getSchemeNotMatchingResponseCode() {
        return schemeNotMatchingResponseCode;
    }

    public String[] getDefaultResourceBundleIds() {
        return defaultResourceBundleIds;
    }

    public String getMountContentPath() {
        return mountContentPath;
    }

    public String getMountContextPath() {
        return mountContextPath;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MountSiteMapConfiguration that = (MountSiteMapConfiguration) o;

        if (cacheable != that.cacheable) {
            return false;
        }
        if (schemeAgnostic != that.schemeAgnostic) {
            return false;
        }
        if (schemeNotMatchingResponseCode != that.schemeNotMatchingResponseCode) {
            return false;
        }
        if (!Arrays.equals(defaultResourceBundleIds, that.defaultResourceBundleIds)) {
            return false;
        }
        if (!Arrays.equals(defaultSiteMapItemHandlerIds, that.defaultSiteMapItemHandlerIds)) {
            return false;
        }
        if (locale != null ? !locale.equals(that.locale) : that.locale != null) {
            return false;
        }
        if (namedPipeline != null ? !namedPipeline.equals(that.namedPipeline) : that.namedPipeline != null) {
            return false;
        }
        if (finalPipeline != that.isFinalPipeline()) {
            return false;
        }
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) {
            return false;
        }
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) {
            return false;
        }
        if (mountContentPath != null ? !mountContentPath.equals(that.mountContentPath) : that.mountContentPath != null) {
            return false;
        }
        if (mountContextPath != null ? !mountContextPath.equals(that.mountContextPath) : that.mountContextPath != null) {
            return false;
        }
        if (!Objects.equals(responseHeaders, that.responseHeaders)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        int result = parameters != null ? parameters.hashCode() : 0;
        result = 31 * result + (defaultSiteMapItemHandlerIds != null ? Arrays.hashCode(defaultSiteMapItemHandlerIds) : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (namedPipeline != null ? namedPipeline.hashCode() : 0);
        result = 31 * result + (cacheable ? 1 : 0);
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        result = 31 * result + (schemeAgnostic ? 1 : 0);
        result = 31 * result + schemeNotMatchingResponseCode;
        result = 31 * result + (defaultResourceBundleIds != null ? Arrays.hashCode(defaultResourceBundleIds) : 0);
        result = 31 * result + (mountContentPath != null ? mountContentPath.hashCode() : 0);
        result = 31 * result + (mountContextPath != null ? mountContextPath.hashCode() : 0);
        result = 31 * result + (responseHeaders != null ? responseHeaders.hashCode() : 0);
        return result;
    }
}
