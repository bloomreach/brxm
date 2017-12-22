<#include "../include/imports.ftl">

<#-- @ftlvariable name="item" type="{{beansPackage}}.EventsDocument" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
<div>
  <#list pageable.items as item>
    <@hst.link var="link" hippobean=item />
    <article class="has-edit-button">
      <@hst.manageContent templateQuery="new-events-document" document=item defaultPath="events"/>
      <h3><a href="${link}">${item.title?html}</a></h3>
      <#if item.date?? && item.date.time??>
        <p><@fmt.formatDate value=item.date.time type="both" dateStyle="medium" timeStyle="short"/></p>
      </#if>
      <#if item.enddate?? && item.endDate.time??>
        <p><@fmt.formatDate value=item.endDate.time type="both" dateStyle="medium" timeStyle="short"/></p>
      </#if>
      <p>${item.location?html}</p>
      <p>${item.introduction?html}</p>
    </article>
  </#list>
  <#if cparam.showPagination>
    <#include "../include/pagination.ftl">
  </#if>
</div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <@hst.manageContent templateQuery="new-events-document" defaultPath="events"/>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/events-list.png'/>"> Click to edit Event List
</div>
</#if>

