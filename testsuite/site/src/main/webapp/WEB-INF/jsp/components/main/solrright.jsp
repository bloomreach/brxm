<%--
  Copyright 2009 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. 
--%>
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="result" type="org.hippoecm.hst.solr.content.beans.query.HippoQueryResult"--%>

<hst:element name="link" var="jquerytheme">
  <hst:attribute name="id" value="jquery-theme"/>
  <hst:attribute name="media" value="all"/>
  <hst:attribute name="rel" value="stylesheet" />
  <hst:attribute name="type" value="text/css"/>
  <hst:attribute name="href" value="http://code.jquery.com/ui/1.8.18/themes/base/jquery-ui.css"/>
</hst:element>
<hst:headContribution keyHint="jquery-theme" element="${jquerytheme}"/>

<hst:element name="script" var="jquery">
  <hst:attribute name="type" value="text/javascript"/>
  <hst:attribute name="src" value="https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"/>
</hst:element>
<hst:headContribution keyHint="jquery" element="${jquery}" category="jsExternal"/>

<hst:element name="script" var="jqueryUI">
  <hst:attribute name="type" value="text/javascript"/>
  <hst:attribute name="src" value="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.18/jquery-ui.min.js"/>
</hst:element>
<hst:headContribution keyHint="jqueryUI" element="${jqueryUI}" category="jsExternal"/>

<hst:headContribution category="jsInline">
  <script type="text/javascript">
    $(function() {
      $( "#fromdate" ).datepicker();
      $( "#todate" ).datepicker();
    });

    function <hst:namespace/>returnedSuggestions(req) {
      var text = req.responseText;
      if (text) {
        simpleio_objectbyid("<hst:namespace/>suggestions").innerHTML = text;
      }
    }

    function <hst:namespace/>loadSuggstions(value) {
      if( simpleio_objectbyid('<hst:namespace/>suggest').checked) {
        if (value.length > 0) {
          simpleio_sendrequest("<hst:resourceURL/>", <hst:namespace/>returnedSuggestions, "suggestquery="+value);
        }
      }
    }

    function <hst:namespace/>autocomplete(e) {
      var TABKEY = 9;
      if(e.keyCode == TABKEY) {
        if(e.preventDefault) {
          e.preventDefault();
        }
        if(simpleio_objectbyid("<hst:namespace/>collated")) {
          simpleio_objectbyid('<hst:namespace/>query').value = simpleio_objectbyid("<hst:namespace/>collated").innerHTML;
        }
        return false;
      }
    }
  </script>
</hst:headContribution>

<hst:link var="simpleiopath" path="/javascript/simple-io.js"/>
<hst:element name="script" var="simpleio">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="${simpleiopath}" />
</hst:element>
<hst:headContribution keyHint="simpleio" element="${simpleio}" />


