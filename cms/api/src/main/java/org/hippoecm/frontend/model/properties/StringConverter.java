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
package org.hippoecm.frontend.model.properties;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringConverter implements IModel<String> {

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(StringConverter.class);
    private JcrPropertyValueModel decorated;

    public StringConverter(JcrPropertyValueModel valueModel) {
        decorated = valueModel;
    }

    public String getObject() {
        try {
            if (decorated != null && decorated.getValue() != null) {
                return decorated.getValue().getString();
            } else {
                log.debug("StringConverter: JcrPropertyValueModel decorated equals null");
            }
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

    public void setObject(String object) {
        try {
            String string = object == null ? "" : object;
            int type = decorated.getType() == PropertyType.UNDEFINED ? PropertyType.STRING : decorated.getType();
            ValueFactory factory = UserSession.get().getJcrSession().getValueFactory();
            Value value = factory.createValue(string, type);
            decorated.setValue(value);
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
    }

    public void detach() {
        decorated.detach();
    }

}
