/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.content.beans.standard;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;

public class MockHippoBean implements HippoBean {

    public boolean equalCompare(Object compare) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T> T getBean(String relPath) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T extends HippoBean> T getBean(String relPath, Class<T> beanMappingClass) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getCanonicalUUID() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T extends HippoBean> List<T> getChildBeans(Class<T> beanMappingClass) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T> List<T> getChildBeans(String jcrPrimaryNodeType) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T> List<T> getChildBeansByName(String childNodeName) throws ClassCastException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T extends HippoBean> List<T> getChildBeansByName(String childNodeName, Class<T> beanMappingClass) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public <T extends HippoBean> T getCanonicalBean() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public Map<Object, Object> getEqualComparator() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T extends HippoBean> T getLinkedBean(String relPath, Class<T> beanMappingClass) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T extends HippoBean> List<T> getLinkedBeans(String relPath, Class<T> beanMappingClass) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T extends HippoBean> T getBeanByUUID(String uuid, Class<T> beanMappingClass) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getLocalizedName() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public Node getNode() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public HippoBean getParentBean() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void setIdentifier(final String identifier) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getPath() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public <T> T getProperty(String name) {
        throw new UnsupportedOperationException("Not supported yet");
    }
    
    public <T> T getProperty(String name, T defaultValue) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public Map<String, Object> getProperty() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public JCRValueProvider getValueProvider() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public boolean isAncestor(HippoBean compare) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public boolean isDescendant(HippoBean compare) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public boolean isHippoDocumentBean() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public boolean isHippoFolderBean() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public boolean isLeaf() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public boolean isSelf(HippoBean compare) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public void setNode(Node node) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public ObjectConverter getObjectConverter() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public int compareTo(HippoBean o) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String getCanonicalPath() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
