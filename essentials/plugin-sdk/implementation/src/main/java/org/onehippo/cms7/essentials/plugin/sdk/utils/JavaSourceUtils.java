/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.onehippo.cms7.essentials.plugin.sdk.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.plugin.sdk.utils.beansmodel.MemoryBean;
import org.onehippo.cms7.essentials.plugin.sdk.utils.code.EssentialsGeneratedMethod;
import org.onehippo.cms7.essentials.plugin.sdk.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.essentials.plugin.sdk.utils.code.exc.EssentialsCodeCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * Utility class for manipulating java source files.
 *
 */
public final class JavaSourceUtils {

    private static final Pattern DOT_SPLITTER = Pattern.compile("\\.");
    /**
     * Pattern for replacing return type in case of array, e.g. {@code String []} or {@code String [  ]}
     */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[\\s*\\]");
    public static final String UNCHECKED = "unchecked";
    public static final String RAWTYPES = "rawtypes";
    public static final String TAB_SIZE = "4";
    private static final String GET_LINKED_BEANS = "getLinkedBeans";
    private static final String HIPPO_BEAN = "HippoBean";
    private static final String GET_LINKED_BEAN = "getLinkedBean";
    private static final String GET_BEAN = "getBean";
    private static final String HIPPO_GALLERY_IMAGE_SET = "HippoGalleryImageSet";
    private static final String ANNOTATION_ALREADY_EXISTS = "Annotation already exists: {}";
    private static final String GET_CHILD_BEANS_BY_NAME = "getChildBeansByName";
    
    private static Logger log = LoggerFactory.getLogger(JavaSourceUtils.class);

    private JavaSourceUtils() {
    }

