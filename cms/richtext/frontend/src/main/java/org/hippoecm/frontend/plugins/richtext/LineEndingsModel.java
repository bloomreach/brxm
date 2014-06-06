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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;

/**
 * Model that converts LF line endings in the delegate to CRLF. Settings a new value
 * will convert CRLF line endings back to LF.
 */
public class LineEndingsModel implements IModel<String> {

    private static final String LF = "\n";
    private static final String CRLF = "\r\n";

    private IModel<String> delegate;

    public LineEndingsModel(IModel<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getObject() {
        final String valueWithLF = delegate.getObject();
        final String valueWithCRLF = StringUtils.replace(valueWithLF, LF, CRLF);
        return valueWithCRLF;
    }

    @Override
    public void setObject(final String newValue) {
        final String valueWithLF = StringUtils.replace(newValue, CRLF, LF);
        delegate.setObject(valueWithLF);
    }

    @Override
    public void detach() {
        delegate.detach();
    }

}
