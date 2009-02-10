package org.hippoecm.hst.service;

@ServiceNamespace(prefix="myproject")
public interface IArticle extends IDocument {
    
    void setTitle(String title);

    String getTitle();
    
    void setContent(String content);
    
    String getContent();

}
