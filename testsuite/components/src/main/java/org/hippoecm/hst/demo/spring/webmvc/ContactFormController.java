/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

//import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * An example form controller extending SimpleFormController.
 * <p/>
 * This example demonstrates the usual simple form controller usage provided by spring web mvc framework. If the
 * property, 'redirectOnSuccess', is set to true, this controller returns a <CODE>RedirectView</CODE>. Otherwise, this
 * returns the success view by default. </P>
 *
 * @version $Id: ContactFormController.java 18406 2009-06-05 11:07:25Z wko $
 */
@Controller
@RequestMapping("/spring/contactspringmvc.do")
public class ContactFormController {

    private static Logger log = LoggerFactory.getLogger(ContactFormController.class);

    @Autowired
    private MailSender mailSender;

    @Autowired
    private SimpleMailMessage templateMessage;

    @Autowired
    private ContactMessageValidator contactMessageValidator;

    @RequestMapping(method = RequestMethod.GET)
    public String initializeForm(final ModelMap model) {
        model.addAttribute("contactMessage", new ContactMessageBean());
        return "spring/contactspringmvc-form";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String onSubmit(@ModelAttribute("contactMessageBean") ContactMessageBean contactMessageBean,
                           BindingResult result) {
        contactMessageValidator.validate(contactMessageBean, result);

        if (result.hasErrors()) {
            log.warn("Binding errors");
            return "spring/contactspringmvc-form";
        }

        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setFrom(contactMessageBean.getEmail());
        msg.setText(contactMessageBean.getMessage());

        try {
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Cannot send message. " + e);
            return "spring/contactspringmvc-form";
        }

        return "spring/contactspringmvc-success";
    }


}
