/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.webapp.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

public class JcrPropertiesDataProvider extends JcrNodeModel implements IDataProvider {
    private static final long serialVersionUID = 1L;

    public JcrPropertiesDataProvider(String path) {
        super(path);
    }

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        try {
            PropertyIterator it = getNode().getProperties();
            it.skip(first);            
            for (int i = 0; i < count; i++) {
                list.add(it.nextProperty());
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        Property prop = (Property) object;
        JcrPropertyModel result = null;
        try {
            result = new JcrPropertyModel(prop.getPath());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public int size() {
        int result = 0;
        try {
            result = new Long(getNode().getProperties().getSize()).intValue();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }
    

}
