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

package org.onehippo.cms7.essentials.dashboard.utils.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for parsing annotations and bean properties (fields/methods)
 *
 * @version $Id$
 */
public final class AnnotationUtils {

    private static Logger log = LoggerFactory.getLogger(AnnotationUtils.class);

    //############################################
    // SOURCE UTILS
    //############################################


    @SuppressWarnings("unchecked")
    public static String addXmlRootAnnotation(final String source, final String name) {

        final CompilationUnit unit = JavaSourceUtils.getCompilationUnit(source);
        final AST ast = unit.getAST();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final NormalAnnotation xmlRootAnnotation = ast.newNormalAnnotation();
        xmlRootAnnotation.setTypeName(ast.newName(XmlRootElement.class.getSimpleName()));
        // name
        final MemberValuePair generatedMemberValue = ast.newMemberValuePair();
        generatedMemberValue.setName(ast.newSimpleName("name"));
        final StringLiteral internalNameLiteral = ast.newStringLiteral();
        internalNameLiteral.setLiteralValue(name);
        generatedMemberValue.setValue(internalNameLiteral);
        xmlRootAnnotation.values().add(generatedMemberValue);
        JavaSourceUtils.addAnnotation(classType, xmlRootAnnotation);
        JavaSourceUtils.addImport(unit, XmlRootElement.class.getName());
        return JavaSourceUtils.rewrite(unit, ast);

    }

    public static String addXmlAdaptorAnnotation(final String source, final Class<?> returnType, final AdapterWrapper wrapper) {

        final CompilationUnit unit = JavaSourceUtils.getCompilationUnit(source);
        final AST ast = unit.getAST();
        final ExistingMethodsVisitor methodsVisitor = JavaSourceUtils.getMethodCollection(unit);
        final List<MethodDeclaration> getterMethods = methodsVisitor.getGetterMethods();
        boolean needsImport = false;
        final String ourReturnType = returnType.getSimpleName();
        for (MethodDeclaration getterMethod : getterMethods) {
            final Type returnType2 = getterMethod.getReturnType2();
            if (returnType2.isSimpleType()) {
                final SimpleType simpleType = (SimpleType) returnType2;
                if (!simpleType.getName().getFullyQualifiedName().equals(ourReturnType)) {
                    continue;
                }
            } else {
                log.warn("TODO: Cannot map type: {}", returnType2);
                continue;
            }
            log.info(ourReturnType);
            @SuppressWarnings("rawtypes")
            final List parameters = getterMethod.parameters();
            if (parameters == null || parameters.size() == 0) {
                final SingleMemberAnnotation generatedAnnotation = ast.newSingleMemberAnnotation();
                generatedAnnotation.setTypeName(ast.newName("XmlJavaTypeAdapter"));
                // name
                final TypeLiteral typeLiteral = ast.newTypeLiteral();
                typeLiteral.setType(ast.newSimpleType(ast.newName(wrapper.className)));
                generatedAnnotation.setValue(typeLiteral);
                JavaSourceUtils.addAnnotation(getterMethod, generatedAnnotation);
                needsImport = true;
            }
        }
        if (needsImport) {
            JavaSourceUtils.addImport(unit, wrapper.importPath);
        }

        return JavaSourceUtils.rewrite(unit, ast);

    }

    public static String addXmlElementAnnotation(final String source) {

        final CompilationUnit unit = JavaSourceUtils.getCompilationUnit(source);
        final AST ast = unit.getAST();
        final ExistingMethodsVisitor methodsVisitor = JavaSourceUtils.getMethodCollection(unit);
        final List<MethodDeclaration> getterMethods = methodsVisitor.getGetterMethods();
        boolean needsImport = false;
        for (MethodDeclaration getterMethod : getterMethods) {
            @SuppressWarnings("rawtypes")
            final List parameters = getterMethod.parameters();
            if (parameters == null || parameters.size() == 0) {
                // annotate getter:
                final MarkerAnnotation xmlElementAnnotation = ast.newMarkerAnnotation();
                xmlElementAnnotation.setTypeName(ast.newSimpleName(XmlElement.class.getSimpleName()));
                JavaSourceUtils.addAnnotation(getterMethod, xmlElementAnnotation);
                needsImport = true;
            }
        }
        if (needsImport) {
            JavaSourceUtils.addImport(unit, XmlElement.class.getName());
        }

        return JavaSourceUtils.rewrite(unit, ast);

    }

