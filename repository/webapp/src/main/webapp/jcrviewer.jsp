<%@page session="true" %>
<%@page import="javax.jcr.Node" %>
<%@page import="javax.jcr.NodeIterator" %>
<%@page import="javax.jcr.Property" %>
<%@page import="javax.jcr.PropertyIterator" %>
<%@page import="javax.jcr.Value" %>
<%@page import="javax.jcr.Repository" %>
<%@page import="javax.jcr.Session" %>
<%@page import="javax.jcr.RepositoryException" %>
<%@page import="javax.jcr.SimpleCredentials" %>
<%@page import="org.hippoecm.repository.servicing.ServicingNode" %>
<%@page import="org.apache.jackrabbit.rmi.client.ClientRepositoryFactory" %>
<%@page import="org.hippoecm.repository.HippoRepository" %>
<%@page import="org.hippoecm.repository.HippoRepositoryFactory" %>
<%@page import="org.hippoecm.repository.servicing.server.ServerServicingAdapterFactory" %>

<%!
   public Node dumpTree(JspWriter out, javax.jcr.Node parent, int level, String targetPath)
     throws javax.jcr.RepositoryException, java.io.IOException
   {
     Node rtNode = null;
     String prefix = "";
     if(targetPath.equals(parent.getPath()))
       rtNode = parent;
     for (int i=0; i<level; i++)
         prefix += "  ";
     String name = parent.getName();
     if(name.equals(""))
       name = "/";
     out.print( prefix + "<a href=\"jcrviewer.jsp?path=" + parent.getPath() + "\">" ); 
     out.print( ((ServicingNode) parent).getDisplayName() );
     if (parent.hasProperty("hippo:count")) {
       out.print(" [" + parent.getProperty("hippo:count").getLong() +"]");
     }
                 
     	out.println("</a>");
     if(targetPath.startsWith(parent.getPath())) {
       for(javax.jcr.NodeIterator iter = parent.getNodes(); iter.hasNext();) {
         Node node = iter.nextNode();
         if(!node.getPath().equals("/jcr:system")) {
              node = dumpTree(out, node, level + 1, targetPath);
              if(node != null)
                rtNode = node;
         }
       }
     }
     return rtNode;
   }
%>

<HTML><HEAD>
  <TITLE>Hippo CMS Home - Hippo</TITLE>
  <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
  <META HTTP-EQUIV="Expires" CONTENT="-1">
  <LINK REL="stylesheet" href="http://www.hippocms.org/styles/main-action.css?spaceKey=CMS" type="text/css" />
  <LINK REL="shortcut icon" href="http://www.hippocms.org/images/icons/favicon.ico">
  <link rel="icon" type="image/png" href="http://www.hippocms.org/images/icons/favicon.png">
  <script src="/decorators/effects.js"></script>
</HEAD><BODY>
  <div id="PageContent"><div id="topBar" class="topBar" style="height:2.2em"><div class="topBarDiv" style="float:left; margin-top:0.5em"><div id="logodiv" style="height=4em"><a href="http://www.hippocms.org/homepage.action"><img src="http://www.hippocms.org/download/userResources/logo" align="absmiddle" border="0"></a></div></div></div></div>

<H1>JCR Browsing</h1>

<DIV ALIGN="left"><PRE>

<%
  HippoRepository repositoryConnection = (HippoRepository) session.getAttribute("repository");
  javax.jcr.Session repositorySession = (javax.jcr.Session) session.getAttribute("session");
  //if(repositoryConnection == null || repositorySession == null) {
    repositoryConnection = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/jackrabbit.repository");
    repositorySession = repositoryConnection.login();
    session.setAttribute("repository", repositoryConnection);
    session.setAttribute("session", repositorySession);
  //}

  String username = repositorySession.getUserID();
  String location = repositorySession.getRepository().getDescriptor(Repository.REP_NAME_DESC);
  String path = (String) request.getParameter("path");
  if(path == null)
    path = "/";
  out.println("Logged in as " + username + " to a " + location + " repository.");
%>
</DIV>

<H2>Node tree</H2>

<DIV ALIGN="left"><PRE>
<%
  Node root = repositorySession.getRootNode();
  Node node = dumpTree(out, root, 0, path);
%>
</PRE></DIV>

<H2>Properties of selected node</H2>
<DIV ALIGN="CENTER>">
<PRE STYLE="bgcolor:white">

<%
  if(node != null) {
     for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
         Property prop = iter.nextProperty();
         out.print(prop.getPath() + " [name=" + prop.getName() + "] = ");
         if(prop.getDefinition().isMultiple()) {
           Value[] values = prop.getValues();
           out.print("[ ");
           for (int i = 0; i < values.length; i++) {
             out.print((i > 0 ? ", " : "") + values[i].getString());
           }
           out.println(" ]");
         } else {
           out.println(prop.getString());
         }
     }
  }
%>

</PRE>

<%
    repositorySession.save();
    repositorySession.logout();
%>

</DIV>
</BODY><HTML>