<div class="yui-b">
  <hst:link var="searchURL" path="/solrsearch" />
  <form action="${searchURL}" method="get">
    <div>
      <input type="checkbox" name="spellcheck" <c:if test="${spellcheck}">checked</c:if> /> Spellcheck<br />
      <input type="checkbox" name="highlight" <c:if test="${highlight}">checked</c:if> /> Highlights in results<br />
      <input type="checkbox" name="score" <c:if test="${score}">checked</c:if> /> Show score<br />
      <br/><br/>

      <input id="<hst:namespace/>suggest" type="checkbox" name="suggest" <c:if test="${suggest}">checked</c:if> /> Enable auto completion<br/>
      <b>Query:</b> <input type="text"
                           id="<hst:namespace/>query"
                           name="query" value="${query}"
                           onkeyup="<hst:namespace/>loadSuggstions(this.value)"
                           onkeydown="<hst:namespace/>autocomplete(event)"
                           autocomplete="off"
                          />
      <div id="<hst:namespace/>suggestions" style="border:dotted;width:75%; background-color:white">

      </div>
      <br/>
      <input type="radio" name="operator" id="and_ed" <c:if test="${operator eq 'and_ed'}" >checked</c:if> value="and_ed"/> <label for="and_ed">AND-ed operator</label><br />
      <input type="radio" name="operator" id="or_ed"  <c:if test="${operator eq  'or_ed'}" >checked</c:if> value="or_ed"/> <label for="or_ed">OR-ed operator</label><br />

      <br/>
      <input type="radio" name="searchfield" <c:if test="${searchfield eq 'all'}" >checked</c:if> value="all"/> Search in all fields<br />
      <input type="radio" name="searchfield" <c:if test="${searchfield eq 'title'}" >checked</c:if> value="title"/> Search in title<br />
      <input type="radio" name="searchfield" <c:if test="${searchfield eq 'summary'}" >checked</c:if> value="summary"/> Search in summary<br />

      <br/> <br/>
      <b>Search in</b> <br/>
      <input type="radio" id="all" name="searchin" <c:if test="${searchin eq 'all'}">checked</c:if> value="all"> <label for="all">All channels</label> <br/>
      <input type="radio" id="current" name="searchin" <c:if test="${searchin eq 'current'}">checked</c:if> value="current"> <label for="current">Current channel</label> <br/>
      <input type="radio" id="external" name="searchin" <c:if test="${searchin eq 'external'}">checked</c:if> value="external"> <label for="external">External Http(s) sources</label> <br/>
      <br/> <br/>
      <b>Search in types:</b>
      <br/>

      <input type="checkbox" name="type" value="all" <c:if test="${types['all'] eq 'all'}">checked</c:if> /> All documents<br />
      <input type="checkbox" name="type" value="TextBean" <c:if test="${types['TextBean'] eq 'TextBean'}">checked</c:if> /> Text documents<br />
      <input type="checkbox" name="type" value="NewsBean" <c:if test="${types['NewsBean'] eq 'NewsBean'}">checked</c:if> /> News documents<br />
      <input type="checkbox" name="type" value="ProductBean" <c:if test="${types['ProductBean'] eq 'ProductBean'}">checked</c:if> /> Product documents<br />
      <input type="checkbox" name="type" value="WikiBean" <c:if test="${types['WikiBean'] eq 'WikiBean'}">checked</c:if> /> Wiki documents<br />
      <input type="checkbox" name="type" value="HippoBean" <c:if test="${types['HippoBean'] eq 'HippoBean'}">checked</c:if> /> ONLY JCR documents<br />
      <input type="checkbox" name="type" value="GoGreenProductBean" <c:if test="${types['GoGreenProductBean'] eq 'GoGreenProductBean'}">checked</c:if> /> GoGreenProductBean document<br />

      <br/>
      <input type="checkbox" name="includeSubtypes" <c:if test="${includeSubtypes}">checked</c:if> /> <b>Include subtypes:</b>
      <br />

      <br/><br/>
      <hst:link var="cal_icon" path="/images/icons/calendar_icon.gif"/>
      <b>From date:</b><br/>
      <label for="fromdate"><img src="${cal_icon}"/></label>
      <input type="text" name="fromdate" id="fromdate" value="${fromdate}" />
      <br/>
      <b>To date:</b><br/>
      <label for="todate"><img src="${cal_icon}"/></label>
      <input type="text" name="todate" id="todate" value="${todate}" />

      <br/><br/>
      <b>Sort by:</b><br />
      <input type="radio" name="sort" id="relevance" value="score" <c:if test="${sort eq 'score'}">checked</c:if> /> <label for="relevance">relevance</label><br />
      <input type="radio" name="sort" id="date" value="date" <c:if test="${sort eq 'date'}">checked</c:if> /> <label for="date">document date</label><br />
      <input type="radio" name="sort" id="hippo_timestamp" value="hippo_timestamp" <c:if test="${sort eq 'hippo_timestamp'}">checked</c:if> /> <label for="date">indexing time</label><br />
      <input type="radio" name="sort" id="random" value="random" <c:if test="${sort eq 'random'}">checked</c:if> /> <label for="random">random</label><br />
      <br />
      <b>Order:</b><br />
      <input type="radio" name="order" id="desc" value="desc" <c:if test="${order eq 'desc'}">checked</c:if> /> <label for="desc">Descending</label><br />
      <input type="radio" name="order" id="asc" value="asc" <c:if test="${order eq 'asc'}">checked</c:if> /> <label for="asc">Ascending</label><br />

      <br/><br/>
      <input type="submit" value="Search" />
    </div>

  </form>
</div>
