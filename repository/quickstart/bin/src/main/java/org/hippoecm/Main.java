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
package org.hippoecm;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

public class Main extends Server {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static private void delete(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++)
                    delete(files[i]);
            }
            path.delete();
        }
    }

    static private void clear() {
        String[] files = new String[] {".lock", "repository", "version", "workspaces"};
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            delete(file);
        }
    }

    public static void main(String[] args) {
        String warFile = "../war/target/hippo-ecm-quickstart-war-2.13.00-SNAPSHOT.war";
        if (args.length > 0) {
            warFile = args[0];
        }
        clear();
        Main main = new Main();
        try {
            Connector connector = new SelectChannelConnector();
            connector.setPort(8080);
            main.addConnector(connector);
            WebAppContext wac = new WebAppContext();
            wac.setContextPath("/cms");
            wac.setWar(warFile);
            main.setHandler(wac);
            main.setStopAtShutdown(true);
            main.start();
        } catch (SAXException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
