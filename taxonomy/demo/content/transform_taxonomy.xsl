<?xml version="1.0" encoding="UTF-8"?>
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