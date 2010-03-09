/*
 *  Copyright 2008 Hippo.
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

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.UnitSettings;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.validation.IValidationListener;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(EditPerspective.class);

    private static final long serialVersionUID = 1L;

    private String topHeight;
    private WireframeSettings wfSettings;
    private FeedbackPanel feedback;
    private boolean feedbackShown;

    public EditPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        feedback = new FeedbackPanel("feedback", new IFeedbackMessageFilter() {
            private static final long serialVersionUID = 1L;

            public boolean accept(FeedbackMessage message) {
                if (config.getString(RenderService.FEEDBACK) != null) {
                    List<IFeedbackMessageFilter> filters = context.getServices(
                            config.getString(RenderService.FEEDBACK), IFeedbackMessageFilter.class);
                    for (IFeedbackMessageFilter filter : filters) {
                        if (filter.accept(message)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        feedback.setOutputMarkupId(true);
        add(feedback);
        feedbackShown = false;

        if (config.containsKey(IValidationService.VALIDATE_ID)) {
            context.registerService(new IValidationListener() {
                private static final long serialVersionUID = 1L;

                public void onResolve(Set<Violation> violations) {
                }

                public void onValidation(IValidationResult result) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(feedback);
                    }
                }

            }, config.getString(IValidationService.VALIDATE_ID));
        }

        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig == null) {
            wfConfig = new JavaPluginConfig();
        }
        wfSettings = new WireframeSettings(wfConfig);
        UnitSettings topSettings = wfSettings.getUnit("top");
        topHeight = topSettings.getHeight();
        add(new WireframeBehavior(wfSettings));
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        boolean hasMessage = feedback.anyMessage();
        UnitSettings topSettings = wfSettings.getUnit("top");
        boolean updateTop = false;
        if (hasMessage && !feedbackShown) {
            topSettings.setHeight(Integer.valueOf(
                    getPluginConfig().getAsInteger("feedback.height", 50) + Integer.parseInt(topHeight)).toString());
            feedbackShown = true;
            updateTop = true;
        } else if (!hasMessage && feedbackShown) {
            topSettings.setHeight(topHeight);
            feedbackShown = false;
            updateTop = true;
        }
        if (updateTop && isVisibleInHierarchy()) {
            String topId = topSettings.getId().getElementId();
            target.appendJavascript("YAHOO.hippo.LayoutManager.findLayoutUnit(YAHOO.util.Dom.get('" + topId
                    + "')).set('height', " + topSettings.getHeight() + ");");
            target.addComponent(feedback);
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

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

}
