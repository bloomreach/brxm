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
package org.hippoecm.hst.components;

import org.hippoecm.hst.component.support.forms.BaseFormHstComponent;
import org.hippoecm.hst.component.support.forms.FormMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class ContactSpring extends BaseFormHstComponent {
    
    static Logger log = LoggerFactory.getLogger(ContactSpring.class);
    
    private static String[] formFields = {"name","email","textarea"};
    
    private MailSender mailSender;
    private SimpleMailMessage templateMessage;
    
    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void setTemplateMessage(SimpleMailMessage templateMessage) {
        this.templateMessage = templateMessage;
    }
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        FormMap formMap = new FormMap();
        super.populate(request, formMap);
        request.setAttribute("form", formMap);
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        super.doAction(request, response);
        FormMap formMap = new FormMap(request, formFields);
        
        if(request.getParameter("prev") != null && request.getParameter("previous") != null) {
            response.setRenderParameter(DEFAULT_UUID_NAME, request.getParameter("previous"));
            return;
        }
        // Do a really simple validation: 
        if(formMap.getField("email") != null && formMap.getField("email").contains("@")) {
            // success
            // send email here.
            SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
            msg.setFrom(formMap.getField("email"));
            msg.setText(formMap.getField("textarea"));
            
            try {
                this.mailSender.send(msg);
                
                // possible do a redirect to a thankyou page: do not use directly response.sendRedirect;
                HstSiteMapItem item = request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getChild("thankyou");
                
                if (item != null) {
                    this.sendRedirect(request, response, item.getId());
                } else {
                    log.warn("Cannot redirect because siteMapItem not found. ");
                }
            } catch (Exception e) {
                formMap.addMessage("email", "Failed to send email: " + e);
            }
        } else {
            // validation failed. Persist form map, and add possible error messages to the formMap
            formMap.addMessage("email", "Email address must contain '@'");
            super.persistFormMap(request, response, formMap, null);
        }
    }
    
}


  
