/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
