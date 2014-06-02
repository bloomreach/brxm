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

package org.onehippo.cms7.essentials.dashboard.generated.jaxb;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "web-app", namespace = WebXml.NS)
public class WebXml {

    public static final String NS = "http://java.sun.com/xml/ns/javaee";

    private List<WebContextParam> parameters;

    @XmlElement(namespace = WebXml.NS, name = "context-param")
    public List<WebContextParam> getParameters() {
        return parameters;
    }

    public void setParameters(final List<WebContextParam> parameters) {
        this.parameters = parameters;
    }

    //############################################
    // UTILS
    //############################################

    /**
     * Get HST {@code hst-beans-annotated-classes} web.xml context value
     *
     * @return null if not defined
     */
    public String getHstBeanContextValue() {
        if (parameters == null) {
            return null;
        }
        for (WebContextParam parameter : parameters) {
            if ("hst-beans-annotated-classes".equals(parameter.getParamName())) {
                return parameter.getParamValue();
            }
        }
        return null;
    }


    /**
     * Sets hst beans context value
     *
     * @param stream     content stream of web.xml file
     * @param valueToAdd new context param value
     * @return all content of web.xml (with replaced param value)
     */
    public String addToHstBeanContextValue(final InputStream stream, final String valueToAdd) {

        final String webXmlContent = GlobalUtils.readStreamAsText(stream);
        final String hstBeanContextValue = getHstBeanContextValue();
        // extract first part
        final int startIndex = webXmlContent.indexOf(hstBeanContextValue);
        final String firstPart = webXmlContent.substring(0, startIndex);
        // extract second part
        final int endIndex = startIndex + hstBeanContextValue.length();
        final String secondPart = webXmlContent.substring(endIndex, webXmlContent.length());
        // just add first values
        return firstPart + hstBeanContextValue + ',' + valueToAdd + secondPart;

    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXml{");
        sb.append("parameters=").append(parameters);
        sb.append('}');
        return sb.toString();
    }
}
