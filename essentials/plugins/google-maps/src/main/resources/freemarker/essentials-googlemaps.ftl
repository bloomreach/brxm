<#include "../include/imports.ftl">

<#-- @ftlvariable name="componentId" type="java.lang.String" -->
<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsGoogleMapsComponentInfo" -->

<div id="map-canvas-${componentId}" style="width: ${cparam.width}px; height: ${cparam.height}px;"></div>

<@hst.headContribution category="htmlBodyEnd">
    <script type="text/javascript">
        if (!window.HEGM) {
              window.HEGM = [];
        }
        window.HEGM.push(function() {
              initGoogleMap('map-canvas-${componentId}', '${cparam.address?html}', ${cparam.longitude}, ${cparam.latitude}, ${cparam.zoomFactor}, '${cparam.mapType}');
        });
    </script>
</@hst.headContribution>

<@hst.headContribution keyHint="essentials-google-maps" category="htmlBodyEnd">
    <@hst.webfile path="/js/essentials-google-maps.js" var="essentialsGoogleMapsJs" />
    <script type="text/javascript" src="${essentialsGoogleMapsJs}"></script>
</@hst.headContribution>

<@hst.headContribution keyHint="google-maps-api" category="htmlBodyEnd">
    <#if cparam.apiKey?has_content>
        <#assign mapsUrl = "https://maps.googleapis.com/maps/api/js?key=${cparam.apiKey?html}&callback=initGoogleMaps"/>
    <#else>
        <#assign mapsUrl = "https://maps.googleapis.com/maps/api/js?callback=initGoogleMaps"/>
    </#if>
    <script type="text/javascript" src="${mapsUrl}" async="async" defer="defer"></script>
</@hst.headContribution>

<#if editMode>
    <script type="text/javascript">
        if (window.initGoogleMap) {
            initGoogleMap('map-canvas-${componentId}', '${cparam.address?html}', ${cparam.longitude}, ${cparam.latitude}, ${cparam.zoomFactor}, '${cparam.mapType}');
        }
    </script>
</#if>
