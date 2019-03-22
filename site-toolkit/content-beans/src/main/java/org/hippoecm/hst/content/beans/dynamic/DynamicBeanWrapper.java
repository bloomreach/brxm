/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoItem;

public class DynamicBeanWrapper {

    HippoItem hippoItem;

    public DynamicBeanWrapper(HippoItem item) {
        hippoItem = item;
    }

    public String getStringValue(String path) {
        return hippoItem.getProperty(path);
    }

    public String[] getStringValues(String path) {
        return hippoItem.getProperty(path);
    }

    public Calendar getDateValue(String path) {
        return hippoItem.getProperty(path);
    }

    public Calendar[] getDateValues(String path) {
        return hippoItem.getProperty(path);
    }

    public HippoHtml getHtmlValue(String path) {
        return hippoItem.getBean(path, HippoHtml.class);
    }

    public List<HippoHtml> getHtmlValues(String path) {
        return hippoItem.getChildBeansByName(path, HippoHtml.class);
    }

    public HippoGalleryImageSet getImageValue(String path) {
        return hippoItem.getLinkedBean(path, HippoGalleryImageSet.class);
    }

    public List<HippoGalleryImageSet> getImageValues(String path) {
        return hippoItem.getLinkedBeans(path, HippoGalleryImageSet.class);
    }

    public HippoBean getLinkedDocument(String path) {
        List<HippoBean> beans = hippoItem.getChildBeansByName(path, HippoBean.class);
        if (!beans.isEmpty())
            return beans.get(0);
        return null;
    }

    public List<HippoBean> getLinkedDocuments(String path) {
        return hippoItem.getChildBeansByName(path, HippoBean.class);
    }

    public Boolean getBooleanValue(String path) {
        return hippoItem.getProperty(path);
    }

    public Boolean[] getBooleanValues(String path) {
        return hippoItem.getProperty(path);
    }

    public Double getDoubleValue(String path) {
        return hippoItem.getProperty(path);
    }

    public Double[] getDoubleValues(String path) {
        return hippoItem.getProperty(path);
    }

    public Long getLongValue(String path) {
        return hippoItem.getProperty(path);
    }

    public Long[] getLongValues(String path) {
        return hippoItem.getProperty(path);
    }

    public HippoBean getDocbaseValue(String path) {
        final String item = hippoItem.getProperty(path);
        if (item == null) {
            return null;
        }
        return hippoItem.getBeanByUUID(item, HippoBean.class);
    }

    public List<HippoBean> getDocbaseValues(String path) {
        final List<HippoBean> beans = new ArrayList<HippoBean>();
        final String[] items = hippoItem.getProperty(path);
        if (items == null) {
            return beans;
        }
        for (String item : items) {
            final HippoBean bean = hippoItem.getBeanByUUID(item, HippoBean.class);
            if (bean != null) {
                beans.add(bean);
            }
        }
        return beans;
    }

}
