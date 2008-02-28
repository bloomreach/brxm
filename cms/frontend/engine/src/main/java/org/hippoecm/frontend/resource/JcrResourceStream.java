/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceStream extends JcrNodeModel implements IResourceStream {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrResourceStream.class);

    private transient InputStream stream;

    public JcrResourceStream(Node node) {
        super(node);

        if (log.isDebugEnabled()) {
            try {
                assert (node.isNodeType("nt:resource"));
            } catch (RepositoryException e) {
                // ignore
            }
        }
    }

    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    public String getContentType() {
        try {
            return getNode().getProperty("jcr:mimeType").getString();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public InputStream getInputStream() throws ResourceStreamNotFoundException {
        if (stream == null) {
            try {
                stream = getNode().getProperty("jcr:data").getStream();
            } catch (RepositoryException ex) {
                throw new ResourceStreamNotFoundException(ex);
            }
        }
        return stream;
    }

    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    public long length() {
        return -1;
    }

    public void setLocale(Locale locale) {
        // TODO Auto-generated method stub
    }

    public Time lastModifiedTime() {
        try {
            Calendar date = getNode().getProperty("jcr:lastModified").getDate();
            return Time.valueOf(date.getTime());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }
}
