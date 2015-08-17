/*
 *  Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterTypeCheckingExtension extends AbstractTypeCheckingExtension {

    private static final Logger log = LoggerFactory.getLogger(UpdaterTypeCheckingExtension.class);

    private static final Collection<Package> defaultIllegalPackages = new ArrayList<Package>() {{
        add(Package.getPackage("java.nio.file"));
        add(Package.getPackage("java.net"));
        add(Package.getPackage("javax.net.ssl"));
        add(Package.getPackage("java.lang.reflect"));
    }};

    private static final Collection<Class> defaultIllegalClasses = new ArrayList<Class>() {{
        add(Runtime.class);
        add(ProcessBuilder.class);
        add(File.class);
        add(FileDescriptor.class);
        add(FileInputStream.class);
        add(FileOutputStream.class);
        add(FileWriter.class);
        add(FileReader.class);
    }};

    private static final Collection<Method> defaultIllegalMethods = new ArrayList<Method>() {{
        try {
            add(System.class.getMethod("exit", int.class));
            add(Class.class.getMethod("forName", String.class));
            add(Class.class.getMethod("forName", String.class, boolean.class, ClassLoader.class));
        } catch (NoSuchMethodException e) {
            log.error("Failed to initialize default illegal methods", e);
        }
    }};

    private Set<Package> illegalPackages;
    private Set<Class> illegalClasses;
    private Set<Method> illegalMethods;

    public UpdaterTypeCheckingExtension(final StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    @Override
    public void onMethodSelection(final Expression expression, final MethodNode target) {
        final Class declaringClass = target.getDeclaringClass().getTypeClass();
        if (getIllegalClasses().contains(declaringClass)) {
            addStaticTypeError("Method call is not allowed: using illegal class", expression);
        }
        final Package declaringPackage = declaringClass.getPackage();
        if (getIllegalPackages().contains(declaringPackage)) {
            addStaticTypeError("Method call is not allowed: using illegal package", expression);
        }
        for (Method method : getIllegalMethods()) {
            if (method.getDeclaringClass().equals(declaringClass)) {
                if (target.getName().equals(method.getName())) {
                    addStaticTypeError("Method call is not allowed", expression);
                }
            }
        }
    }

    private Collection<Package> getIllegalPackages() {
        if (illegalPackages == null) {
            illegalPackages = new HashSet<>(defaultIllegalPackages);
            illegalPackages.addAll(UpdaterExecutor.getConfiguration().getIllegalPackages());
        }
        return illegalPackages;
    }

    private Collection<Class> getIllegalClasses() {
        if (illegalClasses == null) {
            illegalClasses = new HashSet<>(defaultIllegalClasses);
            illegalClasses.addAll(UpdaterExecutor.getConfiguration().getIllegalClasses());
        }
        return illegalClasses;
    }

    private Collection<Method> getIllegalMethods() {
        if (illegalMethods == null) {
            illegalMethods = new HashSet<>(defaultIllegalMethods);
            illegalMethods.addAll(UpdaterExecutor.getConfiguration().getIllegalMethods());
        }
        return illegalMethods;
    }
}
