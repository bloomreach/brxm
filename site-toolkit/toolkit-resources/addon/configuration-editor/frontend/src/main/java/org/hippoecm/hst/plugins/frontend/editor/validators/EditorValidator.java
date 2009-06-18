/*
 *  Copyright 2009 Hippo.
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

package org.hippoecm.hst.plugins.frontend.editor.validators;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class EditorValidator<K extends EditorBean> extends AbstractValidator {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorValidator.class);

    protected String resourceKey = "nodevalidator.default";
    private IValidatable validatable;

    private BeanProvider<K> provider;

    public EditorValidator(BeanProvider<K> provider) {
        this.provider = provider;
    }

    @Override
    protected final void onValidate(IValidatable validatable) {
        this.validatable = validatable; //preserve for error handling
        onValidate(validatable, provider.getBean());
        this.validatable = null;
    }

    protected void error(String resourceKey) {
        this.resourceKey = resourceKey;
        error(validatable);
    }

    @Override
    protected String resourceKey() {
        return resourceKey;
    }

    protected abstract void onValidate(IValidatable validatable, K bean);
}