    /**
     * For given java source, add {@code @XmlAccessorType(XmlAccessType.NONE)} class annotation
     *
     * @param source
     * @see XmlAccessorType
     * @see XmlAccessType#NONE
     */
    public static String addXmlAccessNoneAnnotation(final String source) {

        final CompilationUnit unit = JavaSourceUtils.getCompilationUnit(source);
        if (unit.types() == null || unit.types().size() < 1) {
            log.warn("Invalid java file");
            return source;
        }
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        // create annotation
        final SingleMemberAnnotation xmlAccessAnnotation = ast.newSingleMemberAnnotation();
        xmlAccessAnnotation.setTypeName(ast.newSimpleName(XmlAccessorType.class.getSimpleName()));
        // add annotation value
        final FieldAccess value = ast.newFieldAccess();
        value.setExpression(ast.newName(XmlAccessType.class.getSimpleName()));
        value.setName(ast.newSimpleName("NONE"));
        xmlAccessAnnotation.setValue(value);
        // rewrite & add imports:

        JavaSourceUtils.addImport(unit, XmlAccessType.class.getName());
        JavaSourceUtils.addImport(unit, XmlAccessorType.class.getName());
        JavaSourceUtils.addAnnotation(classType, xmlAccessAnnotation);
        return JavaSourceUtils.rewrite(unit, ast);
    }


    //############################################
    // BINARY STUFF
    //############################################


    /**
     * Return annotation of specific type
     *
     * @param clazz           class file we are scanning
     * @param annotationClass annotation type we are interested in
     * @param <T>             Annotation type
     * @return null if no annotation found, annotation otherwise
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> clazz, final Class<T> annotationClass) {
        if (!clazz.isAnnotationPresent(annotationClass)) {
            return null;
        }
        return clazz.getAnnotation(annotationClass);
    }

    /**
     * Get fields of an class which are annotated with specific
     * annotation and set them accessible (if necessary)
     *
     * @param clazz           class we are scanning for annotated fields.
     * @param annotationClass annotation we are interested in
     * @return a collection containing (accessible) fields we have found (or an empty collection)
     */
    public static Collection<Field> getAnnotatedFields(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        Collection<Field> fields = getClassFields(clazz);
        Iterator<Field> iterator = fields.iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            if (!field.isAnnotationPresent(annotationClass)) {
                iterator.remove();
            } else if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (SecurityException se) {
                    log.error(MessageFormat.format("Security exception while setting accessible: {0}", se));
                }
            }
        }
        return fields;
    }

    /**
     * Find a class for given name
     *
     * @param name of the class
     * @return null if not found or Class if found
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> findClass(final String name) {
        try {
            return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            log.error("No class found within class loader " + e);
        }
        return null;
    }

    /**
     * Get fields for given class.
     *
     * @param clazz class to scan for fields
     * @return collection of Fields
     */
    public static Collection<Field> getClassFields(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();
        Class<?> myClass = clazz;
        for (; myClass != null; ) {
            for (Field field : myClass.getDeclaredFields()) {
                if (!fields.containsKey(field.getName())) {
                    fields.put(field.getName(), field);
                }
            }
            myClass = myClass.getSuperclass();
        }

        return fields.values();
    }

    /**
     * Scans class for declared methods
     *
     * @param clazz class we are interested in
     * @return collection of declared methods
     */
    public static Collection<Method> getMethods(Class<?> clazz) {
        Map<String, Method> returnValue = new HashMap<>();
        Class<?> myClass = clazz;
        for (; myClass != null; ) {
            for (Method method : myClass.getDeclaredMethods()) {
                boolean isOverridden = false;
                for (Method overriddenMethod : returnValue.values()) {
                    if (overriddenMethod.getName().equals(method.getName()) && Arrays.deepEquals(method.getParameterTypes(), overriddenMethod.getParameterTypes())) {
                        isOverridden = true;
                        break;
                    }
                }
                if (!isOverridden) {
                    returnValue.put(method.getName(), method);
                }
            }
            myClass = myClass.getSuperclass();
        }
        return returnValue.values();
    }


    public static class AdapterWrapper {
        private final String importPath;
        private final String className;

        public AdapterWrapper(final String importPath, final String className) {
            this.importPath = importPath;
            this.className = className;
        }
    }

    private AnnotationUtils() {
    }
}

