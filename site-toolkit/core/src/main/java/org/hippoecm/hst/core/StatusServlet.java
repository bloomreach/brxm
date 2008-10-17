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
package org.hippoecm.hst.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.caching.Cache;
import org.hippoecm.hst.caching.CacheManager;

public class StatusServlet extends HttpServlet {
   
    private static final long serialVersionUID = 1L;

    public StatusServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
      
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        
        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        writer.println("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        writer.println("<head><title>Hippo Site Tool Kit Status</title>");
        writer.println("<style type=\"text/css\">");
        writer.println(" table.params {font-size:small}");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("  <h1>Hippo Site Tool Kit Status</h1>");
        writer.println("  <h2>Memory Settings</h2>");

        Runtime runtime = Runtime.getRuntime();
        writer.println("    <table style=\"params\" >");
        writer.println("    <tr><td><b>Memory limit<b></td><td align=\"right\"></td><td align=\"right\">&nbsp;&nbsp;" + ((runtime.maxMemory()+24288)/1048576) + "&nbsp;Mbyte </td></tr>");
        writer.println("    <tr><td><b>Memory in use<b></td><td align=\"right\"> </td><td align=\"right\">&nbsp;&nbsp;" + ((runtime.totalMemory()+24288)/1048576) + "&nbsp;Mbyte </td></tr>");
        writer.println("    <tr><td><b>Memory available<b></td><td align=\"right\"> </td><td align=\"right\">&nbsp;&nbsp;" + ((runtime.freeMemory()+24288)/1048576) + "&nbsp;Mbyte</td></tr>");
        writer.println("    <tr><td><b>Processors<b></td><td align=\"right\"> " + runtime.availableProcessors() + "</td><td>&nbsp;</td></tr>");
        writer.println("    </table>");
        
        writer.println("  <h2>Cache Manager</h2>");
        writer.println("    <table cellpadding=\"5px;\" style=\"background-color:grey\" >");
        
        tdStart(writer, "Options", "background-color:white");
        
        String[] optionsAll =  {"Status","Clear All", "Disable All", "Enable All"};
        String[] actionsAll = {"?", "?clearall=true", "?disableall=true", "?enableall=true"} ;
        
        tdEnd(writer, optionsAll, actionsAll,"background-color:white");
        writer.println("    </table>");
        
        writer.println("  <h3>Caches</h3>");
        writer.println("    <table style=\"background-color:grey\" >");
        int i = 0;

        String showKeys = req.getParameter("showkeys");
        String clearCache = req.getParameter("clearcache");
        String changeActive = req.getParameter("changeactive");
        boolean clearAll  = Boolean.parseBoolean(req.getParameter("clearall"));
        boolean disableAll = Boolean.parseBoolean(req.getParameter("disableall"));
        boolean enableAll = Boolean.parseBoolean(req.getParameter("enableall"));
        
        if(disableAll) {
            CacheManager.setNewCacheIsEnabled(false);
        }
        if(enableAll) {
            CacheManager.setNewCacheIsEnabled(true);
        }
        
        for(Iterator<Entry<String, Cache>> it = CacheManager.getCaches().entrySet().iterator(); it.hasNext(); ){
            i = (++i % 2);
            Entry<String, Cache> entry = it.next();
            tdStart(writer, "Cache name", getColor(i,entry.getValue().isActive())); 
            tdEnd(writer, entry.getKey(), null);
            
            if(showKeys!=null && showKeys.equals(entry.getKey())) {
        
            }
            if(clearAll || (clearCache!=null && clearCache.equals(entry.getKey()))) {
                entry.getValue().clear();
            }
            if(changeActive!=null && changeActive.equals(entry.getKey())) {
                entry.getValue().setActive(entry.getValue().isActive());
            }
            if(disableAll) {
                entry.getValue().setActive(false);
            }
            if(enableAll) {
                entry.getValue().setActive(true);
            }
            
            tdStart(writer, "Options", getColor(i,entry.getValue().isActive())); 
            String action = "Enable";
            if(entry.getValue().isActive()) {
                action = "Disable";
            } 
            String[] options =  {"Show Keys", "Clear", action};
            String[] actions = {"?showkeys="+entry.getKey(), "?clearcache="+entry.getKey(), "?changeactive="+entry.getKey()} ;
            
            tdEnd(writer,options, actions, getColor(i,entry.getValue().isActive()));
            
            for(Iterator <Entry<String,String>> stats = entry.getValue().getStatistics().entrySet().iterator(); stats.hasNext() ; ){
                Entry<String,String> stat = stats.next();
                tdStart(writer,stat.getKey(), getColor(i,entry.getValue().isActive()));
                tdEnd(writer, stat.getValue());
            }
            tdStart(writer, "&nbsp;", getColor(i,entry.getValue().isActive())); 
            tdEnd(writer, "");
        }
        writer.println("</table>");
        writer.println("</body>");
        writer.println("</head>");
        writer.println("</html>");
    }

    
    private String getColor(int i, boolean enable) {
        String color = "background-color:lightgrey;";
        if(i==1) {
            color = "background-color:white;";
        }
        if(!enable) {
            color += "color:grey;";
        }
        return color;
    }

    private void tdStart(PrintWriter writer, String name, String style) {
        writer.println ( "<tr><td valign=\"top\" style=\""+style+"\" ><b>"+name+"</b></td><td style=\""+style+"\">" );
    }
    
    private void tdEnd(PrintWriter writer, String value){
        tdEnd(writer, value, null);
    }
    
    private void tdEnd(PrintWriter writer, String value, String link) {
        if(link!=null) {
            writer.println ("<a href=\""+link+"\"> "+ value + "</a></td></tr>");   
        }else {
            writer.println (value + "</td></tr>");
        }
    }

    private void tdEnd(PrintWriter writer, String[] options, String[] actions, String style) {
        if(options.length != actions.length) {return;}
        for(int i = 0; i < actions.length ; i++) {
            writer.println ("<a style=\""+style+"\" href=\""+actions[i]+"\"> "+ options[i] + "</a><br/>" );
        }
        writer.println ("</td></tr>");
    }


    @Override
    public void destroy() {
        super.destroy();
    }
}
