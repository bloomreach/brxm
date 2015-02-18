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
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.frontend.validation.IValidationListener;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.Violation;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends Perspective {

    static final Logger log = LoggerFactory.getLogger(EditPerspective.class);

    private FeedbackPanel feedback;
    private boolean feedbackShown;

    public EditPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(CssClass.append("hippo-editor"));

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
                        renderFeedbackIfNeeded(target);
                    }
                }

            }, config.getString(IValidationService.VALIDATE_ID));
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        renderFeedbackIfNeeded(target);
    }

    private void renderFeedbackIfNeeded(final AjaxRequestTarget target) {
        if (target != null && isVisibleInHierarchy()) {
            if (!feedbackShown && feedback.anyMessage()) {
                feedbackShown = true;
                target.add(feedback);
            } else if (feedbackShown && !feedback.anyMessage()) {
                feedbackShown = false;
                target.add(feedback);
            }
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                JcrNodeModel nodeModel = (JcrNodeModel) EditPerspective.this.getDefaultModel();
                IModel<String> nodeName = new NodeTranslator(nodeModel).getNodeName();
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
    public ResourceReference getIcon(IconSize iconSize) {
        JcrNodeModel nodeModel = (JcrNodeModel) EditPerspective.this.getDefaultModel();
        if (nodeModel != null) {
            ILocaleProvider localeProvider = getLocaleProvider();
            if (localeProvider != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                            String localeName = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
                            for (HippoLocale locale : localeProvider.getLocales()) {
                                if (localeName.equals(locale.getName())) {
                                    return locale.getIcon(iconSize, LocaleState.EXISTS);
                                }
                            }
                            log.info("Locale '" + localeName + "' was not found in provider");
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Node " + node.getPath() + " is not translated");
                            }
                        }
                    } catch (RepositoryException e) {
                        log.error("Repository error while retrieving locale for edited document", e);
                    }
                }
            }
        }
        return super.getIcon(iconSize);
    }

    protected ILocaleProvider getLocaleProvider() {
        final String defaultServiceId = ILocaleProvider.class.getName();
        final String serviceId = getPluginConfig().getString(ILocaleProvider.SERVICE_ID, defaultServiceId);
        return getPluginContext().getService(serviceId, ILocaleProvider.class);
    }
}
