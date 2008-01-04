/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.migration.webdav;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import nl.hippo.webdav.batchprocessor.ProcessingException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;
import org.apache.webdav.lib.methods.DepthSupport;
import org.apache.webdav.lib.methods.PropFindMethod;
import org.apache.webdav.lib.methods.PropPatchMethod;
import org.apache.webdav.lib.properties.ResourceTypeProperty;

/**
 * Class to hold a WebDAV node. The properties and the content are fetched from the
 * repository when needed (lazy method).
 */
public class WebdavNode {

    /** the uri of the current node */
    private String uri;

    /** Map to hold the properties */
    private Map properties = new HashMap();

    /** the initialized httpClient */
    private HttpClient httpClient;

    /** byte array to hold the contents */
    private byte[] contents = null;

    /**
     * Setup the node
     * @param httpClient
     * @param uri
     */
    public WebdavNode(HttpClient httpClient, String uri) {
        this.httpClient = httpClient;
        this.uri = uri;
    }

    /**
     * Clear the cache of the content and the properties.
     */
    public void clearCache() {
        properties = new HashMap();
        contents = null;
    }

    /**
     * Get the uri of the node
     * @return uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Execute a http method
     * @param method
     * @return the http status code
     * @throws IOException
     */
    public int executeMethod(HttpMethod method) throws IOException
    {
        return httpClient.executeMethod(method);
    }

    /**
     * Check if the current node is a collection
     * @return
     * @throws ProcessingException
     */
    public boolean isCollection() throws ProcessingException {

        boolean result = false;
        Object resourceTypeAsObject = getProperty("DAV:", "resourcetype");
        if (resourceTypeAsObject instanceof ResourceTypeProperty) {
            ResourceTypeProperty resourceType = (ResourceTypeProperty) resourceTypeAsObject;
            result = resourceType != null && resourceType.isCollection();
        }
        return result;
    }

    /**
     * Return a property from the cache or fetch it from the repository and put (all)
     * the properties in the cache
     * @param namespaceUri
     * @param name
     * @return the Property
     * @throws ProcessingException
     */
    public Property getProperty(String namespaceUri, String name) throws ProcessingException {

        Property result;

        final PropertyName propName = new PropertyName(namespaceUri, name);
        if (properties.containsKey(propName)) {
            result = (Property) properties.get(propName);
        } else {
            PropFindMethod propfind = new PropFindMethod(uri);
            propfind.setDoAuthentication(true);
            propfind.setDepth(DepthSupport.DEPTH_0);
            propfind.setType(PropFindMethod.BY_NAME);
            propfind.setPropertyNames(new Enumeration() {
                private boolean m_hasElementBeenRetrieved;

                public boolean hasMoreElements() {
                    return !m_hasElementBeenRetrieved;
                }

                public Object nextElement() {
                    m_hasElementBeenRetrieved = true;
                    return propName;
                }
            });
            try {
                result = null;
                int propfindResult = executeMethod(propfind);
                if (propfindResult == 207) {
                    Enumeration responseUrls = propfind.getAllResponseURLs();
                    if (responseUrls.hasMoreElements()) {
                        Enumeration propertiesEnumeration = propfind.getResponseProperties((String) responseUrls.nextElement());
                        while (propertiesEnumeration.hasMoreElements()) {
                            Property prop = (Property) propertiesEnumeration.nextElement();
                            properties.put(new PropertyName(prop.getNamespaceURI(), prop.getLocalName()), prop);
                        }
                    }
                } else {
                    throw new ProcessingException("WebDAV method failed: " + uri + " with code: " + propfindResult);
                }
            } catch (IOException e) {
                throw new ProcessingException("Error during processing of: " + uri, e);
            } finally {
                propfind.releaseConnection();
            }
        }
        return result;
    }

    /**
     * Set a property and update the cache
     * @param namespaceUri
     * @param name
     * @param value
     * @throws ProcessingException
     * @throws IOException
     */
    public void setProperty(String namespaceUri, String name, String value) throws ProcessingException, IOException {
        int status = 0;
        String statusText = null;

        PropPatchMethod proppatch = new PropPatchMethod(uri);
        PropertyName propName = new PropertyName(namespaceUri, name);
        proppatch.addPropertyToSet(propName.getLocalName(), value, null, propName.getNamespaceURI());

        try {
            status = executeMethod(proppatch);
            statusText = URIUtil.decode(proppatch.getStatusText());
        } finally {
            proppatch.releaseConnection();
        }

        /* handle return codes */
        if (status == 200 || status == 207) {
            // Update current value
            properties.put(propName, value);
            //System.out.println("PROPPATCH: " + status + "(" + statusText + ")");
        } else if (status >= 400 && status < 500) {
            throw new ProcessingException("PROPPATCH ERROR: " + status + "(" + statusText + ")");
        } else if (status >= 500 && status < 600) {
            throw new ProcessingException("SERVER ERROR: " + status + "(" + statusText + ")");
        } else {
            throw new ProcessingException("ERROR invalid status from server: " + status + "(" + statusText + ")");
        }

    }