    public static boolean deleteMethod(final EssentialsGeneratedMethod method, final Path path) {
        final CompilationUnit deleteUnit = getCompilationUnit(path);
        final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(path);
        final List<EssentialsGeneratedMethod> generatedMethods = methodCollection.getGeneratedMethods();
        final Map<String, EssentialsGeneratedMethod> deletedMethods = new HashMap<>();
        final String oldReturnType = getReturnType(method.getReturnType());
        deleteUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(final MethodDeclaration node) {
                final String internalName = getHippoEssentialsAnnotation(node);
                if (internalName == null) {
                    return super.visit(node);
                }
                if (!internalName.equals(method.getInternalName())) {
                    return super.visit(node);
                }
                final String methodName = node.getName().getFullyQualifiedName();
                final Type type = node.getReturnType2();
                final String returnTypeName = getReturnType(type);

                if (returnTypeName != null) {
                    final EssentialsGeneratedMethod method = extractMethod(methodName, generatedMethods);
                    if (method == null) {
                        return super.visit(node);
                    }
                    if (returnTypeName.equals(oldReturnType)) {
                        node.delete();
                        deletedMethods.put(method.getMethodName(), method);
                        return super.visit(node);
                    }
                }
                return super.visit(node);
            }
        });

        final int deletedSize = deletedMethods.size();
        if (deletedSize > 0) {
            // rewrite source:
            final AST deleteAst = deleteUnit.getAST();
            final String deletedSource = JavaSourceUtils.rewrite(deleteUnit, deleteAst);
            GlobalUtils.writeToFile(deletedSource, path);
        }
        return deletedSize > 0;
    }

    public static EssentialsGeneratedMethod extractMethod(final String methodName, final Iterable<EssentialsGeneratedMethod> generatedMethods) {
        for (EssentialsGeneratedMethod generatedMethod : generatedMethods) {
            if (generatedMethod.getMethodName().equals(methodName)) {
                return generatedMethod;
            }
        }
        return null;
    }

    public static String getReturnType(final Type type) {
        if (type == null) {
            log.warn("Cannot extract return type from null type");
            return null;
        }
        if (type.isSimpleType()) {
            final SimpleType simpleType = (SimpleType) type;
            return simpleType.getName().getFullyQualifiedName();

        } else if (type.isArrayType()) {
            ArrayType arrayType = (ArrayType) type;
            return arrayType.toString();
        } else if (JavaSourceUtils.getParameterizedType(type) != null) {
            return JavaSourceUtils.getParameterizedType(type);
        }
        log.warn("Couldn't extract return type for: {}", type);
        return null;
    }

    public static String getParameterizedType(final Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Type myType = parameterizedType.getType();
        @SuppressWarnings("rawtypes")
        final List myArguments = parameterizedType.typeArguments();
        if (myArguments != null && myArguments.size() == 1
                && myType != null && myType.isSimpleType() && ((SimpleType) myType).getName().getFullyQualifiedName().equals("List")) {
            final Object o = myArguments.get(0);
            if (o instanceof SimpleType) {
                final SimpleType paramClazz = (SimpleType) o;
                return paramClazz.getName().getFullyQualifiedName();

            }
        }
        return null;
    }

    /**
     * Creates empty java class file for given class name and package
     *
     * @param sourceRootPath absolute directory path e.g. {@code /home/user/project/site/src/main/java}
     * @param className      name of the class e.g {@code FooBar}
     * @param packageName    name of the package e.g. {@code com.foo.bar}
     * @param fileExtension  name of file extension, if null {@code .java will be used}
     * @return Path object of the file created or null if failed to create a class
     */
    @SuppressWarnings(UNCHECKED)
    public static Path createJavaClass(final String sourceRootPath, final String className, final String packageName, final String fileExtension) {
        String myFileExtension = fileExtension;
        if (Strings.isNullOrEmpty(myFileExtension)) {
            myFileExtension = EssentialConst.FILE_EXTENSION_JAVA;
        }

        try {
            final Path clazzPath = createJavaSourcePath(sourceRootPath, className, packageName, myFileExtension);
            if (clazzPath.toFile().exists()) {
                log.info("File already exists: {}", clazzPath);
                return clazzPath;
            }
            final Path file = Files.createFile(clazzPath);
            final AST ast = AST.newAST(AST.JLS8);
            final CompilationUnit compilationUnit = ast.newCompilationUnit();
            final PackageDeclaration packageDeclaration = ast.newPackageDeclaration();
            packageDeclaration.setName(ast.newName(packageName));
            compilationUnit.setPackage(packageDeclaration);
            final TypeDeclaration td = ast.newTypeDeclaration();
            td.modifiers().addAll(ast.newModifiers(Modifier.PUBLIC));
            final SimpleName clazz = ast.newSimpleName(className);
            td.setName(clazz);

            compilationUnit.types().add(td);
            final String code = compilationUnit.toString();
            final byte[] contentInBytes = code.getBytes();
            try (FileOutputStream outputStream = new FileOutputStream(file.toFile())) {
                outputStream.write(contentInBytes);
                log.info("Created java file: {}", file);
                return file;
            }
        } catch (IOException e) {
            log.error("Error creating class: [" + packageName + '.' + className + ']', e);
        }

        return null;
    }

    /**
     * Annotates given class with Hippo annotations (<strong>Node, HippoEssentialsGenerated</strong>)
     *
     * @param path              path of the java source class
     * @param packageName       name of the package e.g. {@code com.foo.bar.beans}
     * @param documentNamespace namespace of the bean we are creating e.g. {@code project:namespace}
     * @param internalBeanName  name of the bean, e.g. {@code MyNewsBean}
     * @return source code of annotated bean or null on fail
     */
    @SuppressWarnings(UNCHECKED)
    public static String createHippoBean(final Path path, final String packageName, final String documentNamespace, final String internalBeanName) {

        if (path == null) {
            throw new EssentialsCodeCreationException("Cannot create bean for path which is null");
        }
        if (!path.toFile().exists()) {
            throw new EssentialsCodeCreationException("Cannot create bean for path. Given path: " + path);
        }
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        if (unit.types().size() == 0) {
            log.error("Invalid unit for bean: {}", path);
            return null;
        }
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        unit.getPackage().setName(ast.newName(DOT_SPLITTER.split(packageName)));
        // import declaration essentials:
        final String importName = HippoEssentialsGenerated.class.getCanonicalName();
        addImport(unit, ast, importName);
        // import declaration node:
        final String packageNameNode = "org.hippoecm.hst.content.beans.Node";
        addImport(unit, ast, packageNameNode);
        // NODE annotation:
        final NormalAnnotation nodeAnnotation = ast.newNormalAnnotation();
        final MemberValuePair nodeMemberValue = ast.newMemberValuePair();
        nodeMemberValue.setName(ast.newSimpleName("jcrType"));
        final StringLiteral jcrTypeLiteral = ast.newStringLiteral();
        jcrTypeLiteral.setLiteralValue(documentNamespace);
        nodeMemberValue.setValue(jcrTypeLiteral);
        nodeAnnotation.values().add(nodeMemberValue);
        nodeAnnotation.setTypeName(ast.newSimpleName("Node"));
        addAnnotation(classType, nodeAnnotation);
        addHippoGeneratedAnnotation(internalBeanName, unit, classType, ast);
        final String rewrite = rewrite(unit, ast);
        GlobalUtils.writeToFile(rewrite, path);
        return rewrite;

    }

    /**
     * Adds  extends keyword to a class. If class already extends another class it will silently fail
     *
     * @param path               path of java bean
     * @param extendingClassName name of the class bean is extending
     */
    public static void addExtendsClass(final Path path, final String extendingClassName) {

        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        final Type superclassType = classType.getSuperclassType();
        if (superclassType == null) {
            classType.setSuperclassType(ast.newSimpleType(ast.newSimpleName(extendingClassName)));
            final String rewrite = rewrite(unit, ast);
            GlobalUtils.writeToFile(rewrite, path);
        }
    }

    public static String getExtendsClass(final Path path) {

        final CompilationUnit unit = getCompilationUnit(path);
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final Type superclassType = classType.getSuperclassType();
        if (superclassType == null) {
            return null;
        }
        if (superclassType.isSimpleType()) {
            return ((SimpleType) superclassType).getName().getFullyQualifiedName();
        }
        // TODO add complex (wildcard etc types)
        return null;

    }

    public static String getSupertype(final Path path) {

        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        return classType.getSuperclassType().toString();
    }

    /**
     * Returns name of the class, fully qualified e.g. {@code com.foo.BarBean}
     *
     * @param path path to java source file
     */
    public static String getFullQualifiedClassName(final Path path) {

        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);

        final String fullyQualifiedName = unit.getPackage().getName().getFullyQualifiedName();
        final String identifier = classType.getName().getIdentifier();
        return fullyQualifiedName + '.' + identifier;
    }

    /**
     * Returns name of the class, e.g. {@code FooBarBean}
     *
     * @param path path to java source file
     */
    public static String getClassName(final Path path) {

        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        return classType.getName().getIdentifier();
    }

    /**
     * Add text to class comment (javadoc) node. If text already exists it will not be added
     *
     * @param path path of of an class
     * @param text text to add
     * @return rewritten source (with text node added to the javadoc)
     */
    public static void addClassJavaDoc(final Path path, final String text) {
        final CompilationUnit unit = getCompilationUnit(path);
        final String code = addClassJavaDoc(unit.toString(), text);
        GlobalUtils.writeToFile(formatCode(code), path);
    }

    /**
     * Add text to class comment (javadoc) node. If text already exists it will not be added
     *
     * @param content parsed content of an class
     * @param text    text to add
     * @return rewritten source (with text node added to the javadoc)
     */
    public static String addClassJavaDoc(final String content, final String text) {
        final CompilationUnit unit = JavaSourceUtils.getCompilationUnit(content);
        final AST ast = unit.getAST();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        Javadoc javadoc = classType.getJavadoc();
        
        if (javadoc == null) {
            return createNewJavadoc(text, unit, ast, classType);
        } 
        
        if (checkIfTextExists(text, javadoc)) {
            return content;
        }

        return addToExistingJavadoc(text, unit, ast, javadoc);
    }

    @SuppressWarnings("unchecked")
    private static String createNewJavadoc(final String text, final CompilationUnit unit, final AST ast, final TypeDeclaration classType) {
        final Javadoc javadoc;
        javadoc = ast.newJavadoc();
        TextElement element = ast.newTextElement();
        element.setText(text);
        TagElement tag = ast.newTagElement();
        tag.fragments().add(element);
        javadoc.tags().add(tag);
        classType.setJavadoc(javadoc);
        return JavaSourceUtils.rewrite(unit, ast);
    }

    private static boolean checkIfTextExists(final String text, final Javadoc javadoc) {
        @SuppressWarnings("unchecked")
        final List<TagElement> tags = javadoc.tags();
        for (TagElement tag : tags) {
            @SuppressWarnings("rawtypes")
            final List fragments = tag.fragments();
            if (fragments != null) {
                for (Object fragment : fragments) {
                    if (fragment instanceof TextElement) {
                        final TextElement textNode = (TextElement) fragment;
                        final String existingText = textNode.getText();
                        if (existingText.equals(text)) {
                            log.debug("Comment already in there: {}", existingText);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static String addToExistingJavadoc(final String text, final CompilationUnit unit, final AST ast, final Javadoc javadoc) {
        final TextElement element = ast.newTextElement();
        element.setText(text);
        final TagElement tag = ast.newTagElement();
        tag.fragments().add(element);
        javadoc.tags().add(tag);

        return JavaSourceUtils.rewrite(unit, ast);
    }

    /**
     * Adds {@code HippoEssentialsGenerated} annotation to provided java source file (class level)
     *
     * @param path path to java source file
     */
    public static void addHippoGeneratedBeanAnnotation(final Path path) {
        final String nodeJcrType = getNodeJcrType(path);
        if (nodeJcrType == null) {
            log.warn("Cannot generate internal name, jcrType was undefined");
            return;
        }
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        addHippoGeneratedAnnotation(nodeJcrType, unit, classType, ast);
        replaceFile(path, unit, ast);


    }

    public static void annotateMethod(final EssentialsGeneratedMethod method, final Path path) {
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final AST ast = unit.getAST();
        unit.accept(new ASTVisitor() {
            @SuppressWarnings(RAWTYPES)
            @Override
            public boolean visit(MethodDeclaration node) {
                final List parameters = node.parameters();
                final List myParams = method.getMethodDeclaration().parameters();
                if (!method.getMethodName().equals(node.getName().getIdentifier())) {
                    log.debug("skipping {}", method.getMethodName());
                    return super.visit(node);
                }
                // TODO improve this check: we need to check if all parameters are equal / same type
                if (parameters != null && myParams != null && myParams.size() == parameters.size()) {
                    // check method id:
                    addHippoGeneratedAnnotation(method.getInternalName(), unit, node, ast);
                    // rewrite file:
                    replaceFile(path, unit, ast);
                }
                return super.visit(node);
            }
        });

    }

    public static void annotateMethod(final EssentialsGeneratedMethod method, final MemoryBean bean) {
        final Path path = bean.getBeanPath();
        annotateMethod(method, path);
    }

    /**
     * Adds {@code HippoEssentialsGenerated} annotation to class, method etc. node types
     *
     * @param internalName value of the internal name annotation property
     * @param node         node we are adding annotation to
     * @param ast          AST tree  of the source code we are manipulating
     */
    @SuppressWarnings(UNCHECKED)
    public static void addHippoGeneratedAnnotation(final String internalName, final CompilationUnit unit, final ASTNode node, final AST ast) {
        //
        final NormalAnnotation generatedAnnotation = ast.newNormalAnnotation();
        generatedAnnotation.setTypeName(ast.newName(HippoEssentialsGenerated.class.getSimpleName()));
        // name
        final MemberValuePair generatedMemberValue = ast.newMemberValuePair();
        generatedMemberValue.setName(ast.newSimpleName(EssentialConst.ANNOTATION_INTERNAL_NAME_ATTRIBUTE));
        final StringLiteral internalNameLiteral = ast.newStringLiteral();
        internalNameLiteral.setLiteralValue(internalName);
        generatedMemberValue.setValue(internalNameLiteral);
        generatedAnnotation.values().add(generatedMemberValue);
        // allow code modifications
        /*
        final MemberValuePair modifyMemberValue = ast.newMemberValuePair();
        modifyMemberValue.setName(ast.newSimpleName(EssentialConst.ANNOTATION_ATTR_ALLOW_MODIFICATIONS));
        final BooleanLiteral modifyNameLiteral = ast.newBooleanLiteral(true);
        modifyMemberValue.setValue(modifyNameLiteral);
        generatedAnnotation.values().add(modifyMemberValue);
        */
        // add import:
        final String importName = HippoEssentialsGenerated.class.getCanonicalName();

        addImport(unit, ast, importName);


        addAnnotation(node, generatedAnnotation);
    }

    /**
     * Adds Calendar property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     */
    public static void addBeanMethodCalendar(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "Calendar[]" : "Calendar";
        addBeanMethodProperty(path, methodName, propertyName, returnType, multiple);
        final String importName = Calendar.class.getName();
        addImport(path, importName);

    }

    /**
     * Adds {@code getHippoHtml()} method
     *
     * @param path         source file path
     * @param methodName   generated method name
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodHippoHtml(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addImport(path, List.class.getName());
            addParameterizedMethod(methodName, "List", "HippoHtml", path, GET_CHILD_BEANS_BY_NAME, propertyName);
        } else {
            addSimpleMethod("getHippoHtml", path, methodName, propertyName, "HippoHtml");
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoHtml");
    }

    /**
     * Adds {@code getBean(namespace, HippoBean.class)} method
     *
     * @param path         source file path
     * @param methodName   generated method name
     * @param propertyName name of the property
     * @param multiple     is multiple property
     */
    public static void addBeanMethodHippoMirror(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, "List", HIPPO_BEAN, path, GET_LINKED_BEANS, propertyName);
            addImport(path, List.class.getName());
        } else {
            addTwoArgumentsMethod(GET_LINKED_BEAN, HIPPO_BEAN, path, methodName, propertyName);
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoBean");


    }

    /**
     * Adds {@code getBean(namespace, HippoGalleryImageBean.class)} method
     *
     * @param path         source file path
     * @param methodName   generated method name
     * @param propertyName name of the property
     * @param multiple     is multiple property
     */
    public static void addBeanMethodHippoImage(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, "List", "HippoGalleryImageBean", path, "getBeans", propertyName);
            addImport(path, List.class.getName());
        } else {
            addTwoArgumentsMethod(GET_BEAN, "HippoGalleryImageBean", path, methodName, propertyName);
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean");


    }

    public static void addBeanMethodHippoResource(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, "List", "HippoResourceBean", path, GET_CHILD_BEANS_BY_NAME, propertyName);
            addImport(path, List.class.getName());
        } else {
            addTwoArgumentsMethod(GET_BEAN, "HippoResourceBean", path, methodName, propertyName);
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoResourceBean");
    }

    public static void addBeanMethodHippoImageSet(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, "List", HIPPO_GALLERY_IMAGE_SET, path, GET_CHILD_BEANS_BY_NAME, propertyName);
            addImport(path, List.class.getName());
        } else {
            addTwoArgumentsMethod(GET_LINKED_BEAN, HIPPO_GALLERY_IMAGE_SET, path, methodName, propertyName);
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet");
    }

    public static void addBeanMethodInternalType(final Path path, final String className, final String importPath, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, "List", className, path, GET_CHILD_BEANS_BY_NAME, propertyName);
            addImport(path, List.class.getName());
        } else {
            addTwoArgumentsMethod(GET_BEAN, className, path, methodName, propertyName);
        }
        addImport(path, importPath);
    }

    public static void addBeanMethodInternalImageSet(final Path path, final String className, final String importPath, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, "List", className, path, GET_LINKED_BEANS, propertyName);
            addImport(path, List.class.getName());
        } else {
            addTwoArgumentsMethod(GET_LINKED_BEAN, className, path, methodName, propertyName);
        }
        addImport(path, importPath);
    }

    /**
     * Adds {@code getLinkedBean(namespace, HippoGalleryImageSet.class)} method
     *
     * @param path         source file path
     * @param methodName   generated method name
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodImageLink(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addImport(path, List.class.getName());
            addParameterizedMethod(methodName, "List", HIPPO_GALLERY_IMAGE_SET, path, GET_LINKED_BEANS, propertyName);
        } else {
            addTwoArgumentsMethod(GET_LINKED_BEAN, HIPPO_GALLERY_IMAGE_SET, path, methodName, propertyName);
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet");
    }

    /**
     * Adds string property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodString(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "String[]" : "String";
        addBeanMethodProperty(path, methodName, propertyName, returnType, multiple);
    }

    /**
     * Adds Boolean property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodBoolean(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        // TODO add null checks and return Boolean.FALSE
        final String returnType = multiple ? "Boolean[]" : "Boolean";
        addBeanMethodProperty(path, methodName, propertyName, returnType, multiple);
    }

    /**
     * Adds Double property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodDouble(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "Double[]" : "Double";
        addBeanMethodProperty(path, methodName, propertyName, returnType, multiple);
    }

    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodDocbase(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        final String source = multiple ?
                parseTemplate(SourceCodeTemplates.TEMPLATE_DOCBASE_MULTIPLE, propertyName, methodName):
                parseTemplate(SourceCodeTemplates.TEMPLATE_DOCBASE, propertyName, methodName);
        parser.setSource(new Document(source).get().toCharArray());
        final CompilationUnit sourceUnit = (CompilationUnit) parser.createAST(null);
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        sourceUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(final MethodDeclaration node) {
                final ASTNode astNode = ASTNode.copySubtree(ast, node);
                classType.bodyDeclarations().add(astNode);
                return true;
            }
        });
        replaceFile(path, unit, ast);
        addImport(path, HippoEssentialsGenerated.class.getName());
        addImport(path, EssentialConst.HIPPO_BEAN_IMPORT);
        if (multiple) {
            addImport(path, ArrayList.class.getName());
            addImport(path, List.class.getName());
        }
    }

    private static String parseTemplate(final String template, final String propertyName, final String methodName) {
         String retValue = template.replaceAll("\\$methodName\\$", methodName);
          retValue = retValue.replaceAll("\\$internalName\\$", propertyName);
        return retValue;
    }

    /**
     * Adds Long property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodLong(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "Long[]" : "Long";
        addBeanMethodProperty(path, methodName, propertyName, returnType, multiple);
    }

    public static ExistingMethodsVisitor getMethodCollection(final CompilationUnit unit) {
        final ExistingMethodsVisitor visitor = new ExistingMethodsVisitor();
        unit.accept(visitor);
        return visitor;
    }

    public static ExistingMethodsVisitor getMethodCollection(final String source) {
        final CompilationUnit unit = getCompilationUnit(source);
        final ExistingMethodsVisitor visitor = new ExistingMethodsVisitor();
        unit.accept(visitor);
        return visitor;
    }

    /**
     * Parse given java file and return method visitor (ExistingMethodsVisitor)
     *
     * @param path path to java file
     * @return ExistingMethodsVisitor instance
     */

    public static ExistingMethodsVisitor getMethodCollection(final Path path) {
        final CompilationUnit unit = getCompilationUnit(path);
        final ExistingMethodsVisitor visitor = new ExistingMethodsVisitor();
        unit.accept(visitor);
        return visitor;
    }

    /**
     * Parse given java file and return import statements
     *
     * @param path path to java file
     * @return list of import statements (fully qualified)
     */
    public static List<String> getImportStatements(final Path path) {
        final CompilationUnit unit = getCompilationUnit(path);
        @SuppressWarnings(RAWTYPES)
        final List imports = unit.imports();
        final List<String> importList = new ArrayList<>();
        for (Object anImport : imports) {
            final ImportDeclaration myImport = (ImportDeclaration) anImport;
            log.info("myImport {}", myImport);
            importList.add(myImport.getName().getFullyQualifiedName());


        }

        return importList;
    }

    /**
     * Format java code for given input
     *
     * @param source source to format
     * @return formatted source code or original on error
     */
    @SuppressWarnings({RAWTYPES, UNCHECKED})
    public static String formatCode(final String source) {

        final Map options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, TAB_SIZE);
        final String alignmentValue = DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_ON_COLUMN);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS, alignmentValue);
        final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
        final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, System.getProperty("line.separator"));
        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        final IDocument document = new Document(source);
        parser.setSource(document.get().toCharArray());
        try {
            edit.apply(document);
            return document.get();
        } catch (BadLocationException e) {
            log.error("Error formatting java code", e);
        }
        return source;
    }

    /**
     * @see #formatCode(String)
     */

    public static String formatCode(final Path path) {
        return formatCode(GlobalUtils.readTextFile(path).toString());
    }

    /**
     * @see #formatCode(String)
     */
    public static String formatCode(final IDocument document) {
        final String source = document.get();
        return formatCode(source);
    }

    /**
     * For given source, parse it and fetch Node annotation
     *
     * @param path path to the source file
     * @return null if no annotation or JCR type is not defined
     * @see EssentialConst#NODE_ANNOTATION_FULLY_QUALIFIED
     * @see EssentialConst#NODE_ANNOTATION_NAME
     */
    public static String getNodeJcrType(final Path path) {
        @SuppressWarnings(RAWTYPES)
        final List modifiers = getClassAnnotations(path);
        String jcrType = null;
        
        for (Object modifier : modifiers) {
            if (!(modifier instanceof NormalAnnotation)) {
                continue;
            }

            final NormalAnnotation annotation = (NormalAnnotation) modifier;
            final Name typeName = annotation.getTypeName();
            final String fullyQualifiedName = typeName.getFullyQualifiedName();
            
            if (checkNodeAndHippoGeneratedAnnotations(fullyQualifiedName)) {
                continue;
            }
            
            @SuppressWarnings(RAWTYPES)
            final List values = annotation.values();
            if (values == null) {
                continue;
            }
            
            for (Object value : values) {
                jcrType = processModifierAnnotation(jcrType, value);
            }
        }

        log.debug("Found @Node jcrType={}", jcrType);
        return jcrType;
    }

    private static String processModifierAnnotation(String jcrType, final Object value) {
        if (value instanceof MemberValuePair) {
            final MemberValuePair pair = (MemberValuePair) value;
            final SimpleName name = pair.getName();

            final String identifier = name.getIdentifier();
            if (identifier.equals("jcrType")) {
                final Expression literalValue = pair.getValue();
                if (literalValue instanceof StringLiteral) {
                    final StringLiteral ex = (StringLiteral) literalValue;
                    jcrType = ex.getLiteralValue();
                } else {
                    log.warn("Couldn't resolve value for jcrType: {}, we'll retry with internalName one", 
                            literalValue);
                }
            }
            if (jcrType == null && identifier.equals(EssentialConst.ANNOTATION_INTERNAL_NAME_ATTRIBUTE)) {
                final Expression literalValue = pair.getValue();
                if (literalValue instanceof StringLiteral) {
                    final StringLiteral ex = (StringLiteral) literalValue;
                    jcrType = ex.getLiteralValue();
                } else {
                    log.warn("Couldn't resolve value for internalName: {}", literalValue);
                }
            }
        }
        return jcrType;
    }

    private static boolean checkNodeAndHippoGeneratedAnnotations(final String fullyQualifiedName) {
        return 
                (!fullyQualifiedName.equals(EssentialConst.NODE_ANNOTATION_FULLY_QUALIFIED) 
                    && !fullyQualifiedName.equals(EssentialConst.NODE_ANNOTATION_NAME))
                &&
                (!fullyQualifiedName.equals(HippoEssentialsGenerated.class.getName()) 
                    && !fullyQualifiedName.equals(HippoEssentialsGenerated.class.getSimpleName()));
    }

    public static boolean hasHippoEssentialsAnnotation(final Path path) {
        @SuppressWarnings({RAWTYPES})
        final List modifiers = getClassAnnotations(path);
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                if (notAnEssentialsAnnotation(annotation)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    public static HippoEssentialsGeneratedObject getHippoEssentialsAnnotation(final Path beanPath, final MethodDeclaration node) {
        @SuppressWarnings({RAWTYPES})
        final List modifiers = node.modifiers();
        if (modifiers == null) {
            return null;
        }
        return getGeneratedObject(modifiers, beanPath);
    }

    public static String getHippoEssentialsAnnotation(final MethodDeclaration node) {
        @SuppressWarnings(RAWTYPES)
        final List modifiers = node.modifiers();
        if (modifiers == null) {
            return null;
        }
        
        for (Object modifier : modifiers) {
            if (skipAnnotation(modifier)) {
                continue;
            }

            final NormalAnnotation annotation = (NormalAnnotation) modifier;
            final List<?> values = annotation.values();
            for (Object value : values) {
                if (value instanceof MemberValuePair) {
                    final MemberValuePair mvp = (MemberValuePair) value;
                    final String identifier = mvp.getName().getIdentifier();
                    final Expression myValue = mvp.getValue();
                    if (myValue instanceof StringLiteral) {
                        final StringLiteral literal = (StringLiteral) myValue;
                        final String literalValue = literal.getLiteralValue();
                        if ("internalName".equals(identifier)) {
                            return literalValue;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean skipAnnotation(final Object modifier) {
        if (!(modifier instanceof NormalAnnotation)) {
            return true;
        }
        final NormalAnnotation annotation = (NormalAnnotation) modifier;
        return notAnEssentialsAnnotation(annotation);
    }

    private static boolean notAnEssentialsAnnotation(final NormalAnnotation annotation) {
        final Name typeName = annotation.getTypeName();
        final String fullyQualifiedName = typeName.getFullyQualifiedName();
        if (!fullyQualifiedName.equals(HippoEssentialsGenerated.class.getSimpleName())
                && !fullyQualifiedName.equals(HippoEssentialsGenerated.class.getCanonicalName())) {
            return true;
        }
        return false;
    }

    public static HippoEssentialsGeneratedObject getHippoGeneratedAnnotation(final Path path) {
        @SuppressWarnings({RAWTYPES})
        final List modifiers = getClassAnnotations(path);
        return getGeneratedObject(modifiers, path);
    }

    private static HippoEssentialsGeneratedObject getGeneratedObject(final List modifiers, final Path path) {
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                if (notAnEssentialsAnnotation(annotation)) {
                    continue;
                }
                return populateGeneratedObject(path, annotation);
            }
        }
        return null;
    }

    private static HippoEssentialsGeneratedObject populateGeneratedObject(final Path path, final NormalAnnotation annotation) {
        final HippoEssentialsGeneratedObject o = new HippoEssentialsGeneratedObject();
        // set default:
        o.setAllowModifications(true);
        o.setFilePath(path);
        @SuppressWarnings(RAWTYPES)
        final List values = annotation.values();
        if (values != null) {
            for (Object value : values) {
                if (value instanceof MemberValuePair) {
                    final MemberValuePair pair = (MemberValuePair) value;
                    final SimpleName name = pair.getName();
                    final String identifier = name.getIdentifier();
                    switch (identifier) {
                        case EssentialConst.ANNOTATION_ATTR_ALLOW_MODIFICATIONS: {
                            final BooleanLiteral ex = (BooleanLiteral) pair.getValue();
                            o.setAllowModifications(ex.booleanValue());
                            break;
                        }
                        case EssentialConst.ANNOTATION_ATTR_DATE: {
                            final StringLiteral ex = (StringLiteral) pair.getValue();
                            o.setDateGenerated(ex.getLiteralValue());
                            break;
                        }
                        case EssentialConst.ANNOTATION_ATTR_INTERNAL_NAME: {
                            final StringLiteral ex = (StringLiteral) pair.getValue();
                            o.setInternalName(ex.getLiteralValue());
                            break;
                        }
                        default:
                            log.error("Unknown identifier {}", identifier);
                            break;
                    }
                }
            }
        }
        return o;
    }

    /**
     * Add import statement for given class
     *
     * @param path       path of the java source class
     * @param importName name of the import e.g {@code java.util.List}
     */
    public static void addImport(final Path path, final CharSequence importName) {
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final AST ast = unit.getAST();
        addImport(unit, ast, importName);
        replaceFile(path, unit, ast);
    }

    public static void addImport(final CompilationUnit unit, final CharSequence importName) {
        final AST ast = unit.getAST();
        addImport(unit, ast, importName);

    }

    public static String addImport(final String source, final CharSequence importName) {
        final CompilationUnit unit = getCompilationUnit(source);
        unit.recordModifications();
        final AST ast = unit.getAST();
        addImport(unit, ast, importName);
        return rewrite(unit, ast);
    }

    //############################################
    // LOCAL UTILITY METHODS
    //############################################

    private static void replaceFile(final Path path, final CompilationUnit unit, final AST ast) {
        // rewrite AST
        final String rewrite = rewrite(unit, ast);
        log.debug("Rewriting\n{}", rewrite);
        GlobalUtils.writeToFile(rewrite, path);
    }

    @SuppressWarnings(UNCHECKED)
    private static List<Object> getClassAnnotations(final Path path) {
        final CompilationUnit unit = getCompilationUnit(path);
        List<Object> modifiers = null;
        if (unit.types().size() > 0) {
            final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
            modifiers = classType.modifiers();
        }
        if (modifiers == null) {
            return Collections.emptyList();
        }
        return modifiers;
    }


    public static String getImportName(final Path path) {
        final String packageName = JavaSourceUtils.getPackageName(path);
        final String className = JavaSourceUtils.getClassName(path);
        if (Strings.isNullOrEmpty(packageName)) {
            return className;
        }

        return String.format("%s.%s", packageName, className);
    }

    public static String getPackageName(final Path path) {
        final CompilationUnit unit = JavaSourceUtils.getCompilationUnit(path);
        final PackageDeclaration myPackage = unit.getPackage();
        if (myPackage != null) {
            final Name name = myPackage.getName();
            if (name != null) {
                return name.getFullyQualifiedName();
            }

        }
        return "";
    }

    @SuppressWarnings({UNCHECKED, RAWTYPES})
    private static void addImport(final CompilationUnit unit, final AST ast, final CharSequence importName) {
        final List imports = unit.imports();
        for (Object anImport : imports) {
            final ImportDeclaration declaration = (ImportDeclaration) anImport;
            final String fullyQualifiedName = declaration.getName().getFullyQualifiedName();
            if (importName.equals(fullyQualifiedName)) {
                log.debug("Import already exists, skipping {}", fullyQualifiedName);
                return;
            }
        }
        final ImportDeclaration essentialsImportDeclaration = ast.newImportDeclaration();
        essentialsImportDeclaration.setName(ast.newName(DOT_SPLITTER.split(importName)));
        imports.add(essentialsImportDeclaration);
    }

    private static void addBeanMethodProperty(final Path path, final String methodName, final String propertyName, final String returnType, final boolean multiple) {
        final String superMethodName = multiple ? "getMultipleProperty" : "getSingleProperty";
        addSimpleMethod(superMethodName, path, methodName, propertyName, returnType);
    }

    @SuppressWarnings(UNCHECKED)
    private static void addSimpleMethod(final String hippoMethodName, final Path path, final String methodName, final String propertyName, final String returnType) {
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setName(ast.newSimpleName(methodName));
        if (returnType.indexOf('[') != -1) {
            final String type = ARRAY_PATTERN.matcher(returnType).replaceAll("");
            methodDeclaration.setReturnType2(ast.newArrayType(ast.newSimpleType(ast.newSimpleName(type))));
        } else {
            methodDeclaration.setReturnType2(ast.newSimpleType(ast.newSimpleName(returnType)));
        }
        methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        methodDeclaration.setConstructor(false);
        final Block body = ast.newBlock();
        methodDeclaration.setBody(body);
        final ReturnStatement statement = ast.newReturnStatement();
        final MethodInvocation expression = ast.newMethodInvocation();
        expression.setName(ast.newSimpleName(hippoMethodName));
        final StringLiteral literal = ast.newStringLiteral();
        literal.setLiteralValue(propertyName);
        expression.arguments().add(literal);
        statement.setExpression(expression);
        body.statements().add(statement);
        classType.bodyDeclarations().add(methodDeclaration);
        // add annotation
        final MarkerAnnotation generatedAnnotation = ast.newMarkerAnnotation();
        generatedAnnotation.setTypeName(ast.newName(HippoEssentialsGenerated.class.getSimpleName()));
        addHippoGeneratedAnnotation(propertyName, unit, methodDeclaration, ast);
        replaceFile(path, unit, ast);
    }

    @SuppressWarnings(UNCHECKED)
    public static void addAnnotation(ASTNode node, IExtendedModifier annotation) {
        // add annotation at first position:
        if (node instanceof TypeDeclaration) {
            TypeDeclaration type = (TypeDeclaration) node;
            if (hasNotAnnotation(type.modifiers(), annotation)) {
                type.modifiers().add(0, annotation);
            }
        } else if (node instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement localVariable = (VariableDeclarationStatement) node;
            localVariable.modifiers().add(0, annotation);
        } else if (node instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration variable = (SingleVariableDeclaration) node;
            variable.modifiers().add(0, annotation);
        } else if (node instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node;
            if (hasNotAnnotation(method.modifiers(), annotation)) {
                method.modifiers().add(0, annotation);
            }
        } else if (node instanceof FieldDeclaration) {
            FieldDeclaration field = (FieldDeclaration) node;
            field.modifiers().add(0, annotation);
        } else {
            log.info("Couldn't add annotation to node: {}", node);
        }
    }

    @SuppressWarnings(RAWTYPES)
    private static boolean hasNotAnnotation(final List modifiers, final IExtendedModifier annotation) {
        for (Object modifier : modifiers) {
            if (isCorrectType(modifier, annotation)) {
                final Annotation existing = (Annotation) modifier;
                final Annotation newOne = (Annotation) annotation;
                final String fullyQualifiedName = existing.getTypeName().getFullyQualifiedName();
                if (fullyQualifiedName.equals(newOne.getTypeName().getFullyQualifiedName())) {
                    log.debug(ANNOTATION_ALREADY_EXISTS, fullyQualifiedName);
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isCorrectType(final Object modifier, final IExtendedModifier annotation) {
        return (modifier instanceof NormalAnnotation && annotation instanceof NormalAnnotation) 
                || (modifier instanceof SingleMemberAnnotation && annotation instanceof SingleMemberAnnotation)
                || (modifier instanceof MarkerAnnotation && annotation instanceof MarkerAnnotation);
    }

    private static Path createJavaSourcePath(final String sourceRoot, final String className, final CharSequence packageName, final String fileExtension) throws IOException {
        final Iterator<String> iterator = Splitter.on('.').split(packageName).iterator();
        final Joiner joiner = Joiner.on(File.separator).skipNulls();
        String finalPath = joiner.join(iterator);
        if (sourceRoot.endsWith(File.separator)) {
            finalPath = sourceRoot + finalPath + File.separator;
        } else {
            finalPath = sourceRoot + File.separator + finalPath + File.separator;
        }
        // make sure we have directories created:
        Files.createDirectories(Paths.get(finalPath));
        return Paths.get(finalPath + File.separator + className + fileExtension);
    }

    public static CompilationUnit getCompilationUnit(final String content) {
        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(new Document(content).get().toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }

    public static CompilationUnit getCompilationUnit(final Path path) {
        final StringBuilder builder = GlobalUtils.readTextFile(path);
        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(new Document(builder.toString()).get().toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }

    public static String rewrite(final CompilationUnit unit, final AST ast) {
        final IDocument document = new Document(unit.toString());
        final ASTRewrite rewriter = ASTRewrite.create(ast);
        final TextEdit edits = rewriter.rewriteAST(document, null);
        try {
            edits.apply(document);
            //log.debug("{}", formatted);
            return formatCode(document);
        } catch (BadLocationException e) {
            log.error("Error creating HippoBean", e);
        }

        return null;
    }

    @SuppressWarnings(UNCHECKED)
    public static void addRelatedDocsMethod(final String methodName, final Path path) {

        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setName(ast.newSimpleName(methodName));
        final ParameterizedType type = ast.newParameterizedType(ast.newSimpleType(ast.newName("List")));
        type.typeArguments().add(ast.newSimpleType(ast.newSimpleName(HIPPO_BEAN)));
        methodDeclaration.setReturnType2(type);
        methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        methodDeclaration.setConstructor(false);
        // start method body
        final Block body = ast.newBlock();
        methodDeclaration.setBody(body);
        final VariableDeclarationFragment relatedFragment = ast.newVariableDeclarationFragment();
        relatedFragment.setName(ast.newSimpleName("documents"));
        final MethodInvocation relatedInvocation = ast.newMethodInvocation();
        relatedInvocation.setName(ast.newSimpleName(GET_BEAN));
        final StringLiteral stringArg = ast.newStringLiteral();
        stringArg.setLiteralValue(EssentialConst.RELATEDDOCS_DOCS);
        relatedInvocation.arguments().add(stringArg);
        final VariableDeclarationStatement relatedStatement = ast.newVariableDeclarationStatement(relatedFragment);

        relatedStatement.setType(ast.newSimpleType(ast.newSimpleName(EssentialConst.RELATED_DOCS_BEAN)));
        relatedStatement.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
        relatedFragment.setInitializer(relatedInvocation);
        body.statements().add(relatedStatement);

        // facet reference

        final MethodInvocation facetInvocation = ast.newMethodInvocation();
        final SimpleName myName = ast.newSimpleName("documents");
        facetInvocation.setExpression(myName);
        facetInvocation.setName(ast.newSimpleName("getDocs"));
        final ReturnStatement statement = ast.newReturnStatement();
        statement.setExpression(facetInvocation);
        body.statements().add(statement);
        classType.bodyDeclarations().add(methodDeclaration);
        // add annotation
        final MarkerAnnotation generatedAnnotation = ast.newMarkerAnnotation();
        generatedAnnotation.setTypeName(ast.newName(HippoEssentialsGenerated.class.getSimpleName()));
        addHippoGeneratedAnnotation(EssentialConst.RELATEDDOCS_DOCS, unit, methodDeclaration, ast);
        replaceFile(path, unit, ast);
        // add imports
        addImport(path, EssentialConst.HIPPO_BEAN_IMPORT);
        addImport(path, EssentialConst.HIPPO_RELATED_DOCS_IMPORT);
        addImport(path, List.class.getName());

    }

    @SuppressWarnings(UNCHECKED)
    private static VariableDeclarationStatement createListType(final AST ast, final VariableDeclarationFragment varFragment, final String type) {
        final VariableDeclarationStatement varStatement = ast.newVariableDeclarationStatement(varFragment);
        final ParameterizedType fieldListType = ast.newParameterizedType(ast.newSimpleType(ast.newName("List")));
        fieldListType.typeArguments().add(ast.newSimpleType(ast.newSimpleName(type)));
        varStatement.setType(fieldListType);
        varStatement.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
        return varStatement;
    }

    @SuppressWarnings(UNCHECKED)
    public static void addParameterizedMethod(final String methodName, final String returnType, final String genericsType, final Path path, final String returnMethodName, final String propertyName) {
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setName(ast.newSimpleName(methodName));
        final ParameterizedType type = ast.newParameterizedType(ast.newSimpleType(ast.newName(returnType)));
        type.typeArguments().add(ast.newSimpleType(ast.newSimpleName(genericsType)));
        methodDeclaration.setReturnType2(type);
        methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        methodDeclaration.setConstructor(false);
        final Block body = ast.newBlock();
        methodDeclaration.setBody(body);
        final ReturnStatement statement = ast.newReturnStatement();
        final MethodInvocation expression = ast.newMethodInvocation();
        expression.setName(ast.newSimpleName(returnMethodName));
        // arguments
        final StringLiteral literal = ast.newStringLiteral();
        literal.setLiteralValue(propertyName);
        expression.arguments().add(literal);
        // Class argument
        TypeLiteral classLiteral = ast.newTypeLiteral();

        classLiteral.setType(ast.newSimpleType(ast.newName(genericsType)));
        expression.arguments().add(classLiteral);
        //
        statement.setExpression(expression);
        body.statements().add(statement);
        classType.bodyDeclarations().add(methodDeclaration);
        // add annotation
        final MarkerAnnotation generatedAnnotation = ast.newMarkerAnnotation();
        generatedAnnotation.setTypeName(ast.newName(HippoEssentialsGenerated.class.getSimpleName()));
        addHippoGeneratedAnnotation(propertyName, unit, methodDeclaration, ast);
        replaceFile(path, unit, ast);

    }

    @SuppressWarnings(UNCHECKED)
    public static void addTwoArgumentsMethod(final String returnMethodName, final String returnType, final Path path, final String methodName, final String propertyName) {
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setName(ast.newSimpleName(methodName));
        methodDeclaration.setReturnType2(ast.newSimpleType(ast.newSimpleName(returnType)));
        methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        methodDeclaration.setConstructor(false);
        final Block body = ast.newBlock();
        methodDeclaration.setBody(body);
        final ReturnStatement statement = ast.newReturnStatement();
        final MethodInvocation expression = ast.newMethodInvocation();
        expression.setName(ast.newSimpleName(returnMethodName));
        // arguments
        final StringLiteral literal = ast.newStringLiteral();
        literal.setLiteralValue(propertyName);
        expression.arguments().add(literal);
        // Class argument
        TypeLiteral classLiteral = ast.newTypeLiteral();
        classLiteral.setType(ast.newSimpleType(ast.newName(returnType)));
        expression.arguments().add(classLiteral);
        //
        statement.setExpression(expression);
        body.statements().add(statement);
        classType.bodyDeclarations().add(methodDeclaration);
        // add annotation
        final MarkerAnnotation generatedAnnotation = ast.newMarkerAnnotation();
        generatedAnnotation.setTypeName(ast.newName(HippoEssentialsGenerated.class.getSimpleName()));
        addHippoGeneratedAnnotation(propertyName, unit, methodDeclaration, ast);
        replaceFile(path, unit, ast);
    }

}
