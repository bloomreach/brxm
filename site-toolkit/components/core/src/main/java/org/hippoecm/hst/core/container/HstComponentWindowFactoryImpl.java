/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentFatalException;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstComponentWindowFactoryImpl
 *
 * @version $Id$
 */
public class HstComponentWindowFactoryImpl implements HstComponentWindowFactory {

    private static final Logger log = LoggerFactory.getLogger(HstComponentWindowFactoryImpl.class);

    protected String referenceNameSeparator = "_";

    public void setReferenceNameSeparator(String referenceNameSeparator) {
        this.referenceNameSeparator = referenceNameSeparator;
    }

    public String getReferenceNameSeparator() {
        return this.referenceNameSeparator;
    }

    public HstComponentWindow create(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException {
        return create(requestContainerConfig, requestContext, compConfig, compFactory, null);
    }

    public HstComponentWindow create(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException {
        String referenceName = compConfig.getReferenceName();
        StringBuilder referenceNamespaceBuilder = new StringBuilder();

        String parentReferenceNamespace = null;
        if (parentWindow != null) {
            parentReferenceNamespace =  parentWindow.getReferenceNamespace();
        }

        if (parentReferenceNamespace == null || "".equals(parentReferenceNamespace)) {
            referenceNamespaceBuilder.append(referenceName);
        } else {
            referenceNamespaceBuilder.append(parentReferenceNamespace).append(this.referenceNameSeparator).append(referenceName);
        }

        String referenceNamespace = referenceNamespaceBuilder.toString();

        HstComponent component = null;
        HstComponentMetadata componentMetadata = null;
        HstComponentException componentFactoryException = null;

        try {
            Mount mount = requestContext.getResolvedMount().getMount();
            component = compFactory.getComponentInstance(requestContainerConfig, compConfig, mount);
            componentMetadata = compFactory.getComponentMetadata(requestContainerConfig, compConfig, mount);
        } catch (HstComponentFatalException e) {
            throw e;
        } catch (HstComponentException e) {
            componentFactoryException = e;
        } catch (Exception e) {
            componentFactoryException = new HstComponentException(e);
        }
        
        String componentName = compConfig.getComponentClassName();
        
        if (StringUtils.isBlank(componentName)) {
            componentName = compFactory.getDefaultHstComponentClassName();
        }
        
        HstComponentWindowImpl window =
            new HstComponentWindowImpl(
                compConfig,
                componentName,
                component,
                componentMetadata,
                parentWindow,
                referenceNamespace,
                compConfig.getParametersInfoClassName());

        if (componentFactoryException != null) {
            window.addComponentExcpetion(componentFactoryException);
        }

        Map<String, HstComponentConfiguration> childCompConfigMap = compConfig.getChildren();

        if (childCompConfigMap != null && !childCompConfigMap.isEmpty()) {
            Set<String> filter = requestContext.getComponentFilterTags();
            boolean matchTag = false;
            if (!filter.isEmpty()) {
                for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                    HstComponentConfiguration childCompConfig = entry.getValue();
                    String tag = childCompConfig.getComponentFilterTag();
                    if (tag != null && filter.contains(tag)) {
                        matchTag = true;
                        break;
                    }
                }
            }
            for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                HstComponentConfiguration childCompConfig = entry.getValue();
                if (childCompConfig.isMarkedDeleted()) {
                    log.debug("Skipping child component {} because it is marked to be deleted.", childCompConfig);
                    continue;
                }
                String tag = childCompConfig.getComponentFilterTag();
                if (matchTag) {
                    if (tag == null || !filter.contains(tag)) {
                        continue;
                    }
                } else if (tag != null) {
                    continue;
                }
                
                HstComponentWindow childCompWindow = create(requestContainerConfig, requestContext, childCompConfig, compFactory, window);
                
                // now available filters of HstComponentWindow's can be done. It can only be done after all descendant windows have been created
                // because otherwise a filter could not for example remove descendant windows
                for (HstComponentWindowFilter creationFilter : requestContext.getComponentWindowFilters()) {
                    childCompWindow = creationFilter.doFilter(requestContext, childCompConfig, childCompWindow);
                    if (childCompWindow == null) {
                        // comp window is completely skipped/disabled
                        break;
                    }
                }
                
                if (childCompWindow != null) {
                    window.addChildWindow(childCompWindow);
                    if (!window.isVisible()) {
                        childCompWindow.setVisible(false);
                    }
                }
            }
        }

        return window;
    }

}