    /**
     * Delete a property and update the cache
     * @param namespaceUri
     * @param name
     * @throws ProcessingException
     * @throws IOException
     */
    public void deleteProperty(String namespaceUri, String name) throws ProcessingException, IOException {
        int status = 0;
        String statusText = null;


        PropPatchMethod proppatch = new PropPatchMethod(uri);
        PropertyName propName = new PropertyName(namespaceUri, name);
        proppatch.addPropertyToRemove(propName.getLocalName(), null, propName.getNamespaceURI());

        try {
            status = executeMethod(proppatch);
            statusText = URIUtil.decode(proppatch.getStatusText());
        } finally {
            proppatch.releaseConnection();
        }

        /* handle return codes */
        if (status == 200 || status == 207) {
            // Update current value
            properties.put(propName, null);
            //System.out.println("PROPPATCH: " + status + "(" + statusText + ")");
        } else if (status >= 400 && status < 500) {
            throw new ProcessingException("PROPPATCH ERROR: " + status + "(" + statusText + ")");
        } else if (status >= 500 && status < 600) {
            throw new ProcessingException("SERVER ERROR: " + status + "(" + statusText + ")");
        } else {
            throw new ProcessingException("ERROR invalid status from server: " + status + "(" + statusText + ")");
        }

    }

    /**
     * Get the contents of a node from the cache or fetch it from the repository
     * @return the contents of the node
     * @throws IOException
     * @throws ProcessingException
     */
    public byte[] getContents() throws IOException, ProcessingException {
        if (contents != null ) {
            return contents;
        }
        GetMethod get = new GetMethod(uri);
        try {
            int getStatus = executeMethod(get);
            if (getStatus != 200) {
                throw new ProcessingException("Failed to get contents: server responded " + getStatus + " (" + get.getStatusText()
                        + ")");
            }
            contents = get.getResponseBody();
        } finally {
            get.releaseConnection();
        }
        return contents;
    }

    /**
     * Set the contents of a node and update the cache
     * @param contents
     * @throws IOException
     * @throws ProcessingException
     */
    public void setContents(byte[] contents) throws IOException, ProcessingException {
        int status = 0;
        String statusText = null;
        PutMethod putMethod = new PutMethod(uri);
        putMethod.setRequestBody(new ByteArrayInputStream(contents));
        try {
            status = executeMethod(putMethod);
            statusText = URIUtil.decode(putMethod.getStatusText());
        } finally {
            putMethod.releaseConnection();
        }

        /* handle return codes */
        if (status == 201 || status == 202 || status == 204 || status == 210) {
            this.contents = contents;
           // System.out.println("PUT: " + status + "(" + statusText + ")");
        } else if (status >= 400 && status < 500) {
            throw new ProcessingException("PUT ERROR: " + status + "(" + statusText + ")");
        } else if (status >= 500 && status < 600) {
            throw new ProcessingException("SERVER ERROR: " + status + "(" + statusText + ")");
        } else {
            throw new ProcessingException("ERROR invalid status from server: " + status + "(" + statusText + ")");
        }
    }

    /**
     * Delete the node
     * @throws IOException
     * @throws ProcessingException
     */
    public void delete() throws IOException, ProcessingException {
        int status = 0;
        String statusText = null;
        DeleteMethod delete = new DeleteMethod(uri);

        try {
            status = executeMethod(delete);
            statusText = URIUtil.decode(delete.getStatusText());
        } finally {
            delete.releaseConnection();
            clearCache();
        }

        // handle return codes
        if (status == 200 || status == 202 || status == 204) {
            clearCache();
            //System.out.println("DELETED: " + status + "(" + statusText + ")");
        } else if (status >= 400 && status < 500) {
            throw new ProcessingException("DELETE ERROR: " + status + "(" + statusText + ")");
        } else if (status >= 500 && status < 600) {
            throw new ProcessingException("SERVER ERROR: " + status + "(" + statusText + ")");
        } else {
            throw new ProcessingException("ERROR invalid status from server: " + status + "(" + statusText + ")");
        }

    }

}
