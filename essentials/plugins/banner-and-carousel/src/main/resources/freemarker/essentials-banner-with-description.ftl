<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="org.example.beans.Banner" -->
<#if document??>
    <div>
        <figure>
            <a href="<@hst.link hippobean=document.link />">
                <img src="<@hst.link hippobean=document.image />" alt="${document.title?html}"/>
            </a>
            <figcaption>
                <#if document.title??>
                    <h4>${document.title?html}</h4>
                </#if>
                <@hst.html hippohtml=document.content/>
            </figcaption>
        </figure>
    </div>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
</#if>
