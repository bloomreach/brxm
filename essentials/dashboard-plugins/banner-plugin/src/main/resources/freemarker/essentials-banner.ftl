<#include "/WEB-INF/freemarker/include/imports.ftl">
<#-- @ftlvariable name="document" type="{{beansPackage}}.Banner" -->
<#if banner??>
<div class="row">
    <a href="<@hst.link hippobean=document.link />"><img src="<@hst.link hippobean=document.image />" alt="${document.title}"/></a>
</div>
</#if>
