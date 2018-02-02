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

public final class SourceCodeTemplates {

    public static final String TEMPLATE_DOCBASE_MULTIPLE = "class DocbaseMultiple{ " +
            "  @HippoEssentialsGenerated(internalName = \"$internalName$\")\n" +
            "    public List<HippoBean> $methodName$() {\n" +
            "        final List<HippoBean> beans = new ArrayList<>();\n" +
            "        final String[] items = getProperty(\"$internalName$\");\n" +
            "        if (items == null) {\n" +
            "            return beans;\n" +
            "        }\n" +
            "        for (String item : items) {\n" +
            "            final HippoBean bean = getBeanByUUID(item, HippoBean.class);\n" +
            "            if (bean != null) {\n" +
            "                beans.add(bean);\n" +
            "            }\n" +
            "        }\n" +
            "        return beans;\n" +
            "    }" +
            "}";
    public static final String TEMPLATE_DOCBASE = "class Docbase{ " +
            "  @HippoEssentialsGenerated(internalName = \"$internalName$\")\n" +
            "    public HippoBean $methodName$() {\n" +
            "        final String item = getProperty(\"$internalName$\");\n" +
            "        if (item == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        return getBeanByUUID(item, HippoBean.class);\n" +
            "\n" +
            "    }" +
            "}";

    private SourceCodeTemplates() {
    }
}
