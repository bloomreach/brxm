/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.console.dialog.BinaryUploadDialog;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryEditor extends Panel {

    private static final Logger log = LoggerFactory.getLogger(BinaryEditor.class);

    private static final long ONE_KB = 1024;
    private static final long ONE_MB = ONE_KB * ONE_KB;
    private static final long ONE_GB = ONE_KB * ONE_MB;

    public BinaryEditor(String id, JcrPropertyModel model, final IPluginContext pluginContext) {
        super(id);
        final IResourceStream stream = new BinaryResourceStream(model);

        // download
        final ResourceStreamResource resource = new ResourceStreamResource(stream);
        try {
            final Node node = model.getProperty().getParent().getParent();
            final StringBuilder fileName = new StringBuilder(node.getName());
            if (isExtractedTextProperty(model.getProperty())) {
                fileName.append(".txt");
            }
            resource.setFileName(fileName.toString());
        } catch (RepositoryException e) {
            log.error("Unexpected exception while determining download filename", e);
        }
        final Link downloadLink = new ResourceLink("binary-download-link", resource);
        downloadLink.add(new Label("binary-download-text", "download (" + getSizeString(stream.length()) + ")"));
        add(downloadLink);

        // upload
        IDialogFactory factory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public IDialogService.Dialog createDialog() {
                return new BinaryUploadDialog(model);
            }
        };
        final IDialogService service = pluginContext.getService(IDialogService.class.getName(), IDialogService.class);
        final DialogLink uploadLink = new DialogLink("binary-upload-link", new Model<>("Upload binary"), factory, service);
        add(uploadLink);
    }

    private static String getSizeString(final Bytes bytes) {
        if (bytes == null) {
            return "unknown size";
        }
        long length = bytes.bytes();
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

    private static boolean isExtractedTextProperty(Property property) {
        try {
            return property.getName().equals(HippoNodeType.HIPPO_TEXT) && isPartOfHippoDocument(property);
        } catch (RepositoryException e) {
            log.error("Unexpected exception while determining whether property contains extracted text", e);
            return false;
        }
    }

    private static boolean isPartOfHippoDocument(final Property property) throws RepositoryException {
        final Node root = property.getSession().getRootNode();
        Node current = property.getParent();
        while (!current.isSame(root)) {
            Node parent = current.getParent();
            if (parent.isNodeType(HippoNodeType.NT_HANDLE) && current.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                return true;
            }
            current = parent;
        }
        return false;
    }

    private static class BinaryResourceStream extends AbstractResourceStream {

        private transient InputStream is;
        private JcrPropertyModel model;

        private BinaryResourceStream(JcrPropertyModel model) {
            this.model = model;
        }

        @Override
        public String getContentType() {
            try {
                if (BinaryEditor.isExtractedTextProperty(model.getProperty())) {
                    return "text/plain";
                } else {
                    final Node node = model.getProperty().getParent();
                    return JcrUtils.getStringProperty(node, "jcr:mimeType", "unknown");
                }
            } catch (RepositoryException e) {
                log.error("Unexpected exception while determining mime type", e);
            }
            return "unknown";
        }

        @Override
        public Bytes length() {
            try {
                return Bytes.bytes(model.getProperty().getLength());
            } catch (RepositoryException e) {
                return null;
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
            return Time.valueOf(new Date());
        }
    }
}
