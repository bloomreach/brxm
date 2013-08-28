/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This bean is only to map a hippotranslation:translation to a bean: Normally, you never use this bean at all, as translations are
 * available through {@link org.hippoecm.repository.api.HippoNode#getLocalizedName()}
 * @deprecated since 2.26.01 : Use {@link AvailableTranslations} pojo which is not backed by a jcr node instead and does NOT
 * extend from {@link HippoItem} and does not implement {@link HippoBean} at all
 */

@Deprecated
@Indexable(ignore = true)
@Node(jcrType="hippotranslation:translations")
public class HippoAvailableTranslations<K extends HippoBean> extends HippoItem implements HippoAvailableTranslationsBean<K> {

    private static final Logger log = LoggerFactory.getLogger(HippoAvailableTranslations.class);

    @Override
    public List<String> getAvailableLocales() {
        log.warn("HippoAvailableTranslations is deprecated. Use AvailableTranslations instead");
        return Collections.emptyList();
    }

    @Override
    public boolean hasTranslation(final String locale) {
        log.warn("HippoAvailableTranslations is deprecated. Use AvailableTranslations instead");
        return false;
    }

    @Override
    public List<K> getTranslations() throws ClassCastException {
        log.warn("HippoAvailableTranslations is deprecated. Use AvailableTranslations instead");
        return Collections.emptyList();
    }

    @Override
    public K getTranslation(final String locale) throws ClassCastException {
        log.warn("HippoAvailableTranslations is deprecated. Use AvailableTranslations instead");
        return null;
    }
}