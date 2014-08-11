/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.code;

import java.io.Serializable;

import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * @version "$Id: EssentialsGeneratedMethod.java 173277 2013-08-09 10:06:29Z mmilicevic $"
 */
public class EssentialsGeneratedMethod implements Serializable {

    private static final long serialVersionUID = 1L;
    private String methodName;
    private String returnType;
    private String internalName;
    private boolean multiType;
    private MethodDeclaration methodDeclaration;

    public EssentialsGeneratedMethod(final MethodDeclaration methodDeclaration, final String methodName, final String internalName) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.internalName = internalName;
    }

    public EssentialsGeneratedMethod() {

    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public void setMethodDeclaration(final MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(final String internalName) {
        this.internalName = internalName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(final String returnType) {
        this.returnType = returnType;
    }

    public boolean isMultiType() {
        return multiType;
    }

    public void setMultiType(final boolean multiType) {
        this.multiType = multiType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EssentialsGeneratedMethod{");
        sb.append("methodName='").append(methodName).append('\'');
        sb.append(", returnType='").append(returnType).append('\'');
        sb.append(", internalName='").append(internalName).append('\'');
        sb.append(", methodDeclaration=").append(methodDeclaration);
        sb.append('}');
        return sb.toString();
    }
}
