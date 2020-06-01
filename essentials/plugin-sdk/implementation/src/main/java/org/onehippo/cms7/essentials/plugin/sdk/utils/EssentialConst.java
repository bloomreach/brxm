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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import com.google.common.collect.ImmutableSet;

import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

public final class EssentialConst {

    public static final String WEB_INF_LIB = "/WEB-INF/lib/";
    public static final String JAR_EXT = ".jar";

    /**
     * @see HippoEssentialsGenerated#internalName()
     */
    public static final String ANNOTATION_ATTR_INTERNAL_NAME = "internalName";
    /**
     * @see HippoEssentialsGenerated#date()
     */
    public static final String ANNOTATION_ATTR_DATE = "date";
    /**
     * @see HippoEssentialsGenerated#allowModifications() ()
     */
    public static final String ANNOTATION_ATTR_ALLOW_MODIFICATIONS = "allowModifications";
    public static final String FILE_EXTENSION_JAVA = ".java";
    public static final String WEBFILES_PREFIX = "webfile:";

    /**
     * Name of the system property set by cargo maven build
     */
    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
    /**
     * System property for essentials itself (if moved to another context)
     */
    public static final String ESSENTIALS_BASEDIR_PROPERTY = "essentials.dir";
    public static final String NS_JCR_PRIMARY_TYPE = "jcr:primaryType";
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
    public static final String HIPPOSYSEDIT_PROTOTYPE = "hipposysedit:prototype";
    public static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";
    public static final String HIPPO_COMPOUND_IMPORT = "org.hippoecm.hst.content.beans.standard.HippoCompound";
    public static final String HIPPO_BEAN_IMPORT = "org.hippoecm.hst.content.beans.standard.HippoBean";
    public static final String HIPPO_RELATED_DOCS_IMPORT = "org.onehippo.forge.beans.RelatedDocsBean";
    public static final String HIPPO_IMAGE_SET_IMPORT = "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet";
    public static final String INSTRUCTION_GROUP_DEFAULT = "default";
    public static final ImmutableSet<String> DEFAULT_GROUPS = new ImmutableSet.Builder<String>().add(INSTRUCTION_GROUP_DEFAULT).build();
    public static final String REPO_FOLDER_FORMAT = "yyyy/MM";
    public static final String UTF_8 = "UTF8";
    public static final String DEFAULT_INSTRUCTIONS_PATH = "/META-INF/instructions.xml";
    public static final String PROP_SAMPLE_DATA = "sampleData";
    public static final String PROP_EXTRA_TEMPLATES = "extraTemplates";
    public static final String PROP_PLUGIN_DESCRIPTOR = "pluginDescriptor";
    public static final String METHOD_RELATED_DOCUMENTS = "getRelatedDocuments";
    public static final String RELATED_DOCS_BEAN = "RelatedDocsBean";
    public static final String RELATEDDOCS_DOCS = "relateddocs:docs";

    private EssentialConst() {
    }
}
