<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="version"></xsl:param>
<xsl:template match="/">
<xsl:output  method="html" doctype-public='-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd' encoding="UTF-8" indent="no"/>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<meta name="robots" content="index,follow"></meta>
	<meta name="revisit-after" content="7 days"></meta>
	<meta name="keywords" content="MaltParser, Dependency Parsing, Nivre, NLP, CoNLL, Treebank, Machine Learning, Data-driven"></meta>
	<meta name="description" content="MaltParser is a system for data-driven dependency parsing, which can be used to induce a parsing model from treebank data and to parse new data using an induced model."></meta>
	<title>MaltParser - Available options</title>
 <link rel="stylesheet" type="text/css" href="style.css" media="screen" />
 <link rel="stylesheet" type="text/css" href="print.css" media="print" />
 	<script type="text/javascript">
	 var _gaq = _gaq || [];
	 _gaq.push(['_setAccount', 'UA-22905106-2']);
	 _gaq.push(['_trackPageview']);
	 (function() {
		var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
		ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
		var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
	 })();
 	</script>
</head>
<body>
<h1>MaltParser</h1>
<div id="navtop">Modified: @today@</div>
@leftmenu@
<div id="bodycol">
	<div class="section"><h2>MaltParser <xsl:value-of select="$version"/> - Available options</h2></div>
	<div class="section">
	<table cellpadding="1" cellspacing="0">  
	<tr>
		<th style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Option</th>
		<th style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Flag</th>
		<th style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Type</th>
		<th style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Default</th>
		<th style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Usage</th>
		<th style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Description</th>
	</tr>
	<xsl:apply-templates/> 
	</table>
	<p id="footer">Copyright &#169; Johan Hall, Jens Nilsson and Joakim Nivre</p>
	</div>
</div>		
</body>
</html>
</xsl:template>
    
<xsl:template match="optiongroup">
<tr><th colspan="6" style="text-align:left;background-color:#EEEEEE;border:1px solid"><xsl:value-of select="@groupname" /></th></tr> 
<xsl:call-template name="options">
 <xsl:with-param name="option" select="./option"/>
</xsl:call-template>
</xsl:template>

<xsl:template name="options">
 	<xsl:param name="option"/>
   	<xsl:for-each select="$option">
  		<tr valign="top">
   		<td style="border-left:1px solid;border-bottom:1px solid;font-weight:bold">&#160;<xsl:value-of select="@name" /></td>
   		<td style="border-bottom:1px solid">&#160;-<xsl:value-of select="@flag" /></td>
   		<td style="border-bottom:1px solid">&#160;<xsl:value-of select="@type" /></td>
   		<td style="border-bottom:1px solid">&#160;<xsl:value-of select="@default" /></td>
   		<td style="border-bottom:1px solid">&#160;<xsl:value-of select="@usage" /></td>
   		<td style="border-right:1px solid;border-bottom:1px solid">&#160;<xsl:value-of select="shortdesc" />
   		<xsl:if test="@type='class' or @type='enum' or @type='stringenum'">
   		<table cellpadding="1" cellspacing="0" width="100%">
		<xsl:call-template name="legalvalues">
			<xsl:with-param name="legalvalue" select="./legalvalue"/>
		</xsl:call-template>
		</table>
		</xsl:if>
   		</td>
   		</tr> 
   	</xsl:for-each>
</xsl:template>
    
<xsl:template name="legalvalues">
 <xsl:param name="legalvalue"/>
 <xsl:for-each select="$legalvalue">
  <tr><td style="width:30%;border-top:1px dashed;font-style:italic;font-weight:bold">&#160;<xsl:value-of select="@name" /></td>
    <td style="width:70%;border-top:1px dashed"><xsl:value-of select="." /></td></tr>
 </xsl:for-each>
</xsl:template>
</xsl:stylesheet>