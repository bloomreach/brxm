/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.utils;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceUtils {

    private static final Logger log = LoggerFactory.getLogger(ResourceUtils.class);

    public static final String DEFAULT_BASE_BINARIES_CONTENT_PATH = "";

    public static final String DEFAULT_BINARY_RESOURCE_NODE_TYPE = HippoNodeType.NT_RESOURCE;

    public static final String DEFAULT_BINARY_DATA_PROP_NAME = "jcr:data";

    public static final String DEFAULT_BINARY_MIME_TYPE_PROP_NAME = "jcr:mimeType";

    public static final String DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME = "jcr:lastModified";


    /**
     * Hide constructor of utility class
     */
    private ResourceUtils() {
    }

    public static long getLastModifiedDate(Node resourceNode, String binaryLastModifiedPropName) {
        if (resourceNode == null) {
            return -1L;
        }
        Calendar lastModifiedDate = null;
        try {
            // don't check for existence first as the property will almost always exist
            lastModifiedDate = resourceNode.getProperty(binaryLastModifiedPropName).getDate();
        } catch (PathNotFoundException e) {
            log.info("The property, {}, is not found {}", binaryLastModifiedPropName, e.getMessage());
        } catch (ValueFormatException e) {
            log.warn("The property, {}, is not in valid format. {}", binaryLastModifiedPropName, e);
        } catch (RepositoryException e) {
            log.error("The property, {}, cannot be retrieved. {}", binaryLastModifiedPropName, e);
        }
        return lastModifiedDate != null ? lastModifiedDate.getTimeInMillis() : -1L;
    }

    public static long getDataLength(Node resourceNode, String binaryDataPropName) {
        try {
            return resourceNode.getProperty(binaryDataPropName).getLength();
        } catch (RepositoryException e) {
            log.warn("Unable to determine binary data length", e);
        }
        return -1;
    }

    public static String getFileName(Node resourceNode, String[] contentDispositionFileNamePropertyNames) {
        if (contentDispositionFileNamePropertyNames != null && contentDispositionFileNamePropertyNames.length > 0) {
            for (String name : contentDispositionFileNamePropertyNames) {
                try {
                    if (resourceNode.hasProperty(name)) {
                        return resourceNode.getProperty(name).getString();
                    }
                } catch (RepositoryException e) {
                    log.warn("Unable to determine file name", e);
                }
            }
        }
        return null;
    }

    public static String getResourcePath(HttpServletRequest request, String contentPath) {
        String relPath = getResourceRelPath(request);
        StringBuilder resourcePathBuilder = new StringBuilder();

        if (contentPath != null && !"".equals(contentPath)) {
            resourcePathBuilder.append('/').append(PathUtils.normalizePath(contentPath));
        }
        if (relPath != null) {
            resourcePathBuilder.append('/').append(PathUtils.normalizePath(relPath));
        }
        return resourcePathBuilder.toString();
    }

    public static String getResourceRelPath(HttpServletRequest request) {
        String path = null;

        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);

        if (hstRequest != null) {
            path = hstRequest.getResourceID();
        }

        if (path == null) {
            try {
                path = HstRequestUtils.getRequestPath(request);
                path = path.substring(request.getServletPath().length());
            } catch (IllegalArgumentException e) {
                log.warn("Cannot decode uri: {}. {}", request.getRequestURI(), e.getMessage());
            }
        }

        if (path != null && !path.startsWith("/") && path.indexOf(':') > 0) {
            path = path.substring(path.indexOf(':') + 1);
        }

        return path;
    }

    public static Node lookUpResource(Session session, String resourcePath,
            Map<String, List<ResourceContainer>> prefix2ResourceContainer, List<ResourceContainer> allResourceContainers) {
        Node resourceNode = null;

        // find the correct item
        String[] elems = resourcePath.substring(1).split("/");
        List<ResourceContainer> resourceContainersForPrefix = prefix2ResourceContainer.get(elems[0]);
        if (resourceContainersForPrefix == null) {
            for (ResourceContainer container : allResourceContainers) {
                resourceNode = container.resolveToResourceNode(session, resourcePath);
                if (resourceNode != null) {
                    return resourceNode;
                }
            }
        } else {
            // use the first resourceContainer that can fetch a resourceNode for this path
            for (ResourceContainer container : resourceContainersForPrefix) {
                resourceNode = container.resolveToResourceNode(session, resourcePath);
                if (resourceNode != null) {
                    return resourceNode;
                }
            }

            // we did not find a container that could resolve the node. Fallback to test any container who can resolve the path
            for (ResourceContainer container : allResourceContainers) {
                if (resourceContainersForPrefix.contains(container)) {
                    // skip already tested resource containers
                    continue;
                }
                resourceNode = container.resolveToResourceNode(session, resourcePath);
                if (resourceNode != null) {
                    return resourceNode;
                }
            }
        }
        log.info("Node at path '{}' cannot be found.", resourcePath);
        return null;
    }

    public static boolean isValidResourcePath(String path) {
        if (path == null || !path.startsWith("/")) {
            return false;
        }
        return true;
    }

    public static boolean hasValideType(Node resourceNode, String binaryResourceNodeType) {
        try {
            if (resourceNode.isNodeType(binaryResourceNodeType)) {
                return true;
            }
            log.info("Found node is not of type '{}' but was of type '{}'.", binaryResourceNodeType, resourceNode
                    .getPrimaryNodeType().getName());
        } catch (RepositoryException e) {
            log.info("Unable to determine if resource is of type " + binaryResourceNodeType, e);
        }
        return false;
    }

    public static boolean hasBinaryProperty(Node resourceNode, String binaryDataPropName) {
        try {
            if (resourceNode.hasProperty(binaryDataPropName)) {
                return true;
            }
            log.info("Node at path '{}' does not have a binary property: {}.", resourceNode.getPath(),
                    binaryDataPropName);
        } catch (RepositoryException e) {
            log.info("Unable to determine if resource has binary property " + binaryDataPropName, e);
        }
        return false;
    }

    public static boolean hasMimeTypeProperty(Node resourceNode, String binaryMimeTypePropName) {
        try {
            if (resourceNode.hasProperty(binaryMimeTypePropName)) {
                return true;
            }
            log.info("Node at path '{}' does not have a mime type property: {}.", resourceNode.getPath(),
                    binaryMimeTypePropName);
        } catch (RepositoryException e) {
            log.info("Unable to determine resource mime  type " + binaryMimeTypePropName, e);
        }
        return false;
    }
}
