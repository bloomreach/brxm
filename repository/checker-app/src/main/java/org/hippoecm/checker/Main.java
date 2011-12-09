package org.hippoecm.checker;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.LocalHippoRepository;

public class Main {
    public static void main(String[] args) {
        try {
            if(args.length == 1 && "-h".equals(args[0]) || "--help".equals(args[0]) || "help".equals(args[0])) {
                usage();
            } else if(args.length >= 1 && ("--sample".equals(args[0]) || "sample".equals(args[0]))) {
                sample(args);
            } else if(args.length == 0) {
                LocalHippoRepository.check(null, true);
            } else {
                LocalHippoRepository.check(null, args);
            }
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    
    private static void usage() {
        try {
            InputStream in = Main.class.getResource("usage.txt").openStream();
            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) >= 0) {
              System.out.write(buffer, 0, len);
            }
        } catch(IOException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void sample(String[] args) {
        if (args.length != 5) {
            System.err.println("Wrong number of arguments, use --help for help");
            return;
        }
        String location = args[1];
        String database = args[2];
        String username = args[3];
        String password = args[4];
        try {
            InputStream in = Main.class.getResource("repository-sample.xml").openStream();
            byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();
            int len;
            while ((len = in.read(buffer)) >= 0) {
                StringBuffer s = null;
                for (int i = 0; i < len; i++)
                    sb.append((char)buffer[i]);
            }
            String repositoryXml = sb.toString();
            repositoryXml = repositoryXml.replaceAll("HOSTNAME", location);
            repositoryXml = repositoryXml.replaceAll("DATABASE", database);
            repositoryXml = repositoryXml.replaceAll("USERNAME", username);
            repositoryXml = repositoryXml.replaceAll("PASSWORD", password);
            System.out.print(repositoryXml);
        } catch (IOException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
