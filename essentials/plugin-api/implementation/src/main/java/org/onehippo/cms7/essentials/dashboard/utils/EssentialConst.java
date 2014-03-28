/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id$"
 */
public final class EssentialConst {

    /**
     * SCRIPTING
     */
    public static final String FREEMARKER_RELATIVE_FOLDER = "src" + File.separator + "main" + File.separator + "webapp" + File.separator + "WEB-INF" + File.separator + "ftl";
    public static final String PATH_REL_WEB_ROOT = "src" + File.separator + "main" + File.separator + "webapp";
    public static final String PATH_REL_OVERRIDE = "META-INF" + File.separator + "hst-assembly" + File.separator + "overrides";
    /**
     * Namespace placeholder name
     */
    public static final String PLACEHOLDER_NAMESPACE = "namespace";
    public static final String PLACEHOLDER_PROJECT_ROOT = "projectRoot";
    public static final String PLACEHOLDER_DATE_REPO_YYYY_MM = "dateRepoYearMonth";
    public static final String PLACEHOLDER_DATE_FILE_YYYY_MM = "dateFileYearMonth";
    public static final String PLACEHOLDER_DATE_REPO_YYYY_MM_NEXT_MONTH = "dateRepoYearMonthNextMonth";
    public static final String PLACEHOLDER_DATE_FILE_YYYY_MM_NEXT_MONTH = "dateFileYearMonthNextMonth";
    public static final String PLACEHOLDER_DATE_REPO_YYYY_MM_NEXT_YEAR = "dateRepoYearMonthNextYear";
    public static final String PLACEHOLDER_DATE_FILE_YYYY_MM_NEXT_YEAR = "dateFileYearMonthNextYear";
    public static final String PLACEHOLDER_SITE_ROOT = "siteRoot";
    public static final String PLACEHOLDER_SITE_WEB_ROOT = "siteWebRoot";
    public static final String PLACEHOLDER_SITE_RESOURCES = "siteResources";
    public static final String PLACEHOLDER_SITE_FREEMARKER_ROOT = "freemarkerRoot";
    public static final String PLACEHOLDER_JSP_ROOT = "jspRoot";
    public static final String PLACEHOLDER_JAVASCRIPT_ROOT = "javascriptRoot";
    public static final String PLACEHOLDER_CSS_ROOT = "cssRoot";
    public static final String PLACEHOLDER_CMS_ROOT = "cmsRoot";
    public static final String PLACEHOLDER_CMS_RESOURCES = "cmsResources";
    public static final String PLACEHOLDER_CMS_WEB_ROOT = "cmsWebRoot";
    public static final String PLACEHOLDER_SOURCE = "source";
    public static final String PLACEHOLDER_TARGET = "target";
    public static final String PLACEHOLDER_BEANS_PACKAGE = "beansPackage";
    public static final String PLACEHOLDER_COMPONENTS_PACKAGE = "componentsPackage";
    public static final String PLACEHOLDER_REST_PACKAGE = "restPackage";
    public static final String PLACEHOLDER_BEANS_FOLDER = "beansFolder";
    public static final String PLACEHOLDER_COMPONENTS_FOLDER = "componentsFolder";
    public static final String PLACEHOLDER_REST_FOLDER = "restFolder";
    public static final String PLACEHOLDER_TMP_FOLDER = "tmpFolder";
    public static final String PLACEHOLDER_SITE_OVERRIDE_FOLDER = "siteOverrideFolder";
    public static final String PLACEHOLDER_JCR_TODAY_DATE = "jcrDate";
    public static final String PLACEHOLDER_JCR_DATE_NEXT_MONTH = "jcrDateNextMonth";
    public static final String PLACEHOLDER_JCR_DATE_NEXT_YEAR = "jcrDateNextYear";
    /**
     * @see org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated#internalName()
     */
    public static final String ANNOTATION_ATTR_INTERNAL_NAME = "internalName";
    /**
     * @see org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated#date()
     */
    public static final String ANNOTATION_ATTR_DATE = "date";
    /**
     * @see org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated#allowModifications() ()
     */
    public static final String ANNOTATION_ATTR_ALLOW_MODIFICATIONS = "allowModifications";
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String MIME_IMAGE_JPEG = "text/jpeg";
    public static final String MIME_IMAGE_GIF = "image/gif";
    public static final String MIME_IMAGE_PNG = "text/png";
    public static final String MIME_APPLICATION_PDF = "application/pdf";
    public static final String FILE_EXTENSION_JAVA = ".java";
    public static final String SOURCE_PATTERN_JAVA = "java";
    /**
     * Name of the system property set by cargo maven build
     */
    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
    /**
     * Hippo system andd plugin CND namespaces
     */
    // TODO add known plugin namespaces to this list
    public static final Set<String> HIPPO_BUILT_IN_NAMESPACES =
            ImmutableSet.of(
                    "dashboard",
                    "frontend",
                    "ef", // easy forms
                    "hippo",
                    "hst",
                    "system",
                    "hippogallery",
                    "hippogallery",
                    "hippostd",
                    "hippostdpubwf",
                    "hipposysedit",
                    "hippotaxonomy",
                    "hippogallerypi cker");
    public static final String[] XML_FILTER = new String[]{"xml"};
    public static final String[] JAR_FILTER = new String[]{"jar"};
    public static final String[] FTL_FILTER = new String[]{"ftl"};
    public static final String NS_JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String NS_HIPPOSYSEDIT_TEMPLATETYPE = "hipposysedit:templatetype";
    public static final String URI_JCR_NAMESPACE = "http://www.jcp.org/jcr/sv/1.0";
    public static final String URI_AUTOEXPORT_NAMESPACE = "http://www.onehippo.org/jcr/xmlimport";
    /**
     * Namespace of plugin.xml descriptor, HippoEssentials framework
     */
    public static final String URI_ESSENTIALS_PLUGIN = "http://www.onehippo.org/essentials";
    public static final String URI_ESSENTIALS_INSTRUCTIONS = "http://www.onehippo.org/essentials/instructions";
    /**
     * Fully qualified name of HST node annotation
     */
    public static final String NODE_ANNOTATION_FULLY_QUALIFIED = "org.hippoecm.hst.content.beans.Node";
    /**
     * Name of HST Node annotation
     */
    public static final String NODE_ANNOTATION_NAME = "Node";
    public static final String INVALID_METHOD_NAME = "getTODO";
    public static final String INVALID_CLASS_NAME = "InvalidClassName";
    public static final String ANNOTATION_INTERNAL_NAME_ATTRIBUTE = "internalName";
    public static final String POM_XML = "pom.xml";
    public static final String XPATH = "xpath";
    public static final String HIPPOSYSEDIT_PROTOTYPE = "hipposysedit:prototype";
    public static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";
    public static final String HIPPO_COMPOUND = "hippo:compound";
    public static final String HIPPO_COMPOUND_BASE_CLASS = "HippoDocument";
    public static final String HIPPO_DOCUMENT_IMPORT = "org.hippoecm.hst.content.beans.standard.HippoDocument";
    public static final String CONTENT_BLOCKS_TEMPLATE_XML = "content_blocks_template.xml";
    public static final String INSTRUCTION_GROUP_DEFAULT = "default";
    public static final String REPO_FOLDER_FORMAT = "yyyy/MM";


    private EssentialConst() {
    }
}
