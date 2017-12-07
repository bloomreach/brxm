/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;


@XmlTransient
public interface PluginDescriptor extends Serializable {


    Map<String, Set<String>> getCategories();

    void setCategories(Map<String, Set<String>> categories);

    List<String> getRestClasses();

    void setRestClasses(List<String> restClasses);

    String getDescription();

    void setDescription(String description);

    String getIcon();

    void setIcon(String icon);

    List<String> getImageUrls();

    void setImageUrls(List<String> imageUrls);

    String getInstallState();

    void setInstallState(String installState);

    String getId();

    void setId(String id);

    String getPackageFile();

    void setPackageFile(String packageFile);

    Vendor getVendor();

    String getPackageClass();

    void setPackageClass(String packageClass);

    void setVendor(Vendor vendor);

    String getName();

    void setName(String name);

    String getDocumentationLink();

    void setDocumentationLink(String documentationLink);

    String getType();

    void setType(String type);


    List<ModuleMavenDependency> getDependencies();

    void setDependencies(List<ModuleMavenDependency> dependencies);

    String getIntroduction();

    void setIntroduction(String introduction);

    List<Repository> getRepositories();

    void setRepositories(List<Repository> repositories);

    void setHasConfiguration(boolean hasConfiguration);

    boolean getHasConfiguration();
}
