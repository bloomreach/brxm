package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.onehippo.cms7.essentials.dashboard.utils.code.ComponentInformation;
import org.onehippo.cms7.essentials.dashboard.utils.code.EssentialsGeneratedMethod;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.essentials.dashboard.utils.code.NoAnnotationMethodVisitor;
import org.onehippo.cms7.essentials.dashboard.utils.code.exc.EssentialsCodeCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * Utility class for manipulating java source files.
 *
 * @version "$Id$"
 */
public final class JavaSourceUtils {

    private static final Pattern DOT_SPLITTER = Pattern.compile("\\.");
    /**
     * Pattern for replacing return type in case of array, e.g. {@code String []} or {@code String [  ]}
     */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[\\s*\\]");
    public static final String UNCHECKED = "unchecked";
    public static final String RAWTYPES = "rawtypes";
    private static Logger log = LoggerFactory.getLogger(JavaSourceUtils.class);

    private JavaSourceUtils() {
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
        FileOutputStream outputStream = null;
        try {

            final Path clazzPath = createJavaSourcePath(sourceRootPath, className, packageName, myFileExtension);
            if (clazzPath.toFile().exists()) {
                log.info("File already exists: {}", clazzPath);
                return clazzPath;
            }
            final Path file = Files.createFile(clazzPath);
            final AST ast = AST.newAST(AST.JLS3);
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
            outputStream = new FileOutputStream(file.toFile());
            outputStream.write(contentInBytes);
            log.info("Created java file: {}", file);
            //log.debug("Code written: {}", code);
            return file;
        } catch (IOException e) {
            log.error("Error creating class: [" + packageName + '.' + className + ']', e);
        } finally {
            IOUtils.closeQuietly(outputStream);
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

    /**
     * Writes a component
     *
     * @param info    component information
     * @param context plugin context
     * @return path of created file
     */
    public static Path writeEssentialsComponent(final ComponentInformation info, final PluginContext context) {
        final File siteDirectory = context.getSiteDirectory();
        final String sitePath = siteDirectory.getAbsolutePath();
        final Path path = createJavaClass(sitePath, info.getTargetClassName(), info.getTargetPackageName(), EssentialConst.FILE_EXTENSION_JAVA);
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final AST ast = unit.getAST();
        unit.getPackage().setName(ast.newName(DOT_SPLITTER.split(info.getTargetPackageName())));
        // import declaration essentials:
        final List<String> imports = info.getImports();
        for (String anImport : imports) {
            addImport(unit, ast, anImport);
        }
        // add extends:
        classType.setSuperclassType(ast.newSimpleType(ast.newSimpleName(info.getExtendingComponentName())));
        addHippoGeneratedAnnotation(info.getTargetClassName(), unit, classType, ast);
        replaceFile(path, unit, ast);
        return path;


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
        final String returnType = multiple ? "List<Calendar>" : Calendar.class.getSimpleName();
        if (multiple) {
            addImport(path, "java.util.List");
        }
        addImport(path, "java.util.Calendar");
        addBeanMethodProperty(path, methodName, propertyName, returnType);
        final String importName = Calendar.class.getPackage().getName();
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
    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodHippoHtml(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "List<HippoHtml>" : "HippoHtml";
        if (multiple) {
            addImport(path, "java.util.List");
        }
        addSimpleMethod("getHippoHtml", path, methodName, propertyName, returnType);
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoHtml");
    }

    /**
     * getBean("politie:assetfolder", HippoMirrorBean.class);
     */

    /**
     * Adds {@code getBean(namespace, HippoMirrorBean.class)} method
     *
     * @param path         source file path
     * @param methodName   generated method name
     * @param propertyName name of the property
     * @param multiple     is multiple property
     */
    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodHippoMirror(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addTwoArgumentsMethod("getLinkedBeans", "List<HippoMirrorBean>", path, methodName, propertyName);
            addImport(path, "java.util.List");
        } else {
            addTwoArgumentsMethod("getLinkedBean", "HippoMirrorBean", path, methodName, propertyName);
        }
        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoMirrorBean");


    }

    /**
     * Adds {@code getLinkedBean(namespace, HippoGalleryImageSetBean.class)} method
     *
     * @param path         source file path
     * @param methodName   generated method name
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    public static void addBeanMethodImageLink(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addImport(path, "java.util.List");
            addTwoArgumentsMethod("getLinkedBeans", "HippoGalleryImageSetBean", path, methodName, propertyName);
        } else {
            addTwoArgumentsMethod("getLinkedBean", "HippoGalleryImageSetBean", path, methodName, propertyName);
        }

        addImport(path, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean");
    }

    /**
     * Adds string property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodString(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "String[]" : "String";
        addBeanMethodProperty(path, methodName, propertyName, returnType);
    }

    /**
     * Adds Boolean property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodBoolean(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        // TODO add null checks and return Boolean.FALSE
        final String returnType = multiple ? "Boolean[]" : "Boolean";
        addBeanMethodProperty(path, methodName, propertyName, returnType);
    }

    /**
     * Adds Double property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodDouble(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "Double[]" : "Double";
        addBeanMethodProperty(path, methodName, propertyName, returnType);
    }

    /**
     * Adds Long property method (getProperty)
     *
     * @param path         path of java bean
     * @param methodName   name of the method
     * @param propertyName name of the property
     * @param multiple     indicates multiple property
     */
    @SuppressWarnings(UNCHECKED)
    public static void addBeanMethodLong(final Path path, final String methodName, final String propertyName, final boolean multiple) {
        final String returnType = multiple ? "Long[]" : "Long";
        addBeanMethodProperty(path, methodName, propertyName, returnType);
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
     * Parse given java file and return method visitor (NoAnnotationMethodVisitor).
     * This visitor contains method names which are *not* annotated and we can annotate
     *
     * @param path path to java file
     * @return NoAnnotationMethodVisitor instance
     * @see org.onehippo.cms7.essentials.dashboard.utils.code.NoAnnotationMethodVisitor
     */
    public static NoAnnotationMethodVisitor getAnnotateMethods(final PluginContext context, final Path path) {

        /*
        When you use: parser.setSource(source); What is the type of param "source"?
        Binding information is obtained from the Java model. This means that the compilation unit
        must be located relative to the Java model. This happens automatically when the source code comes from
        either setSource(ICompilationUnit) or setSource(IClassFile). When source is supplied by setSource(char[]),
        the location must be extablished explicitly by calling setProject(IJavaProject) and setUnitName(String).
    */
        final CompilationUnit unit = getCompilationUnit(path);
        final ASTParser parser = ASTParser.newParser(AST.JLS3);
        /*IFile file =
        parser.setSource(JavaCore.createClassFileFrom(file));*/
        parser.setSource(GlobalUtils.readTextFile(path).toString().toCharArray());
        parser.setStatementsRecovery(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        parser.setResolveBindings(true);
        final ASTNode node = parser.createAST(null);
        //



        /*
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        log.info("workspace {}", workspace);
        IWorkspaceRoot root = workspace.getRoot();
        IProject project = root.getProject("dummy");
        try {
            project.open(null *//* IProgressMonitor *//*);
        } catch (CoreException e) {
            log.error("", e);
        }
        IJavaProject javaProject = JavaCore.create(project);
        */


        node.accept(new NoAnnotationMethodVisitor());

        final NoAnnotationMethodVisitor visitor = new NoAnnotationMethodVisitor();
        unit.accept(visitor);
        return visitor;
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
        final String alignmentValue = DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_ON_COLUMN);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS, alignmentValue);
        final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
        final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, System.getProperty("line.separator"));
        final ASTParser parser = ASTParser.newParser(AST.JLS3);
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
    @SuppressWarnings({RAWTYPES, UNCHECKED})
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
        @SuppressWarnings({UNCHECKED, RAWTYPES})
        final List modifiers = getClassAnnotations(path);
        String jcrType = null;
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                final Name typeName = annotation.getTypeName();
                final String fullyQualifiedName = typeName.getFullyQualifiedName();
                // check Node & HippoGenerated annotations
                if (
                        (!fullyQualifiedName.equals(EssentialConst.NODE_ANNOTATION_FULLY_QUALIFIED) && !fullyQualifiedName.equals(EssentialConst.NODE_ANNOTATION_NAME))
                                &&
                                (!fullyQualifiedName.equals(HippoEssentialsGenerated.class.getName()) && !fullyQualifiedName.equals(HippoEssentialsGenerated.class.getSimpleName()))

                        ) {
                    log.debug("Skipping annotation: {}", fullyQualifiedName);
                    continue;
                }
                @SuppressWarnings(RAWTYPES)
                final List values = annotation.values();
                if (values != null) {
                    for (Object value : values) {
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


                                    log.warn("Couldn't resolve value for jcrType: {}, we'll retry with internalName one", literalValue);
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
                    }
                }
            }
        }

