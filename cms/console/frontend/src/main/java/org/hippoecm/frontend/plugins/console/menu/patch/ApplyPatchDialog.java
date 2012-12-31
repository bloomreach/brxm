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
package org.hippoecm.frontend.plugins.console.menu.patch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.plugins.console.dialog.MultiStepDialog;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.jcrdiff.content.jcr.JcrTreeNode;
import org.onehippo.cms7.jcrdiff.delta.Operation;
import org.onehippo.cms7.jcrdiff.delta.Patch;
import org.onehippo.cms7.jcrdiff.patch.PatchLog;
import org.onehippo.cms7.jcrdiff.patch.Patcher;
import org.onehippo.cms7.jcrdiff.serialization.PatchReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyPatchDialog extends MultiStepDialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ApplyPatchDialog.class);

    private final TextArea<String> textArea;
    private final FileUploadField fileUploadField;
    private List<Step> steps;
    private File tempFile;

    public ApplyPatchDialog(IModel<Node> model) {
        super(model);
        textArea = new TextArea<String>("log", new Model<String>());
        textArea.setOutputMarkupId(true);
        add(textArea);

        setMultiPart(true);
        add(fileUploadField = new FileUploadField("fileInput"));
        fileUploadField.setOutputMarkupId(true);

    }

    private boolean uploadPatch() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload == null) {
            error("No file selected");
            return false;
        }

        boolean result = false;
        InputStream uis = null, fis = null;
        OutputStream fos = null;
        final StringBuilder sb = new StringBuilder();
        try {
            uis = upload.getInputStream();
            tempFile = File.createTempFile("patch-" + Time.now(), ".xml");
            fos = new FileOutputStream(tempFile);
            IOUtils.copy(uis, fos);
            fis = new FileInputStream(tempFile);
            final Patch patch = parsePatch(fis);
            final String target = patch.getTarget();
            if (target != null && !target.equals(getModelObject().getPath())) {
                sb.append("The patch seems to be targeted at a different node than the one to which you are about to apply it.");
                sb.append(" Patch is targeted at ").append(target).append(".");
                sb.append(" About to apply patch to ").append(getModelObject().getPath()).append(".");
                sb.append(" Continue?");
            }
            if (patch.getOperations().isEmpty()) {
                sb.append("The patch is empty.");
            }
            result = true;
        } catch (IOException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        } catch (RepositoryException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        } finally {
            IOUtils.closeQuietly(uis);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(fis);
            final String message = sb.toString();
            if (!message.isEmpty()) {
                info(message);
            }
            if (result) {
                try {
                    fis = new FileInputStream(tempFile);
                    textArea.setDefaultModelObject(new String(IOUtils.toCharArray(fis)));
                    AjaxRequestTarget.get().addComponent(textArea);
                } catch (IOException e) {
                    log.error(e.getClass().getName() + ": " + e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
                fileUploadField.setEnabled(false);
                AjaxRequestTarget.get().addComponent(fileUploadField);
            }
        }

        return result;
    }

    private Patch parsePatch(final InputStream is) throws IOException, RepositoryException {
        final PatchReader patchReader = new PatchReader(new InputStreamReader(is), UserSession.get().getJcrSession());
        return patchReader.readPatch();
    }

    private boolean applyPatch() {
        final StringBuilder logMessage = new StringBuilder();
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(textArea.getModelObject().getBytes());
            final Patch patch = parsePatch(bais);
            final Session session = UserSession.get().getJcrSession();
            final Node atticNode = session.getRootNode().addNode("hippo:patch-attic");
            final Patcher patcher = new Patcher(new JcrTreeNode(getModelObject()), new JcrTreeNode(atticNode));
            final MyPatchLog patchLog = new MyPatchLog(logMessage);
            patcher.applyPatch(patch, patchLog);
            atticNode.remove();
            if (patchLog.errors) {
                warn("Some patch operations failed");
            }
            return true;
        }  catch (RepositoryException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message, e);
        } catch (IOException e) {
            final String message = "An unexpected error occurred: " + e.getMessage();
            error(message);
            log.error(message);
        } finally {
            IOUtils.closeQuietly(bais);
            textArea.setDefaultModel(new Model<String>(logMessage.toString()));
            AjaxRequestTarget.get().addComponent(textArea);
        }
        return false;
    }

    @Override
    protected List<Step> getSteps() {
        if (steps == null) {
            steps = new ArrayList<Step>(3);
            steps.add(new UploadPatchStep());
            steps.add(new ApplyPatchStep());
            steps.add(new DoneStep());
        }
        return steps;
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Apply patch");
    }

    private static class MyPatchLog implements PatchLog {
        private final StringBuilder logMessage;

        private boolean errors = false;

        public MyPatchLog(final StringBuilder logMessage) {
            this.logMessage = logMessage;
        }

        @Override
        public void logOperation(final Operation action, final boolean success) {
            errors |= !success;
            logMessage.append("Action : ").append(action).append(": success = ").append(success).append("\n");
        }

    }

    private class UploadPatchStep extends Step {

        private static final long serialVersionUID = 1L;

        @Override
        protected int execute() {
            return uploadPatch() ? 1 : 0;
        }

        @Override
        protected IModel<String> getOkLabel() {
            return new Model<String>("Upload patch");
        }

    }

    private class ApplyPatchStep extends Step {

        private static final long serialVersionUID = 1L;

        @Override
        protected int execute() {
            return applyPatch() ? 1 : 0;
        }

        @Override
        protected IModel<String> getOkLabel() {
            return new Model<String>("Apply patch");
        }
    }
}
