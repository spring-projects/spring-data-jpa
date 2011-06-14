<?xml version='1.0'?>
<!-- 
    Simple highlighter for HTML output. Follows the Eclipse color scheme.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xslthl="http://xslthl.sf.net"
                exclude-result-prefixes="xslthl"
                version='1.0'>

	<xsl:template match='xslthl:keyword'>
	  <span class="hl-keyword"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:comment'>
	  <span class="hl-comment"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:oneline-comment'>
	  <span class="hl-comment"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:multiline-comment'>
	  <span class="hl-multiline-comment"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:tag'>
	  <span class="hl-tag"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:attribute'>
	  <span class="hl-attribute"><xsl:value-of select='.'/></span>
	</xsl:template>

	<xsl:template match='xslthl:value'>
	  <span class="hl-value"><xsl:value-of select='.'/></span>
	</xsl:template>
	
	<xsl:template match='xslthl:string'>
	  <span class="hl-string"><xsl:value-of select='.'/></span>
	</xsl:template>

</xsl:stylesheet>