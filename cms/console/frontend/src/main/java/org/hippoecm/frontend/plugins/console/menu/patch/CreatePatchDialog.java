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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.plugins.console.dialog.MultiStepDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.download.DownloadLink;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.onehippo.cms7.jcrdiff.JcrDiffException;
import org.onehippo.cms7.jcrdiff.content.jcr.JcrTreeNode;
import org.onehippo.cms7.jcrdiff.delta.Patch;
import org.onehippo.cms7.jcrdiff.match.Matcher;
import org.onehippo.cms7.jcrdiff.match.MatcherItemInfo;
import org.onehippo.cms7.jcrdiff.match.PatchFactory;
import org.onehippo.cms7.jcrdiff.patch.PatchFilter;
import org.onehippo.cms7.jcrdiff.serialization.PatchWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePatchDialog extends MultiStepDialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CreatePatchDialog.class);

    private static final String[] ILLEGAL_PATHS = new String[] {
            "/content",
            "/hippo:log",
            "/formdata",
            "/jcr:system",
            "/hippo:configuration/hippo:initialize",
            "/hippo:configuration/hippo:modules/brokenlinks/hippo:moduleconfig/hippo:request",
            "/hippo:configuration/hippo:update/hippo:queue",
            "/hippo:configuration/hippo:udpate/hippo:history"
    };

    private final Label diff;
    private String path;
    private List<Step> steps;

    public CreatePatchDialog(IModel<Node> model) {
        super(model);
        diff = new Label("diff");
        diff.setOutputMarkupId(true);
        add(diff);
        DownloadLink link = new PatchDownloadLink("download-link");
        link.add(new Label("download-link-text", "Download (or right click and choose \"Save as...\")"));
        add(link);
        try {
            path = getModelObject().getPath();
            if (!isPathValid(path)) {
                error("Creating a diff for path " + path + " is not supported");
                setOkEnabled(false);
                link.setEnabled(false);
            }
        } catch (RepositoryException e) {
            setOkEnabled(false);
        }
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Create Diff for " + path);
    }

    @Override
    protected List<Step> getSteps() {
        if (steps == null) {
            steps = new ArrayList<Step>(2);
            steps.add(new CreatePatchStep());
            steps.add(new DoneStep());
        }
        return steps;
    }

    private boolean isPathValid(String path) {
        if (path.equals("/")) {
            return false;
        }
        for (String illegalPath : ILLEGAL_PATHS) {
            if (path.startsWith(illegalPath)) {
                return false;
            }
        }
        return true;
    }

    private void createDiff(final Writer writer) {
        Session session = null;
        try {

            final ReferenceWorkspace referenceWorkspace = UserSession.get().getHippoRepository().getOrCreateReferenceWorkspace();
            if (referenceWorkspace == null) {
                error("This functionality is not available in your environment");
                return;
            }

            session = referenceWorkspace.login();

            if (!session.nodeExists("/hippo:configuration")) {
                referenceWorkspace.bootstrap();
            }

            if (!session.nodeExists(path)) {
                error("No node at " + path + " in reference repository");
            }

            final Patch patch = createPatch(getModelObject(), session.getNode(path));

            final PatchWriter patchWriter = new PatchWriter(patch, writer, UserSession.get().getJcrSession());
            patchWriter.writePatch();

            writer.write("\n\n");

        } catch (RepositoryException e) {
            final String message = "An unexpected error occurred while creating diff: " + e.getMessage();
            error(message);
            log.error(message, e);
        } catch (IOException e) {
            final String message = "An unexpected error occurred while creating diff: " + e.getMessage();
            error(message);
            log.error(message, e);
        } catch (JcrDiffException e) {
            final String message = "An unexpected error occurred while creating diff: " + e.getMessage();
            error(message);
            log.error(message, e);
        } catch (JAXBException e) {
            final String message = "An unexpected error occurred while creating diff: " + e.getMessage();
            error(message);
            log.error(message);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

    }

    private Patch createPatch(final Node currentNode, final Node referenceNode) throws JcrDiffException, JAXBException, IOException {

        final Matcher matcher = new Matcher();
        final MatcherItemInfo currentInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(currentNode));
        final MatcherItemInfo referenceInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(referenceNode));
        matcher.setSource(referenceInfo);
        matcher.setResult(currentInfo);
        matcher.match();

        final PatchFactory factory = new PatchFactory();
        return factory.createPatch(referenceInfo, currentInfo, new PatchFilter() {
            @Override
            public boolean isIncluded(final MatcherItemInfo info) {
                return isPathValid(info.getPath());
            }
        });
    }

    private class CreatePatchStep extends Step {

        private static final long serialVersionUID = 1L;

        @Override
        protected int execute() {
            final StringWriter writer = new StringWriter();
            createDiff(writer);
            diff.setDefaultModel(new Model<String>(writer.getBuffer().toString()));
            AjaxRequestTarget.get().addComponent(diff);
            return 1;
        }

        @Override
        protected IModel<String> getOkLabel() {
            return new Model<String>("Show Diff");
        }
    }

    private class PatchDownloadLink extends DownloadLink {

        private static final long serialVersionUID = 1L;

        private File tempFile;

        public PatchDownloadLink(String id) {
            super(id);
        }

        @Override
        protected String getFilename() {
            return "patch.txt";
        }

        @Override
        protected void onDownloadTargetDetach() {
            if (tempFile != null) {
                tempFile.delete();
            }
        }

        @Override
        protected InputStream getContent() {
            Writer writer = null;
            try {
                tempFile = File.createTempFile("patch-" + Time.now(), ".txt");
                writer = new FileWriter(tempFile);
                createDiff(writer);
                return new FileInputStream(tempFile);
            } catch (IOException e) {
                final String message = "IOException while creating patch: " + e.getMessage();
                error(message);
                log.error(message, e);
            } finally {
                IOUtils.closeQuietly(writer);
            }
            return null;
        }
    }
}
