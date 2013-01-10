<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>


<div class="yui-u">
  
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="touch" value="true"/>
     <input type="submit" value="Touch HST config to trigger reload" /> 
  </form>
  
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="1"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 1 New Site" /> 
  </form>
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="5"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 5 New Sites" /> 
  </form>
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="10"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 10 New Sites" /> 
  </form>
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="25"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 25 New Sites" /> 
  </form>
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="100"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 100 New Sites" /> 
  </form>
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="1000"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 1000 New Sites" /> 
  </form>

  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="1"/>
     <input type="hidden" name="copycomponents" value="false"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 1 New Site - Sitemap, Sitemenu only" /> 
  </form>
  
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="10"/>
     <input type="hidden" name="copycomponents" value="false"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 10 New Sites - Sitemap, Sitemenu only" /> 
  </form>

  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="100"/>
     <input type="hidden" name="copycomponents" value="false"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 100 New Sites - Sitemap, Sitemenu only" /> 
  </form>
  <form method="post" action="<hst:actionURL/>">
     <input type="hidden" name="number" value="1000"/>
     <input type="hidden" name="copycomponents" value="false"/>
     <input type="hidden" name="type" value="addnewsites"/>
     <input type="submit" value="add 1000 New Sites - Sitemap, Sitemenu only" /> 
  </form>
   
  <br/>
  <br/>
  
  
  <h2>Sites overview</h2>
  <ol>
  <c:forEach var="mount" items="${mounts}" varStatus="counter">
     <li>${counter.index + 1} <b>${mount.name}</b>  : ${mount.virtualHost.hostName}</li>    
  </c:forEach>
  </ol>  
  
</div>


  
  
  
