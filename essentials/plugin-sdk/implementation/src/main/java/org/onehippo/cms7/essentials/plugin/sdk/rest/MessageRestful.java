/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.rest;

import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModel;

import org.onehippo.cms7.essentials.plugin.sdk.instructions.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.model.Restful;

@ApiModel
@XmlRootElement(name = "message")
public class MessageRestful implements Restful {

    private boolean successMessage = true;
    private boolean globalMessage = true;

    public boolean isSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(final boolean successMessage) {
        this.successMessage = successMessage;
    }

    private static final long serialVersionUID = 1L;

    private Instruction.Type group;

    public MessageRestful() {
    }

    public MessageRestful(final String value) {

        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }


    public boolean isGlobalMessage() {
        return globalMessage;
    }

    public void setGlobalMessage(final boolean globalMessage) {
        this.globalMessage = globalMessage;
    }

    public Instruction.Type getGroup() {
        return group;
    }

    public void setGroup(final Instruction.Type group) {
        this.group = group;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageRestful{");
        sb.append("value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
