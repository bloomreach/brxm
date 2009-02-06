package org.hippoecm.hst.service;

import java.util.Calendar;

public interface IBlog {

    void setTitle(String title);

    String getTitle();
    
    void setContent(String content);
    
    String getContent();
    
    void setWritable(boolean writable);
    
    boolean isWritable();
    
    void setVersion(long version);
    
    long getVersion();
    
    void setModifiedDate(Calendar date);
    
    Calendar getModifiedDate();
    
}