        log.debug("Found @Node jcrType={}", jcrType);
        return jcrType;
    }

    public static boolean hasHippoEssentialsAnnotation(final Path path) {
        @SuppressWarnings({UNCHECKED, RAWTYPES})
        final List modifiers = getClassAnnotations(path);
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                final Name typeName = annotation.getTypeName();
                final String fullyQualifiedName = typeName.getFullyQualifiedName();
                if (!fullyQualifiedName.equals(HippoEssentialsGenerated.class.getSimpleName())
                        && !fullyQualifiedName.equals(HippoEssentialsGenerated.class.getCanonicalName())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    public static HippoEssentialsGeneratedObject getHippoGeneratedAnnotation(final Path path) {
        @SuppressWarnings({UNCHECKED, RAWTYPES})
        final List modifiers = getClassAnnotations(path);
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                final Name typeName = annotation.getTypeName();
                final String fullyQualifiedName = typeName.getFullyQualifiedName();
                if (!fullyQualifiedName.equals(HippoEssentialsGenerated.class.getSimpleName())
                        && !fullyQualifiedName.equals(HippoEssentialsGenerated.class.getCanonicalName())) {
                    continue;
                }
                return populateGeneratedObject(path, annotation);
            }
        }
        return null;
    }

    private static HippoEssentialsGeneratedObject populateGeneratedObject(final Path path, final NormalAnnotation annotation) {
        final HippoEssentialsGeneratedObject o = new HippoEssentialsGeneratedObject();
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
    @SuppressWarnings(UNCHECKED)
    public static void addImport(final Path path, final String importName) {
        final CompilationUnit unit = getCompilationUnit(path);
        unit.recordModifications();
        final AST ast = unit.getAST();
        final ImportDeclaration essentialsImportDeclaration = ast.newImportDeclaration();
        essentialsImportDeclaration.setName(ast.newName(importName));
        unit.imports().add(essentialsImportDeclaration);
        replaceFile(path, unit, ast);
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
        final TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
        final List<Object> modifiers = classType.modifiers();
        if (modifiers == null) {
            return Collections.emptyList();
        }
        return modifiers;
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

    @SuppressWarnings(UNCHECKED)
    private static void addBeanMethodProperty(final Path path, final String methodName, final String propertyName, final String returnType) {
        addSimpleMethod("getProperty", path, methodName, propertyName, returnType);
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
            methodDeclaration.setReturnType2(ast.newSimpleType(ast.newSimpleName(type)));
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
    private static void addAnnotation(ASTNode node, IExtendedModifier annotation) {
        // add annotation at first position:
        if (node instanceof TypeDeclaration) {
            TypeDeclaration type = (TypeDeclaration) node;
            if (!hasAnnotation(type.modifiers(), annotation)) {
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
            method.modifiers().add(0, annotation);
        } else if (node instanceof FieldDeclaration) {
            FieldDeclaration field = (FieldDeclaration) node;
            field.modifiers().add(0, annotation);
        } else {
            log.info("Couldn't add annotation to node: {}", node);
        }
    }

    @SuppressWarnings(RAWTYPES)
    private static boolean hasAnnotation(final List modifiers, final IExtendedModifier annotation) {
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation && annotation instanceof NormalAnnotation) {
                final NormalAnnotation existing = (NormalAnnotation) modifier;
                final NormalAnnotation newOne = (NormalAnnotation) annotation;
                final String fullyQualifiedName = existing.getTypeName().getFullyQualifiedName();
                if (fullyQualifiedName.equals(newOne.getTypeName().getFullyQualifiedName())) {
                    log.debug("Annotation already exists: {}", fullyQualifiedName);
                    return true;
                }
            }
        }

        return false;
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

    private static CompilationUnit getCompilationUnit(final Path path) {
        final StringBuilder builder = GlobalUtils.readTextFile(path);
        final ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setResolveBindings(true);
        parser.setSource(new Document(builder.toString()).get().toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }

    private static String rewrite(final CompilationUnit unit, final AST ast) {
        final IDocument document = new Document(unit.toString());
        final ASTRewrite rewriter = ASTRewrite.create(ast);
        final TextEdit edits = rewriter.rewriteAST(document, null);
        try {
            edits.apply(document);
            final String formattedSource = formatCode(document);
            log.debug("formattedSource {}", formattedSource);
            return formattedSource;
        } catch (BadLocationException e) {
            log.error("Error creating HippoBean", e);
        }

        return null;
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
