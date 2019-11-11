<#include "../include/imports.ftl">

<#-- @ftlvariable name="componentId" type="java.lang.String" -->
<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsOpenStreetMapComponentInfo" -->
<div>
  <#assign style>
    style="<#if cparam.width gt 0>width: ${cparam.width}px;</#if><#if cparam.height gt 0>height: ${cparam.height}px;</#if>"
  </#assign>

  <div id="mapid-${componentId}" class="openstreetmap" ${style}></div>

 <@hst.headContribution category="htmlBodyEnd">
    <script type="text/javascript">
      (function(win) {
        var he, osm;
        if (!win.HippoEssentials) {
          win.HippoEssentials = {};
        }
        he = win.HippoEssentials;

        if (!he.OpenStreetMap) {
          he.OpenStreetMap = {
            queue: []
          };
        }
        osm = he.OpenStreetMap;

        osm.showMap('mapid-${componentId}', 
          '<#if cparam.latitude gt 0>${cparam.latitude}</#if>',
          '<#if cparam.longitude gt 0>${cparam.longitude}</#if>',
          '${cparam.address?html}',
          '${cparam.zoomFactor}',
          '${cparam.mapType?html}', 
          '<#if cparam.mapOverlay?has_content && cparam.mapOverlay != "none">${cparam.mapOverlay?html}</#if>',
          '${cparam.showMarker}',
          '${cparam.markerCustomText?html}');
      })(window);
    </script>
  </@hst.headContribution>

  <@hst.headContribution keyHint="openStreetMapCSS">
    <link rel="stylesheet" href="<@hst.webfile path="css/leaflet.css"/>"/>
  </@hst.headContribution>

  <@hst.headContribution keyHint="openStreetMapStyles">
    <link rel="stylesheet" href="<@hst.webfile path="css/essentials-openstreetmap.css"/>"/>
  </@hst.headContribution>
  
  <@hst.headContribution keyHint="openStreetMapLibrary">
    <script type="text/javascript" src="<@hst.webfile path="js/leaflet.js"/>"></script>
  </@hst.headContribution>

  <@hst.headContribution keyHint="jqueryLibrary">
    <script type="text/javascript" src="<@hst.webfile path="js/jquery-3.4.1.min.js"/>"></script>
  </@hst.headContribution>

  <@hst.headContribution keyHint="openStreetMapScript">
    <script type="text/javascript" src="<@hst.webfile path="js/essentials-openstreetmap.js"/>"></script>
  </@hst.headContribution>

  <#if editMode>
    <script type="text/javascript">
      if (window.HippoEssentials && window.HippoEssentials.OpenStreetMap) {
        window.HippoEssentials.OpenStreetMap.showMap('mapid-${componentId}',
          '<#if cparam.latitude gt 0>${cparam.latitude}</#if>',
          '<#if cparam.longitude gt 0>${cparam.longitude}</#if>',
          '${cparam.address?html}',
          '${cparam.zoomFactor}',
          '${cparam.mapType?html}',
          '<#if cparam.mapOverlay?has_content && cparam.mapOverlay != "none">${cparam.mapOverlay?html}</#if>',
          '${cparam.showMarker}',
          '${cparam.markerCustomText?html}');
      }
    </script>
  </#if>
  
</div>