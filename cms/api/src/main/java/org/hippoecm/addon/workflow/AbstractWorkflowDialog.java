/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated This class is deprecated since 13.1. Use the {@link WorkflowDialog} instead.
 */
@Deprecated
public abstract class AbstractWorkflowDialog<T> extends Dialog<T> {

    private static Logger log = LoggerFactory.getLogger(AbstractWorkflowDialog.class);

    private final IWorkflowInvoker invoker;

    public AbstractWorkflowDialog(IModel<T> model, IWorkflowInvoker invoker) {
        this(model, null, invoker);
    }

    public AbstractWorkflowDialog(IModel<T> model, IModel message, IWorkflowInvoker invoker) {
        super(model);
        this.invoker = invoker;

        Label notification = new Label("notification");
        if (message != null) {
            notification.setDefaultModel(message);
        } else {
            notification.setVisible(false);
        }
        add(notification);
        notification.add(CssClass.append("notification"));
    }

    @Override
    protected void onOk() {
        try {
            invoker.invokeWorkflow();
        } catch (WorkflowSNSException e) {
            log.warn("Could not execute workflow due to same-name-sibling issue: " + e.getMessage());
            handleExceptionTranslation(e, e.getConflictingName());
        } catch (WorkflowException e) {
            log.warn("Could not execute workflow: " + e.getMessage());
            handleExceptionTranslation(e);
        } catch (AccessDeniedException e) {
            log.warn("Access denied: " + e.getMessage());
            handleExceptionTranslation(e);
        }
        catch (Exception e) {
            log.error("Could not execute workflow.", e);
            error(e);
        }
    }

    private void handleExceptionTranslation(final Throwable e, final Object... parameters) {
        List<String> errors = new ArrayList<>();
        Throwable t = e;
        while(t != null) {
            final String translatedMessage = getExceptionTranslation(t, parameters).getObject();
            if (translatedMessage != null && !errors.contains(translatedMessage)) {
                errors.add(translatedMessage);
            }
            t = t.getCause();
        }
        if (log.isDebugEnabled()) {
            log.debug("Exception caught: {}", StringUtils.join(errors.toArray(), ";"), e);
        }

        errors.stream().forEach(this::error);
    }
}
