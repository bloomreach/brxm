package org.hippoecm.hst.service;

import java.util.Calendar;

@ServiceNamespace(prefix = "myblog")
public interface IBlog {

    void setTitle(String title);

    String getTitle();
    
    void setContent(String content);
    
    String getContent();
    
    @ServiceNamespace(prefix = "hippostd")
    void setWritable(boolean writable);
    
    @ServiceNamespace(prefix = "hippostd")
    boolean isWritable();
    
    @ServiceNamespace(prefix = "hippostd")
    void setVersion(long version);
    
    @ServiceNamespace(prefix = "hippostd")
    long getVersion();
    
    @ServiceNamespace(prefix = "hippostd")
    void setModifiedDate(Calendar date);
    
    @ServiceNamespace(prefix = "hippostd")
    Calendar getModifiedDate();
    
}
