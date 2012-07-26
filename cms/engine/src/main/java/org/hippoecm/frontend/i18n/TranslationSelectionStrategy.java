/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.i18n;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationSelectionStrategy<T extends IModel> implements Comparator<ITranslation<T>> {

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(ConfigTraversingPlugin.class);

    private Set<String> keys;

    public TranslationSelectionStrategy(Set<String> keys) {
        this.keys = keys;
    }

    public ITranslation<T> select(Set<? extends ITranslation<T>> candidates) {
        if (candidates.size() > 0) {
            return Collections.max(candidates, this);
        }
        return new ITranslation<T>() {
            private static final long serialVersionUID = 1L;

            public Set<String> getMatchingCriteria() {
                return keys;
            }

            public T getModel() {
                return null;
            }

        };
    }

    public int compare(ITranslation<T> o1, ITranslation<T> o2) {
        Set<String> o1Matches = o1.getMatchingCriteria();
        Set<String> o2Matches = o2.getMatchingCriteria();
        for (String key : keys) {
            boolean match1 = o1Matches.contains(key);
            boolean match2 = o2Matches.contains(key);
            if (match1 && !match2) {
                return -1;
            } else if (!match1 && match2) {
                return 1;
            }
        }
        if (o1Matches.size() > o2Matches.size()) {
            return 1;
        } else if (o1Matches.size() < o2Matches.size()) {
            return -1;
        }
        return 0;
    }

}
