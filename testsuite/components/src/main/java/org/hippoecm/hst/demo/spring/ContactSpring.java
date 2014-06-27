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
package org.hippoecm.hst.demo.spring;

import java.io.IOException;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.component.support.forms.FormMap;
import org.hippoecm.hst.component.support.forms.FormUtils;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class ContactSpring extends BaseHstComponent {
    
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
        FormUtils.populate(request, formMap);
        request.setAttribute("form", formMap);
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        super.doAction(request, response);
        FormMap formMap = new FormMap(request, formFields);
        
        if(request.getParameter("prev") != null && request.getParameter("previous") != null) {
            response.setRenderParameter(FormUtils.DEFAULT_UUID_NAME, request.getParameter("previous"));
            return;
        }
        // Do a really simple validation: 
        if(formMap.getField("email") != null && formMap.getField("email").getValue().contains("@")) {
            // success
            // send email here.
            SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
            msg.setFrom(formMap.getField("email").getValue());
            msg.setText(formMap.getField("textarea").getValue());
            
            try {
                this.mailSender.send(msg);
            } catch (Exception e) {
                log.warn("Cannot send message. " + e);
            }
            
            // possible do a redirect to a thankyou page: do not use directly response.sendRedirect;
            HstSiteMapItem item = request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getChild("thankyou");
            
            if (item != null) {
                sendRedirect(request, response, item.getId());
            } else {
                log.warn("Cannot redirect because siteMapItem not found. ");
            }
        } else {
            // validation failed. Persist form map, and add possible error messages to the formMap
            formMap.addMessage("email", "Email address must contain '@'");
            FormUtils.persistFormMap(request, response, formMap, null);
        }
    }
    
    private void sendRedirect(HstRequest request, HstResponse response, String redirectToSiteMapItemId) {
        HstLinkCreator linkCreator = request.getRequestContext().getHstLinkCreator();
        HstLink link = linkCreator.createByRefId(redirectToSiteMapItemId, request.getRequestContext().getResolvedMount().getMount());

        StringBuffer url = new StringBuffer();
        
        for (String elem : link.getPathElements()) {
            String enc = response.encodeURL(elem);
            url.append("/").append(enc);
        }

        String urlString = ((HstResponse) response).createNavigationalURL(url.toString()).toString();
        
        try {
            response.sendRedirect(urlString);
        } catch (IOException e) {
            throw new HstComponentException("Could not redirect. ",e);
        }
    }
    
}


  
