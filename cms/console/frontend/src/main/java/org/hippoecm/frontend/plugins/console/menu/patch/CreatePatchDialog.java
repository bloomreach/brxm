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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.MultiStepDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.onehippo.cms7.jcrdiff.JcrDiffException;
import org.onehippo.cms7.jcrdiff.content.jcr.JcrTreeNode;
import org.onehippo.cms7.jcrdiff.delta.Patch;
import org.onehippo.cms7.jcrdiff.match.Matcher;
import org.onehippo.cms7.jcrdiff.match.MatcherItemInfo;
import org.onehippo.cms7.jcrdiff.match.PatchFactory;

public class CreatePatchDialog extends MultiStepDialog<Node> {

    private static final String[] ILLEGAL_PATHS = new String[] {
            "/content",
            "/hippo:log",
            "/formdata",
            "/jcr:system"
    };

    private final Label diff;
    private String path;
    private List<Step> steps;

    public CreatePatchDialog(IModel<Node> model) {
        super(model);
        diff = new Label("diff");
        diff.setOutputMarkupId(true);
        add(diff);
        try {
            path = getModelObject().getPath();
            if (!isPathValid(path)) {
                error("Creating a diff for path " + path + " is not supported");
                setOkEnabled(false);
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

    private String createDiff() {
        javax.jcr.Session session = null;
        try {

            final ReferenceWorkspace referenceWorkspace = UserSession.get().getHippoRepository().getOrCreateReferenceWorkspace();
            session = referenceWorkspace.getSession();

            if (!session.nodeExists("/hippo:configuration")) {
                referenceWorkspace.bootstrap();
            }

            // check if reference node exists
            if (!session.nodeExists(path)) {
                error("No node at " + path + " in reference repository");
                return "";
            }

            final Patch patch = createPatch(getModelObject(), session.getNode(path));

            final JAXBContext context = JAXBContext.newInstance(Patch.class);
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final StringWriter writer = new StringWriter();
            marshaller.marshal(patch, writer);
            return writer.getBuffer().toString();

        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (IOException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (JcrDiffException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (JAXBException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        return null;
    }

    private Patch createPatch(final Node currentNode, final Node referenceNode) throws JcrDiffException, JAXBException, IOException {

        final Matcher matcher = new Matcher();
        final MatcherItemInfo currentInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(currentNode));
        final MatcherItemInfo cleanInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(referenceNode));
        matcher.setSource(cleanInfo);
        matcher.setResult(currentInfo);
        matcher.match();

        final PatchFactory factory = new PatchFactory();
        return factory.createPatch(cleanInfo, currentInfo);
    }

    private class CreatePatchStep extends Step {

        @Override
        protected int execute() {
            diff.setDefaultModel(new Model<String>(createDiff()));
            AjaxRequestTarget.get().addComponent(diff);
            return 1;
        }

        @Override
        protected IModel<String> getOkLabel() {
            return new Model<String>("Show Diff");
        }
    }
}
