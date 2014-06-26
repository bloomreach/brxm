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
package org.onehippo.cms7.essentials.plugins.taxonomy;

import java.text.MessageFormat;

import javax.jcr.Node;

public class ServiceNameBuilder {

    public static String getServiceName(String docType, String taxonomyName){
        return MessageFormat.format("taxonomyclassification{0}{1}", docType.substring(docType.indexOf(":")+1), taxonomyName);
    }

    public static String getDaoName(String docType, String taxonomyName){
        return MessageFormat.format("taxonomy.classification.{0}.{1}.dao", docType.substring(docType.indexOf(":")+1), taxonomyName);
    }

    public static String getParentPath(String prefix, String documentType){
        return MessageFormat.format("/hippo:namespaces/{0}/{1}/editor:templates/_default_", prefix, documentType);
    }

    public static String getAdditionalTaxonomyNodeName(String prefix, String documentName, String taxonomyName){
        return MessageFormat.format("/hippo:namespaces/{0}/{1}/editor:templates/_default_/{2}", prefix, documentName, taxonomyName);
    }


}
