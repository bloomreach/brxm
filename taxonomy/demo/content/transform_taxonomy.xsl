<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet xmlns:sv="http://www.jcp.org/jcr/sv/1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="sv:property[@sv:name='jcr:mixinTypes']">
        <!--nothing-->
    </xsl:template>
    
    <xsl:template match="sv:property[@sv:name='hippo:paths']">
        <!--nothing-->
    </xsl:template>
    
    <xsl:template match="sv:property[@sv:name='hippo:related___pathreference']">
        <!--nothing-->
    </xsl:template>
    
    <xsl:template match="sv:property[@sv:name='hippotaxonomy:description']">
        <!--nothing-->
    </xsl:template>
    
    <xsl:template match="*" priority="-1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="sv:property[@sv:name='jcr:uuid']">
        <sv:property sv:name="hippotaxonomy:key" sv:type="String">
            <sv:value>key_<xsl:value-of select="sv:value/text()"/></sv:value>
        </sv:property>
    </xsl:template> 
    
    <xsl:template match="@*|comment()">
        <xsl:copy/>
    </xsl:template> 
    
    
    
</xsl:stylesheet>