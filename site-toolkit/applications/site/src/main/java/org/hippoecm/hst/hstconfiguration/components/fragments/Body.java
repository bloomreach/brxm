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
package org.hippoecm.hst.hstconfiguration.components.fragments;

import java.io.IOException;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagetypes.NewsPage;
import org.hippoecm.hst.pagetypes.StdType;

public class Body extends GenericHstComponent {
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        System.out.println("[HstComponent: " + getClass().getName() + "] doAction() with params: " + request.getParameterMap());
        
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

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        HstRequestContext hrc = request.getRequestContext();
        
        
//        String siteMapPath = hrc.getMatchedSiteMapItem().getSiteMapItem().getRelativeContentPath();
//        String remainder = hrc.getMatchedSiteMapItem().getRemainder();
//        
//        try {
//            Session s = hrc.getSession();
//            Node contentNode = (Node)s.getItem(hrc.getMatchedSiteMapItem().getSiteMapItem().getHstSiteMap().getSite().getContentPath());
//            
//            Node siteMapEntryNode = contentNode.getNode(siteMapPath);
//            Node target = siteMapEntryNode;
//            
//            if(remainder != null && !"".equals(remainder) ) {
//                if(!siteMapEntryNode.hasNode(remainder)) {
//                    // the path does not result in any node. Return
//                    return;
//                }
//                target = siteMapEntryNode.getNode(remainder);
//                
//            }
//                
//            Node parentNode = target.getParent();
//            StdType pstdType =  ServiceFactory.create(parentNode, StdType.class);
//            HstLink parentLink = hrc.getHstLinkCreator().create(pstdType.getUnderlyingService(), hrc.getMatchedSiteMapItem().getSiteMapItem());
//            
//            Parent p  = new Parent(pstdType, parentLink == null ? "" :  parentLink.getPath());
//            request.setAttribute("parent", p); 
//            
//            if(target.isNodeType(HippoNodeType.NT_HANDLE)) {
//                if(target.hasNode(target.getName())) {
//                    NewsPage newsPage = ServiceFactory.create(target.getNode(target.getName()), NewsPage.class);
//                    
//                    HstLink link = hrc.getHstLinkCreator().create(newsPage.getUnderlyingService(), hrc.getMatchedSiteMapItem().getSiteMapItem());
//                    Document d = new Document(newsPage, link == null ? "" :  link.getPath());
//                    request.setAttribute("document", d); 
//                }
//            }
//            else {
//                NodeIterator it = target.getNodes();
//                List<Document> docs = new ArrayList<Document>();
//                List<Folder> folders = new ArrayList<Folder>();
//                
//                while(it.hasNext()) {
//                    Node n = it.nextNode();
//                    if(n!= null){
//                        if(n.isNodeType(HippoNodeType.NT_HANDLE)) {
//                            if(n.hasNode(n.getName())) {
//                                NewsPage newsPage = ServiceFactory.create(n.getNode(n.getName()), NewsPage.class);
//                                
//                                HstLink link = hrc.getHstLinkCreator().create(newsPage.getUnderlyingService(), hrc.getMatchedSiteMapItem().getSiteMapItem());
//                                docs.add(new Document(newsPage, link == null ? "" :  link.getPath()));
//                            }
//                        } else if(n.isNodeType(HippoNodeType.NT_DOCUMENT) && !n.getParent().isNodeType(HippoNodeType.NT_HANDLE)){
//                           StdType stdType =  ServiceFactory.create(n, StdType.class);
//                           HstLink link = hrc.getHstLinkCreator().create(stdType.getUnderlyingService(), hrc.getMatchedSiteMapItem().getSiteMapItem());
//                           
//                           folders.add(new Folder(stdType, link == null ? "" :  link.getPath()));
//                        }
//                    }
//                }
//
//                request.setAttribute("documents", docs); 
//                request.setAttribute("folders", folders); 
//            }   
//            
//            
//            
//        } catch (PathNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (LoginException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (RepositoryException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        
    }

    
    public class Document {
        
        private NewsPage p;
        private String link;
        
        public Document(NewsPage p, String link) {
            this.p = p;
            this.link = link;
        }
        public NewsPage getPage() {
            return p;
        }
        public void setPage(NewsPage p) {
            this.p = p;
        }
        public String getLink() {
            return link;
        }
        public void setLink(String link) {
            this.link = link;
        }
    }

    public class Folder {
        private String name;
        private String link;
        
        public Folder(StdType f, String link) {
            this.name = f.getUnderlyingService().getValueProvider().getName();
            this.link = link;
        }
        
        public String getLink() {
            return link;
        }
        public String getName() {
            return name;
        }
    }
    
    public class Parent {
        private String name;
        private String link;
        
        public Parent(StdType f, String link) {
            this.name = f.getUnderlyingService().getValueProvider().getName();
            this.link = link;
        }
        
        public String getLink() {
            return link;
        }
        public String getName() {
            return name;
        }
    }
}
