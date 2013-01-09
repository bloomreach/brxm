/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.registry.Registry;

public class RepositoryUrl {

    // defaults
    public static final String DEFAULT_RMI_NAME = "hipporepository";
    public static final String RMI_PREFIX = "rmi";

    public static final int RMI_PORT = 1099;
    public static final String RMI_HOST = "localhost";
    public static final String RMI_NAME = "hipporepository";

    private String name;
    private String host;
    private int port;

    public RepositoryUrl(String str) throws MalformedURLException {
        if(str == null || str.trim().equals("")) {
            str = RMI_PREFIX + "://" + RMI_HOST + ":" + RMI_PORT + "/" + RMI_NAME;
        }
        try {
            URI uri = new URI(str);
            if (uri.getFragment() != null) {
                throw new MalformedURLException("invalid character, '#', in URL name: " + str);
            } else if (uri.getQuery() != null) {
                throw new MalformedURLException("invalid character, '?', in URL name: " + str);
            } else if (uri.getUserInfo() != null) {
                throw new MalformedURLException("invalid character, '@', in URL host: " + str);
            }
            String scheme = uri.getScheme();
            if (scheme != null && !scheme.equals(RMI_PREFIX)) {
                throw new MalformedURLException("invalid URL scheme: " + str);
            }

            name = uri.getPath();
            if (name != null) {
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.length() == 0) {
                    name = DEFAULT_RMI_NAME;
                }
            }

            host = uri.getHost();
            if (host == null) {
                host = "";
                if (uri.getPort() == -1) {
                    /* handle URIs with explicit port but no host
                     * (e.g., "//:1098/foo"); although they do not strictly
                     * conform to RFC 2396, Naming's javadoc explicitly allows
                     * them.
                     */
                    String authority = uri.getAuthority();
                    if (authority != null && authority.startsWith(":")) {
                        authority = "localhost" + authority;
                        uri = new URI(null, authority, null, null, null);
                    }
                }
            }
            port = uri.getPort();
            if (port == -1) {
                port = Registry.REGISTRY_PORT;
            }
        } catch (URISyntaxException ex) {
            throw (MalformedURLException) new MalformedURLException("invalid URL string: " + str).initCause(ex);
        }
    }

    @Override
    public String toString() {
        return RMI_PREFIX + host + ":" + port + "/" + name;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }
}
