/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dev.updater;

import java.io.IOException;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.repository.util.JcrUtils;

public class UpdaterOutput extends Panel {

    public UpdaterOutput(final String id, final Component container, final boolean updating) {
        super(id);
        final Label output = new Label("output", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                final Object o = container.getDefaultModelObject();
                if (o != null) {
                    final Node node = (Node) o;
                    try {
                        final Binary fullLog = JcrUtils.getBinaryProperty(node, "hipposys:log", null);
                        if (fullLog != null) {
                            return IOUtils.toString(fullLog.getStream());
                        } else {
                            return JcrUtils.getStringProperty(node, "hipposys:logtail", "");
                        }
                    } catch (RepositoryException e) {
                        return "Cannot read log: " + e.getMessage();
                    } catch (IOException e) {
                        return "Cannot read log: " + e.getMessage();
                    }
                }
                return "";
            }
        });
        if (updating) {
            output.setOutputMarkupId(true);
            output.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
        }
        add(output);
    }

}
