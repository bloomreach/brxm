/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceStream extends NodeModelWrapper<Void> implements IResourceStream {

    private static final Logger log = LoggerFactory.getLogger(JcrResourceStream.class);

    private transient InputStream stream;

    public JcrResourceStream(IModel<Node> model) {
        super(model);
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    @Override
    public String getContentType() {
        try {
            Node node = getNode();
            if (node != null) {
                return node.getProperty("jcr:mimeType").getString();
            } else {
                return "unknown";
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException {
        try {
            if (stream != null) {
                stream.close();
            }
            Node node = getNode();
            if (node != null) {
                stream = node.getProperty("jcr:data").getStream();
            } else {
                stream = new ByteArrayInputStream(new byte[0]);
            }
        } catch (IOException | RepositoryException ex) {
            throw new ResourceStreamNotFoundException(ex);
        }
        return stream;
    }

    @Override
    public Bytes length() {
        long length = -1;
        try {
            Node node = getNode();
            if (node == null) {
                return null;
            }
            length = node.getProperty("jcr:data").getLength();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return Bytes.bytes(length);
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public void setLocale(Locale locale) {
    }

    @Override
    public String getStyle() {
        return null;
    }

    @Override
    public void setStyle(final String style) {
    }

    @Override
    public String getVariation() {
        return null;
    }

    @Override
    public void setVariation(final String variation) {
    }

    @Override
    public Time lastModifiedTime() {
        try {
            Node node = getNode();
            Calendar date;
            if (node != null) {
                date = node.getProperty("jcr:lastModified").getDate();
            } else {
                date = Calendar.getInstance();
            }
            return Time.valueOf(date.getTime());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }
}
