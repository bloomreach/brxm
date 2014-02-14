/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.rest;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.rest.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "message")
public class MessageRestful implements Restful {

    private boolean successMessage = true;

    public boolean isSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(final boolean successMessage) {
        this.successMessage = successMessage;
    }

    private static final long serialVersionUID = 1L;

    private DisplayEvent.DisplayType displayType;

    public MessageRestful() {
    }

    public MessageRestful(final String value) {

        this.value = value;
    }

    private String value;

    public MessageRestful(final String message, final DisplayEvent.DisplayType displayType) {
        this(message);
        this.displayType = displayType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public DisplayEvent.DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(final DisplayEvent.DisplayType displayType) {
        this.displayType = displayType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageRestful{");
        sb.append("value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
