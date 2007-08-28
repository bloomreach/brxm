package org.hippoecm.repository.testutils.history;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.Task;

//Copy junit-frames.xsl from the plugin to the target directory of
//the current project so that the junit-report ant task can use it.
public class PrepareReport extends Task {

    public void execute() {
        InputStream from = getClass().getResourceAsStream("/junit-frames.xsl");
        File toFile = new File("target/junit-frames.xsl");
        
        OutputStream to = null;
        try {
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
            
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

}
