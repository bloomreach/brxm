package org.hippoecm.frontend.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TraceMonitor {

    static final Logger log = LoggerFactory.getLogger(TraceMonitor.class);

    private static final Map<String, String> initializedAndNotDetached = new HashMap<String, String>();

    protected static final void track(Object object) {
        if (log.isDebugEnabled() && object instanceof Item) {
            initializedAndNotDetached.put(object.toString(), getCallee());
        }
    }

    protected static final void release(Object object) {
        if (log.isDebugEnabled() && object instanceof Item) {
            initializedAndNotDetached.remove(object.toString());
        }
    }

    protected static final void trace(Object object) {
        if (log.isDebugEnabled() && object instanceof Item && initializedAndNotDetached.containsKey(object.toString())) {
            String stackTrace = initializedAndNotDetached.get(object.toString());
            log.debug(stackTrace);
        }
    }

    protected static final String getCallee() {
        Exception exception = new RuntimeException("Determine CallStackTrace");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        exception.printStackTrace(pw);
        pw.flush();

        return os.toString();
    }

}
