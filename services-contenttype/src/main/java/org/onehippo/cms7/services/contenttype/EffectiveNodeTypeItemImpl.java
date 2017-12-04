/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services.contenttype;

public abstract class EffectiveNodeTypeItemImpl extends Sealable implements EffectiveNodeTypeItem {
    private final String name;
    private final String definingType;
    private final boolean nodeType;
    private final boolean residual;
    private boolean multiple;
    private boolean mandatory;
    private boolean autoCreated;
    private boolean protect;

    protected void doSeal() {
    }

    protected EffectiveNodeTypeItemImpl(String name, String definingType, boolean nodeType) {
        this.name = name;
        this.definingType = definingType;
        this.residual = "*".equals(name);
        this.nodeType = nodeType;
    }

    public boolean isNodeType() {
        return nodeType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDefiningType() {
        return definingType;
    }

    @Override
    public boolean isResidual() {
        return residual;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        checkSealed();
        this.multiple = multiple;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        checkSealed();
        this.mandatory = mandatory;
    }

    @Override
    public boolean isAutoCreated() {
        return autoCreated;
    }

    public void setAutoCreated(boolean autoCreated) {
        checkSealed();
        this.autoCreated = autoCreated;
    }

    @Override
    public boolean isProtected() {
        return protect;
    }

    public void setProtected(boolean protect) {
        checkSealed();
        this.protect = protect;
    }
}
