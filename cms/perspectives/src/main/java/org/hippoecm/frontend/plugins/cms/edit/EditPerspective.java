/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.edit;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.editor.icon.EditorTabIconProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.frontend.validation.IValidationListener;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.frontend.widgets.UpdateFeedbackInfo;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends Perspective {

    static final Logger log = LoggerFactory.getLogger(EditPerspective.class);

    private FeedbackPanel feedback;
    private boolean feedbackShown;
    private Component icon;

    public EditPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(CssClass.append("hippo-editor"));
        add(CssClass.append("qa-editor"));

        feedback = new FeedbackPanel("feedback", message -> {
            final String serviceId = config.getString(RenderService.FEEDBACK);
            if (serviceId != null) {
                List<IFeedbackMessageFilter> filters = context.getServices(serviceId, IFeedbackMessageFilter.class);
                for (IFeedbackMessageFilter filter : filters) {
                    if (filter.accept(message)) {
                        return true;
                    }
                }
            }
            return false;
        });
        feedback.add(CssClass.append(new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return feedback.anyMessage() ? "hippo-shown" : "hippo-hidden";
            }
        }));
        feedback.setOutputMarkupId(true);
        add(feedback);

        if (config.containsKey(IValidationService.VALIDATE_ID)) {
            context.registerService(new IValidationListener() {
                public void onResolve(Set<Violation> violations) {
                }

                public void onValidation(IValidationResult result) {
                    AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        boolean hasMessage = false;
                        if (result != null && result.getViolations() != null){
                            hasMessage = !result.getViolations().isEmpty();
                        }
                        renderFeedbackIfNeeded(target, hasMessage);
                    }
                }

            }, config.getString(IValidationService.VALIDATE_ID));
        }
    }

    @Override
    public void onEvent(IEvent event) {
        // handle notified validation events from wicket fields
        if(event.getPayload() instanceof UpdateFeedbackInfo) {
            final UpdateFeedbackInfo ufi = (UpdateFeedbackInfo) event.getPayload();
            final AjaxRequestTarget target = ufi.getTarget();
            if (ufi.isClearAll()) {
                if (hasFeedbackMessage()) {
                    getFeedbackMessages().clear();
                }
                clearFeedbackOfChildComponents();
                if (target != null) {
                    target.add(feedback);
                }
            } else {
                renderFeedbackIfNeeded(target, feedback.anyMessage());
            }
        }
    }

    private void clearFeedbackOfChildComponents() {
        visitChildren(HippoForm.class, (object, visit) -> ((HippoForm)object).clearFeedbackMessages());
    }

    private void renderFeedbackIfNeeded(final AjaxRequestTarget target, final boolean hasFeedbackMessage) {
        if (target != null && isVisibleInHierarchy()) {
            // only render if there is any change in the feedback panel
            if (hasFeedbackMessage || feedbackShown) {
                target.add(feedback);
            }
            feedbackShown = hasFeedbackMessage;
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                JcrNodeModel nodeModel = getJcrNodeModel();
                IModel<String> nodeName = getDisplayName(nodeModel);
                if (nodeModel != null) {
                    Node node = nodeModel.getNode();
                    if (node != null) {
                        try {
                            if (node.isNodeType("nt:frozenNode")) {
                                Node versionNode = node.getParent();
                                Calendar calendar = versionNode.getProperty("jcr:created").getDate();
                                MessageFormat format = new MessageFormat("{0} {1,date} {1,time}", getLocale());
                                return format.format(new Object[] { nodeName.getObject(), calendar.getTime() });
                            }
                        } catch (ValueFormatException e) {
                            log.error("Value is not a date", e);
                        } catch (PathNotFoundException e) {
                            log.error("Could not find node", e);
                        } catch (RepositoryException e) {
                            log.error("Repository error", e);
                        }
                    }
                }
                return nodeName.getObject();
            }
        };
    }

    private JcrNodeModel getJcrNodeModel() {
        final JcrNodeModel defaultModel = (JcrNodeModel) EditPerspective.this.getDefaultModel();
        return Optional.ofNullable(defaultModel.getNode())
                .filter(this::isFrozenNode)
                .map(this::getFrozenUuid)
                .map(this::getNodeByIdentifier)
                .map(JcrNodeModel::new)
                .orElse(defaultModel);
    }

    private Node getNodeByIdentifier(final String uuid) {
        try {
            return getSession().getJcrSession().getNodeByIdentifier(uuid);
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    private String getFrozenUuid(final Node frozenNode) {
        try {
            return JcrUtils.getStringProperty(frozenNode, JcrConstants.JCR_FROZENUUID, frozenNode.getIdentifier());
        } catch (RepositoryException e) {
            log.warn(e.getMessage(),e );
        }
        return null;
    }

    private boolean isFrozenNode(final Node node) {
        try {
            return node.isNodeType(JcrConstants.NT_FROZENNODE);
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }

    private IModel<String> getDisplayName(JcrNodeModel model) {
        try {
            final IModel<String> result = DocumentUtils.getDocumentNameModel(model);
            if (result != null) {
                return result;
            }
        } catch (RepositoryException ignored) {
        }
        return Model.of(getString("unknown"));
    }

    @Override
    public String getMarkupId(boolean createIfDoesNotExist) {
        String wicketServiceId = getPluginContext().getReference(this).getServiceId() + "-edit-perspective";
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(wicketServiceId.getBytes(), 0, wicketServiceId.length());
            // use 'id' prefix to be compliant with w3c identifier specification
            return "id" + new BigInteger(1, m.digest()).toString(16);

        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Component getIcon(final String id, final IconSize size) {
        if (icon == null) {
            final EditorTabIconProvider iconProvider = new EditorTabIconProvider(getLocaleProvider());
            final JcrNodeModel nodeModel = getJcrNodeModel();
            icon = iconProvider.getIcon(nodeModel, id, size);
        }

        if (icon == null) {
            icon = super.getIcon(id, size);
        }

        return icon;
    }

    @Override
    protected void onDetach() {
        super.onDetach();

        if (icon != null) {
            icon.detach();
        }
    }

    protected ILocaleProvider getLocaleProvider() {
        final String defaultServiceId = ILocaleProvider.class.getName();
        final String serviceId = getPluginConfig().getString(ILocaleProvider.SERVICE_ID, defaultServiceId);
        return getPluginContext().getService(serviceId, ILocaleProvider.class);
    }
}
