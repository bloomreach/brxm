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
package org.hippoecm.hst.springmvc;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class ContactFormController extends SimpleFormController {

    private MailSender mailSender;
    private SimpleMailMessage templateMessage;
    
    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void setTemplateMessage(SimpleMailMessage templateMessage) {
        this.templateMessage = templateMessage;
    }
    
    @Override
    protected ModelAndView onSubmit(Object command, BindException errors) throws Exception {
        ModelAndView mv = null;
        
        ContactMessageBean bean = (ContactMessageBean) command;
        
        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setFrom(bean.getEmail());
        msg.setText(bean.getMessage());
        mailSender.send(msg);
        
        mv = new ModelAndView(new SiteMapItemRedirectView("thankyou", true));
        
        return mv;
    }
    
}
