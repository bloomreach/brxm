/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model.properties;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

public class LongConverter implements IModel<Long> {

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(LongConverter.class);
    private JcrPropertyValueModel decorated;

    public LongConverter(JcrPropertyValueModel valueModel) {
        decorated = valueModel;
    }

    public Long getObject() {
        try {
            if (decorated != null && decorated.getValue() != null) {
                return decorated.getValue().getLong();
            } else {
                log.debug("LongConverter: JcrPropertyValueModel decorated equals null");
            }
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

    public void setObject(Long object) {
        try {
            long longValue = object == null ? 0L : object;
            ValueFactory factory = UserSession.get().getJcrSession().getValueFactory();
            Value value = factory.createValue(longValue);
            decorated.setValue(value);
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
    }

    public void detach() {
        decorated.detach();
    }

}
