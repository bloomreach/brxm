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

import java.util.Date;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.PropertyValueProvider;

public class MapEmptyDateToNullModel implements IModel<Date> {

    private IModel<Date> delegate;

    public MapEmptyDateToNullModel(IModel<Date> valueModel) {
        delegate = valueModel;
    }

    @Override
    public Date getObject() {
        final Date object = delegate.getObject();
        if (object != null && object.equals(PropertyValueProvider.EMPTY_DATE)) {
            return null;
        }
        return object;
    }

    @Override
    public void setObject(final Date date) {
        if (date == null) {
            delegate.setObject(PropertyValueProvider.EMPTY_DATE);
        } else {
            delegate.setObject(date);
        }
    }

    @Override
    public void detach() {
        delegate.detach();
    }

}
