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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.console.dialog.MultiStepDialog;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.jcrdiff.content.jcr.JcrTreeNode;
import org.onehippo.cms7.jcrdiff.delta.Operation;
import org.onehippo.cms7.jcrdiff.delta.Patch;
import org.onehippo.cms7.jcrdiff.patch.PatchLog;
import org.onehippo.cms7.jcrdiff.patch.Patcher;

public class ApplyPatchDialog extends MultiStepDialog<Node> {

    private static final long serialVersionUID = 1L;

    private final Label log;
    private final FileUploadField fileUploadField;
    private List<Step> steps;

    public ApplyPatchDialog(IModel<Node> model) {
        super(model);
        log = new Label("log");
        log.setOutputMarkupId(true);
        add(log);

        setMultiPart(true);
        add(fileUploadField = new FileUploadField("fileInput"));

    }

    private boolean uploadPatch() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload == null) {
            error("No file selected");
            return false;
        }
        StringBuilder sb = new StringBuilder();
        try {
            final Patch patch = parsePatch(upload);
            final String target = patch.getTarget();
            if (!getModelObject().getPath().equals(target)) {
                sb.append("The patch seems to be targeted at a different node than the one to which you are about to apply it.\n");
                sb.append("Patch is targeted at ").append(target).append("\n");
                sb.append("About to apply patch to ").append(getModelObject().getPath()).append("\n");
                sb.append("Continue?");
            }
            if (patch.getOperations().isEmpty()) {
                sb.append("The patch is empty");
            }
            return true;
        } catch (JAXBException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (IOException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } finally {
            String message = sb.toString();
            if (!message.isEmpty()) {
                log.setDefaultModel(new Model<String>(message));
                AjaxRequestTarget.get().addComponent(log);
            }
        }

        return false;
    }

    private Patch parsePatch(final FileUpload upload) throws JAXBException, IOException {
        final JAXBContext context = JAXBContext.newInstance(Patch.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return (Patch) unmarshaller.unmarshal(new InputStreamReader(upload.getInputStream()));
    }

    private boolean applyPatch() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload == null) {
            error("No file selected");
            return false;
        }

        final StringBuilder logMessage = new StringBuilder();
        try {
            final Patch patch = parsePatch(upload);
            final javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            final Node atticNode = session.getRootNode().addNode("hippo:patch-attic");
            final Patcher patcher = new Patcher(new JcrTreeNode(getModelObject()), new JcrTreeNode(atticNode));
            patcher.applyPatch(patch, new PatchLog() {
                @Override
                public void logOperation(final Operation action, final boolean success) {
                    logMessage.append("Action : " + action + ": success = " + success + "\n");
                }
            });
            atticNode.remove();
            return true;
        } catch (JAXBException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (IOException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } finally {
            log.setDefaultModel(new Model<String>(logMessage.toString()));
            AjaxRequestTarget.get().addComponent(log);
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
