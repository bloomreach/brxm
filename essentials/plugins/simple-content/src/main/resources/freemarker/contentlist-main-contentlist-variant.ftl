<#include "../../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="item" type="org.example.beans.ContentDocument" -->
<#if pageable?? && pageable.items?has_content>
    <#list pageable.items as item>
        <@hst.link var="link" hippobean=item/>
        <div class="media has-edit-button">
            <@hst.cmseditlink hippobean=item/>
            <div class="media-body">
                <h4 class="media-heading">
                    <a href="${link}">${item.title?html}</a>
                    <#if item.publicationDate?? && item.publicationDate.time??>
                        <span class="label label-success pull-right">
                            <@fmt.formatDate value=item.publicationDate.time
                                             type="both" dateStyle="medium" timeStyle="short"/>
                        </span>
                    </#if>
                </h4>
                <p>${item.introduction?html}</p>
            </div>
        </div>
    </#list>
    <#if cparam.showPagination>
        <#include "../../include/pagination.ftl">
    </#if>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/generic-list.png'/>"> Click to edit list
</#if>
