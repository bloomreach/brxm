package org.hippoecm.hst.service;

import java.util.Calendar;

@ServiceNamespace(prefix = "hippostd")
public interface IDocument {
    
    void setWritable(boolean writable);
    
    boolean isWritable();
    
    void setVersion(long version);
    
    long getVersion();
    
    void setModifiedDate(Calendar date);
    
    Calendar getModifiedDate();

}
