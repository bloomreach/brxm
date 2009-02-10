package org.hippoecm.hst.service;

public interface IBlogArticle extends IArticle {

    void setComments(String [] comments);
    
    String [] getComments();
    
}
