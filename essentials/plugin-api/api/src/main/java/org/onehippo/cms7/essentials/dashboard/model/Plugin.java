/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonSubTypes;


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

    boolean isInstalled();

    void setInstalled(boolean installed);

    String getPluginLink();

    void setPluginLink(String pluginLink);


    Vendor getVendor();

    void setVendor(Vendor vendor);

    String getName();

    void setName(String name);

    String getDocumentationLink();

    void setDocumentationLink(String documentationLink);

    String getIssuesLink();

    void setIssuesLink(String issuesLink);

    String getType();

    void setType(String type);


    List<Dependency> getDependencies();

    void setDependencies(List<Dependency> dependencies);

    String getPluginClass();

    void setPluginClass(String pluginClass);
}
