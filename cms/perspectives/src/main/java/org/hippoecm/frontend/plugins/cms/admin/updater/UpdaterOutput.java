/*
 * Copyright 2012-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.updater;

import java.io.IOException;
import java.time.Duration;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.update.UpdaterExecutionReport;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_LOG;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_LOGTAIL;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_LOGTARGET;

public class UpdaterOutput extends Panel {

    public UpdaterOutput(final String id, final Component container, final boolean updating) {
        super(id);

        final Label output;
        final String logs = parseOutput(container.getDefaultModelObject());
        // For BC it is not enough to test eg UpdaterExecutionReport#isPersistLogsSupported or the log target as older
        // history nodes do not have the log target, therefore, just check if there are logs and if not, mention logs are
        // written to normal log files
        if (StringUtils.isEmpty(logs)) {
            output = new Label("output","Logs are stored in normal log files for logger '" +
                    UpdaterExecutionReport.class.getName() + "' and not visible here");
        } else {
            output = new Label("output", () -> logs);
        }
        if (updating) {
            output.setOutputMarkupId(true);
            output.add(new AjaxSelfUpdatingTimerBehavior(Duration.ofSeconds(5)));
        }
        add(output);
    }

    private static String parseOutput(final Object o) {
        if (!(o instanceof Node)) {
            return StringUtils.EMPTY;
        }

        final Node node = (Node) o;
        try {
            final Binary fullLog = JcrUtils.getBinaryProperty(node, HIPPOSYS_LOG, null);
            if (fullLog != null) {
                return IOUtils.toString(fullLog.getStream());
            } else {
                return JcrUtils.getStringProperty(node, HIPPOSYS_LOGTAIL, "");
            }
        } catch (RepositoryException | IOException e) {
            return "Cannot read log: " + e.getMessage();
        }
    }

}
