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
package org.hippoecm.hst.demo.spring.webmvc;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class ContactMessageValidator implements Validator {
    
    public boolean supports(Class clazz) {
        return ContactMessageBean.class.isAssignableFrom(clazz);
    }

    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name","field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email","field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "message","field.required");
        
        ContactMessageBean bean = (ContactMessageBean) target;

        // Do a really simple validation for email address:
        if (bean.getEmail() != null && !bean.getEmail().contains("@")) {
            errors.rejectValue("email", "field.invalid.email.address");
        }
    }
    
}
