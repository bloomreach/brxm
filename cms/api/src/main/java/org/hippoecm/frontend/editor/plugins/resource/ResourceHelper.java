/*
 *  Copyright 2010-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource helper for creating and validating nodes of type <code>hippo:resource</code>
 */
public class ResourceHelper {

    static final Logger log = LoggerFactory.getLogger(ResourceHelper.class);

    private ResourceHelper() {
    }

    /**
     * Set the default 'hippo:resource' properties:
     * <ul>
     *   <li>jcr:mimeType</li>
     *   <li>jcr:data</li>
     *   <li>jcr:lastModified</li>
     * </ul>
     *
     * @param node the {@link Node} on which to set the properties
     * @param mimeType the mime-type of the binary data (e.g. <i>application/pdf</i>, <i>image/jpeg</i>)
     * @param inputStream the data stream. Once the properties have been set the input stream will be closed.
     *
     * @throws RepositoryException exception thrown when one of the properties or values could not be set
     */
    public static void setDefaultResourceProperties(Node node, String mimeType, InputStream inputStream) throws RepositoryException {
        try (InputStream is = inputStream) {
            setDefaultResourceProperties(node, mimeType, getValueFactory(node).createBinary(is), null);
        } catch (IOException e) {
            log.warn("Something went wrong while reading the inputstream when setting properties of node: {}", JcrUtils.getNodePathQuietly(node) ,e);
        }
    }

    /**
     * Set the default 'hippo:resource' properties:
     * <ul>
     *   <li>jcr:mimeType</li>
     *   <li>jcr:data</li>
     *   <li>jcr:lastModified</li>
     *   <li>hippo:filename</li>
     * </ul>
     *
     * @param node the {@link Node} on which to set the properties
     * @param mimeType the mime-type of the binary data (e.g. <i>application/pdf</i>, <i>image/jpeg</i>)
     * @param inputStream the data stream. Once the properties have been set the input stream will be closed.
     *
     * @throws RepositoryException exception thrown when one of the properties or values could not be set
     */
    public static void setDefaultResourceProperties(Node node, String mimeType, InputStream inputStream, String filename) throws RepositoryException {
        try (InputStream is = inputStream){
            setDefaultResourceProperties(node, mimeType, getValueFactory(node).createBinary(is), filename);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Set the default 'hippo:resource' properties:
     * <ul>
     *   <li>jcr:mimeType</li>
     *   <li>jcr:data</li>
     *   <li>jcr:lastModified</li>
     *   <li>hippo:filename</li>
     * </ul>
     *
     * @param node the {@link Node} on which to set the properties
     * @param mimeType the mime-type of the binary data (e.g. <i>application/pdf</i>, <i>image/jpeg</i>)
     * @param binary the binary data.
     * @param filename the name of the resource file, should not be <code>null</code> or "".
     *
     * @throws RepositoryException exception thrown when one of the properties or values could not be set
     */
    public static void setDefaultResourceProperties(final Node node, final String mimeType, final Binary binary, final String filename) throws RepositoryException {
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        node.setProperty(JcrConstants.JCR_DATA, binary);
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
        node.setProperty(HippoNodeType.HIPPO_FILENAME, filename);
    }

    /**
     * Handles the {@link InputStream} and extract text content from the PDF and sets it as a binary type property on the
     * resource node. Once the property has been set the input stream will be closed.
     *
     * @param node the {@link Node} on which to set the '{@value org.hippoecm.repository.api.HippoNodeType#HIPPO_TEXT}' property
     * @param inputStream data stream
     */
    public static void handlePdfAndSetHippoTextProperty(Node node, InputStream inputStream) {
        String content = StringUtils.EMPTY;
        try {
            content = PdfParser.parse(inputStream);
        } catch (Throwable e) {
            log.warn("An exception or error occurred while trying to parse the pdf", e);
        }
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content.getBytes())) {
            node.setProperty(HippoNodeType.HIPPO_TEXT, getValueFactory(node).createBinary(byteInputStream));
        } catch (RepositoryException | IOException e) {
            setEmptyHippoTextBinary(node);
            log.warn("An exception occurred while trying to set property with extracted text for node {}", JcrUtils.getNodePathQuietly(node), e);
        }
        finally{
            if (inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("An exception occurred while trying to close stream associated with node {}", JcrUtils.getNodePathQuietly(node), e);
                }
            }
        }
    }

    private static void setEmptyHippoTextBinary(final Node node) {
        try {
            final ByteArrayInputStream emptyByteArrayInputStream = new ByteArrayInputStream(new byte[0]);
            node.setProperty(HippoNodeType.HIPPO_TEXT, getValueFactory(node).createBinary(emptyByteArrayInputStream));
        } catch (RepositoryException e) {
            log.error("Unable to store empty hippo:text binary for node {}", JcrUtils.getNodePathQuietly(node), e);
        }
    }

    /**
     * Gets the {@link ValueFactory} from the {@link Session}
     *
     * @param node the {@link Node} from which to get the {@link Session}
     *
     * @return a {@link ValueFactory}
     *
     * @throws RepositoryException In case something goes wrong while trying to get the {@link Session} or {@link ValueFactory}
     */
    public static ValueFactory getValueFactory(Node node) throws RepositoryException {
        return node.getSession().getValueFactory();
    }
}
