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
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * An example form controller extending SimpleFormController.
 * <P>
 * This example demonstrates the usual simple form controller usage provided by spring web mvc framework.
 * If the property, 'redirectOnSuccess', is set to true, this controller returns a <CODE>RedirectView</CODE>.
 * Otherwise, this returns the success view by default.
 * </P>
 * 
 * @version $Id: ContactFormController.java 18406 2009-06-05 11:07:25Z wko $
 */
public class ContactFormController extends SimpleFormController {
    
    private static Logger log = LoggerFactory.getLogger(ContactFormController.class);

    private MailSender mailSender;
    private SimpleMailMessage templateMessage;
    private boolean redirectOnSuccess;
    
    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void setTemplateMessage(SimpleMailMessage templateMessage) {
        this.templateMessage = templateMessage;
    }
    
    public void setRedirectOnSuccess(boolean redirectOnSuccess) {
        this.redirectOnSuccess = redirectOnSuccess;
    }

    protected void sendContactMessage(ContactMessageBean messageBean) throws Exception {
        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setFrom(messageBean.getEmail());
        msg.setText(messageBean.getMessage());
        
        try {
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Cannot send message. " + e);
        }
    }
    
    @Override
    protected void doSubmitAction(Object command) throws Exception {
        if (redirectOnSuccess) {
            super.doSubmitAction(command);
        } else {
            sendContactMessage((ContactMessageBean) command);
        }
    }

    @Override
    protected ModelAndView onSubmit(Object command, BindException errors) throws Exception {
        if (!redirectOnSuccess) {
            return super.onSubmit(command, errors);
        } else {
            sendContactMessage((ContactMessageBean) command);
            return new ModelAndView(new SiteMapItemRedirectView("thankyou", true));
        }
    }
    
}
