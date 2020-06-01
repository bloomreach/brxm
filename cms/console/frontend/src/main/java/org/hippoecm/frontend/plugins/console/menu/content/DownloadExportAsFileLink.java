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
package org.hippoecm.frontend.plugins.console.menu.content;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.widgets.download.DownloadLink;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadExportAsFileLink extends DownloadLink<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DownloadExportAsFileLink.class);

    private IModel<Boolean> skipBinaryModel;
    private File tempFile;

    public DownloadExportAsFileLink(String id, IModel<Node> model, IModel<Boolean> skipBinaryModel) {
        super(id, model);
        this.skipBinaryModel = skipBinaryModel;
    }

    @Override
    protected String getFilename() {
        try {
            return NodeNameCodec.decode(getModel().getObject().getName()) + ".xml";
        } catch (RepositoryException e) {
            final String message = "Unable to get node name for file name, using default";
            log.error(message, e);
            error(message);
        }
        return null;
    }

    @Override
    protected void onDownloadTargetDetach() {
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    @Override
    protected InputStream getContent() {
        try {
            tempFile = File.createTempFile("export-" + Time.now().toString() + "-", ".xml");
            FileOutputStream fos = new FileOutputStream(tempFile);
            try {
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                try {
                    boolean skipBinary = skipBinaryModel.getObject();
                    ((HippoSession)getModelObject().getSession()).exportDereferencedView(getModelObject().getPath(), bos, skipBinary, false);
                } finally {
                    bos.close();
                }
            } finally {
                fos.close();
            }
            return new FileInputStream(tempFile);
        } catch (FileNotFoundException e) {
            final String message = "Tempfile missing during export";
            error(message);
            log.error(message, e);
        } catch (IOException e) {
            final String message = "IOException during export";
            error(message);
            log.error(message, e);
        } catch (RepositoryException e) {
            final String message = "Repository exception during export: " + e.getMessage();
            error(message);
            log.error(message, e);
        }
        return null;
    }

}
