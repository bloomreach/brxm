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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class Action extends GenericResourceServingHstComponent {
    
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        String s = this.getParameter("foo" , request);
        
        HippoBean  n = this.getContentBean(request);
        
        if(n == null) {
            return;
        }
        
        request.setAttribute("parent", n.getParentBean());
        request.setAttribute("current",(n));
        
        if(n.isHippoFolderBean()) {
            request.setAttribute("collections",((HippoFolder)n).getFolders());
            request.setAttribute("documents",((HippoFolder)n).getDocuments());
        }
        
        
        
    }


    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
       
        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                
                Map<String, String> fields = new HashMap<String, String>();
                List<FileItem> items = (List<FileItem>) upload.parseRequest(request);
                
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        fields.put(item.getFieldName(), item.getString());
                    }
                }
                
                String caption = fields.get("caption");
                
                String fileStoreDirPath = getServletConfig().getServletContext().getRealPath("/WEB-INF/file-storage");
                File fileStoreDir = new File(fileStoreDirPath);
                
                if (!fileStoreDir.isDirectory()) {
                    fileStoreDir.mkdirs();
                }
                
                File storeFile = new File(fileStoreDir, caption);
                
                for (FileItem item : items) {
                    if (!item.isFormField()) {
                        item.write(storeFile);
                    }
                }
            } catch (FileUploadException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String sort = request.getParameter("sort");
            String redirect = request.getParameter("redirect");
            
            if (redirect != null && !"".equals(redirect)) {
                try {
                    response.sendRedirect(redirect);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            
            if ("descending".equals(sort)) {
                response.setRenderParameter("sortpage", "descending-10");
            } else {
                response.setRenderParameter("sortpage", "" + sort + "-00");
            }
        }
        
    }
}


  
