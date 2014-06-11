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

package org.onehippo.cms7.essentials.dashboard.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;


/**
 * @version "$Id$"
 */
@XmlTransient
public interface Plugin extends Serializable {


    List<String> getRestClasses();

    void setRestClasses(List<String> restClasses);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getDescription();

    void setDescription(String description);


    boolean isNeedsInstallation();

    void setNeedsInstallation(boolean needsInstallation);

    String getInstallState();

    void setInstallState(String installState);

    boolean isInstalled();

    void setInstalled(boolean installed);

    String getPluginId();

    void setPluginId(String pluginId);

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

    String getIssuesLink();

    void setIssuesLink(String issuesLink);

    String getType();

    void setType(String type);


    List<EssentialsDependency> getDependencies();

    void setDependencies(List<EssentialsDependency> dependencies);

    String getTitle();

    void setTitle(String title);

    String getIntroduction();

    void setIntroduction(String introduction);

}
