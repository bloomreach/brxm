<#include "../include/imports.ftl">
<form id="search-form" action="<@hst.link path="/product-search" />">
    <div class="typeahead__container">
        <div class="typeahead__field">
            <span class="typeahead__query"><input class="js-typeahead" id="query" name="query" type="search"
                                                  value="" autocomplete="off"></span>
            <span class="typeahead__button"><button type="submit"><span class="typeahead__search-icon"></span></button></span>
        </div>
    </div>
</form>

<@hst.headContribution category="htmlHead">
<link rel="stylesheet" href="<@hst.webfile  path="/css/jquery.typeahead.min.css"/>" type="text/css"/>
</@hst.headContribution>
<#--
<#if autoSuggest>
-->
<@hst.headContribution category="htmlBodyEnd">
  <script type="text/javascript" src="<@hst.webfile path="js/jquery-2.1.0.min.js"/>"></script>
</@hst.headContribution>
<@hst.headContribution category="htmlBodyEnd">
  <script type="text/javascript" src="<@hst.webfile path="js/jquery.typeahead.min.js"/>"></script>
</@hst.headContribution>
<@hst.headContribution category="htmlBodyEnd">
  <script type="application/javascript">

      function cleanUrl(url) {
          if (url.indexOf('&amp;query=') !== -1) {
              url = url.split('&amp;query=');
              return url[0];
          }
          return url;

      }

      var url = cleanUrl('<@hst.resourceURL resourceId='autoSuggest'/>');
      $.typeahead({
          input: ".js-typeahead",
          order: "asc",
          /*  cache:true,
            ttl:300,*/
          dynamic: true,
          minLength: 1,
          filter: true,
          source: {
              groupName: {
                  ajax: function (query) {
                      return {
                          type: "GET",
                          url: url + "&amp;query=",
                          path: "suggestions",
                          data: {
                              q: ""
                          },
                          callback: {
                              done: function (data) {
                                  if (data) {
                                      var suggestions = data;
                                      var val = [];
                                      if (suggestions) {
                                          for (var i = 0; i &lt; suggestions.length; i++) {
                                              var obj = suggestions[i];
                                              console.log(obj);
                                              val.push(obj.q);
                                          }
                                      }
                                      return {"suggestions": val};
                                  }
                                  return {"suggestions": []};
                              }
                          }
                      };
                  }
              }
          },
          callback: {
              onClickBefore: function () {
                  $("search-form").submit();
              },
              onSubmit: function () {
                  $("search-form").submit();
              }
          }
      });

  </script>
</@hst.headContribution>

<#--</#if>-->
