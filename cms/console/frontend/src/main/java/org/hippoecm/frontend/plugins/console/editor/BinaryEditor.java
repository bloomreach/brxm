/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.console.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Resource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryEditor extends Panel {

    private static final Logger log = LoggerFactory.getLogger(BinaryEditor.class);

    private static final long ONE_KB = 1024;
    private static final long ONE_MB = ONE_KB * ONE_KB;
    private static final long ONE_GB = ONE_KB * ONE_MB;

    public BinaryEditor(String id, JcrPropertyModel model) {
        super(id);
        final IResourceStream stream = new BinaryResourceStream(model);
        final Link link = new ResourceLink("binary-link", new Resource() {
            @Override
            public IResourceStream getResourceStream() {
                return stream;
            }
        });
        link.add(new Label("binary-link-text", "download (" + getSizeString(stream.length()) + ")"));
        add(link);
    }

    private static String getSizeString(final long length) {
        String sizeString;
        if (length / ONE_GB > 0) {
            sizeString = String.valueOf(length / ONE_GB) + " GB";
        } else if (length / ONE_MB > 0) {
            sizeString = String.valueOf(length / ONE_MB) + " MB";
        } else if (length / ONE_KB > 0) {
            sizeString = String.valueOf(length / ONE_KB) + " KB";
        } else {
            sizeString = String.valueOf(length) + " bytes";
        }
        return sizeString;
    }

    private static class BinaryResourceStream implements IResourceStream, Serializable {

        private transient InputStream is;
        private JcrPropertyModel model;

        private BinaryResourceStream(JcrPropertyModel model) {
            this.model = model;
        }

        @Override
        public String getContentType() {
            try {
                final Node node = model.getProperty().getParent();
                return JcrUtils.getStringProperty(node, "jcr:mimeType", "unknown");
            } catch (RepositoryException e) {
                log.error("Unexpected exception while determining mime type", e);
            }
            return "unknown";
        }

        @Override
        public long length() {
            try {
                return model.getProperty().getLength();
            } catch (RepositoryException e) {
                return -1;
            }
        }

        @Override
        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            try {
                is = model.getProperty().getBinary().getStream();
                return is;
            } catch (RepositoryException e) {
                throw new ResourceStreamNotFoundException(e);
            }
        }

        @Override
        public void close() throws IOException {
            if (is != null) {
                IOUtils.closeQuietly(is);
            }
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void setLocale(final Locale locale) {
        }

        @Override
        public Time lastModifiedTime() {
            try {
                final Node node = model.getProperty().getParent();
                return Time.valueOf(JcrUtils.getDateProperty(node, "jcr:lastModified", Calendar.getInstance()).getTime());
            } catch (RepositoryException e) {
                log.error("Unexpected exception while determining last modified date", e);
            }
            return Time.valueOf(new Date().getTime());
        }
    }
}
