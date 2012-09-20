/*
 *  Copyright 2010 Hippo.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.utils.ParseUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource helper for creating and validating nodes of type <code>hippo:resource</code>
 */
public class ResourceHelper {

    public static final String MIME_IMAGE_PJPEG = "image/pjpeg";
    public static final String MIME_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_PDF = "application/pdf";

    static final Logger log = LoggerFactory.getLogger(ResourceHelper.class);

    private ResourceHelper() {
    }

    public static void validateResource(Node resource, String filename) throws ResourceException, RepositoryException {
        try {
            String mimeType = (resource.hasProperty(JcrConstants.JCR_MIMETYPE) ? resource.getProperty(JcrConstants.JCR_MIMETYPE).getString()
                    : "");
            mimeType = mimeType.toLowerCase();
            if (mimeType.equals(MIME_IMAGE_PJPEG)) {
                mimeType = MIME_IMAGE_JPEG;
            }
            if (mimeType.startsWith("image/")) {
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.setInput(resource.getProperty(JcrConstants.JCR_DATA).getBinary().getStream());
                if (imageInfo.check()) {
                    String imageInfoMimeType = imageInfo.getMimeType();
                    if (imageInfoMimeType == null) {
                        throw new ResourceException("impermissible image type content");
                    } else {
                        if (imageInfoMimeType.equals(MIME_IMAGE_PJPEG)) {
                            imageInfoMimeType = MIME_IMAGE_JPEG;
                        }
                        if (!imageInfoMimeType.equalsIgnoreCase(mimeType)) {
                            throw new ResourceException("mismatch image mime type");
                        }
                    }
                } else {
                    throw new ResourceException("impermissible image type content");
                }
            } else if (mimeType.equals(MIME_TYPE_PDF)) {
                String line;
                line = new BufferedReader(new InputStreamReader(resource.getProperty(JcrConstants.JCR_DATA).getBinary().getStream()))
                        .readLine().toUpperCase();
                if (!line.startsWith("%PDF-")) {
                    throw new ResourceException("impermissible pdf type content");
                }
            } else if (mimeType.equals("application/postscript")) {
                String line;
                line = new BufferedReader(new InputStreamReader(resource.getProperty(JcrConstants.JCR_DATA).getBinary().getStream()))
                        .readLine().toUpperCase();
                if (!line.startsWith("%!")) {
                    throw new ResourceException("impermissible postscript type content");
                }
            } else {
                // This method can be overridden to allow more such checks on content type.  if such an override
                // wants to be really strict and not allow unknown content, the following thrown exception is to be included
                // throw new ValueFormatException("impermissible unrecognized type content");
            }
        } catch (IOException ex) {
            throw new ResourceException("impermissible unknown type content");
        }
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
        try{
            setDefaultResourceProperties(node, mimeType, getValueFactory(node).createBinary(inputStream));
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
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
     * @param binary the binary data.
     *
     * @throws RepositoryException exception thrown when one of the properties or values could not be set
     */
    public static void setDefaultResourceProperties(final Node node, final String mimeType, final Binary binary) throws RepositoryException {
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        node.setProperty(JcrConstants.JCR_DATA, binary);
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
    }

    /**
     * Handles the {@link InputStream} and extract text content from the PDF and sets it as a binary type property on the
     * resource node. Once the property has been set the input stream will be closed.
     *
     * @param node the {@link Node} on which to set the '{@value org.hippoecm.repository.api.HippoNodeType#HIPPO_TEXT}' property
     * @param inputStream data stream
     */
    public static void handlePdfAndSetHippoTextProperty(Node node, InputStream inputStream) {
        ByteArrayInputStream byteInputStream = null;
        String nodePath = null;
        try {
            nodePath = node.getPath();
            String content = ParseUtils.getStringContent(inputStream, getTikaConfig(), MIME_TYPE_PDF);
            byteInputStream = new ByteArrayInputStream(content.getBytes());
            node.setProperty(HippoNodeType.HIPPO_TEXT, getValueFactory(node).createBinary(byteInputStream));
        } catch (IOException e) {
            setEmptyHippoTextBinary(node);
            log.warn("An exception has occurred while trying to create inputstream based on " +
                        "extracted text for node '"+nodePath+"' ",e);
        } catch (RepositoryException e) {
            setEmptyHippoTextBinary(node);
            log.warn("An exception occurred while trying to set property with extracted text for node '"+nodePath+"' ",e);
        } catch (TikaException e) {
            setEmptyHippoTextBinary(node);
            log.warn("An exception occurred while trying to set Tika configuration for node '"+nodePath+"' ",e);
        } catch (Throwable e) {
            setEmptyHippoTextBinary(node);
            log.warn("An exception occurred while trying to set property with extracted text for node '"+nodePath+"' ",e);
        } finally {
            IOUtils.closeQuietly(byteInputStream);
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static void setEmptyHippoTextBinary(final Node node) {
        String nodePath = null;
        try {
            nodePath = node.getPath();
            final ByteArrayInputStream emptyByteArrayInputStream = new ByteArrayInputStream(new byte[0]);
            node.setProperty(HippoNodeType.HIPPO_TEXT, getValueFactory(node).createBinary(emptyByteArrayInputStream));
        } catch (RepositoryException e) {
            log.error("Unable to store empty hippo:text binary for node '"+nodePath+"'", e);
        }
    }

    /**
     * Gets the default Tika Configuration
     *
     * @return the {@link org.apache.tika.config.TikaConfig}
     */
    private static TikaConfig getTikaConfig(){
        return TikaConfig.getDefaultConfig();
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
