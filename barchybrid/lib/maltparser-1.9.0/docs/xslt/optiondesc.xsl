<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="version"></xsl:param>
 <xsl:template match="/">
<xsl:output  method="html" doctype-public='-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd' encoding="UTF-8" indent="no"/>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
 <head>
 <meta name="robots" content="index,follow"></meta>
 <meta name="revisit-after" content="7 days"></meta>
 <meta name="keywords" content="MaltParser, Dependency Parsing, Nivre, "></meta>
 <meta name="description" content="Description all available options in MaltParser."></meta>
 <title>MaltParser - Option description</title>
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
  <div class="section">
  <h2>MaltParser <xsl:value-of select="$version"/> - Available options</h2>
  <p>All options are categorized into one of the following option groups:
  <xsl:for-each select="options/optiongroup">
  <xsl:choose>
   <xsl:when test="position() != last()">
    <b><xsl:value-of select="@groupname" /></b>, 
   </xsl:when>
   <xsl:otherwise>
    <b><xsl:value-of select="@groupname" /></b>.
   </xsl:otherwise>
  </xsl:choose>
  </xsl:for-each>
 Every option can have the following attributes:</p>
 <table class="bodyTable">
  <tr class="a"><th>Attribute</th><th>Description</th></tr>
  <tr class="b"><td align="left" style="font-weight:bold">name</td><td align="left">The name of the option</td></tr>
  <tr class="b"><td align="left" style="font-weight:bold">type</td><td align="left">
  There are following option types:
  <table>
   <tr><td width="20%">unary</td><td>The option has no value, this type is only used by the <b>help</b> option to indicate that help should be displayed.</td></tr>
   <tr><td width="20%">bool</td><td>Boolean option, can take either <b>true</b> or <b>false</b> value.</td></tr>
   <tr><td width="20%">integer</td><td>Integer option, can take an integer value.</td></tr>
   <tr><td width="20%">string</td><td>String option, can take a string value.</td></tr>
   <tr><td width="20%">enum</td><td>Enum option, can only take a predefined value.</td></tr>
   <tr><td width="20%">stringenum</td><td>StringEnum option, can either take a string value or a predefined value.</td></tr>
   <tr><td width="20%">class</td><td>Class option, can take a predefined value that corresponds to a class in the MaltParser distribution. </td></tr>
  </table>
  </td></tr>
  <tr class="b"><td align="left" style="font-weight:bold">flag</td><td align="left">A short version option indicator. </td></tr>
  <tr class="b"><td align="left" style="font-weight:bold">default</td><td align="left">If there is a default value it is specified by this attribute.</td></tr>
  <tr class="b"><td align="left" style="font-weight:bold">usage</td><td align="left">
   Indicates the usage of the option:
   <table>
	<tr><td width="20%">train</td><td>The option is only relevant during learning.</td></tr>
	<tr><td width="20%">process</td><td>The option is only relevant during processing (parsing)</td></tr>
	<tr><td width="20%">both</td><td>The option is relevant both during learning and processing (parsing)</td></tr>
	<tr><td width="20%">save</td><td>The option is saved during learning and cannot be overridden during processing (parsing)</td></tr>
   </table>	
   </td></tr>
  </table>
  <p>All the option groups and options are described in detail below. An option begins with the following format if the attribute is applicable:
  <h4>
  <table>
  	<tr><td style="width:30%;font-weight:bold">name</td>
  	<td style="width:5%">-flag</td>
  	<td style="width:15%">type</td>
  	<td style="width:25%">default value</td>
  	<td style="width:25%">usage</td></tr>
  </table>
  </h4>
  </p>
<xsl:apply-templates/>
<p id="footer">Copyright &#169; Johan Hall, Jens Nilsson and Joakim Nivre</p>
 </div>
</div>		
</body>
</html>
</xsl:template>
    
    <xsl:template match="optiongroup">
    	<a>
    		<xsl:attribute name="name">
  				<xsl:value-of select="@groupname" />
  			</xsl:attribute> 
    	</a>
    	<h3><xsl:value-of select="@groupname" /></h3> 
    	<p><xsl:value-of select="desc" disable-output-escaping="yes" /></p>
		<xsl:call-template name="options">
			<xsl:with-param name="option" select="./option"/>
		</xsl:call-template>
    </xsl:template>

    <xsl:template name="options">
    	<xsl:param name="option"/>
    	<xsl:for-each select="$option">
    	    <a>
    			<xsl:attribute name="name">
  					<xsl:value-of select="../@groupname" />-<xsl:value-of select="@name" />
  				</xsl:attribute> 
    		</a>
    		<h4>
    		<table>
    		<tr><td style="width:30%;font-weight:bold"><xsl:value-of select="@name" /></td>
    		<td style="width:5%">&#160;-<xsl:value-of select="@flag" /></td>
    		<td style="width:15%">&#160;<xsl:value-of select="@type" /></td>
    		<td style="width:25%">&#160;<xsl:value-of select="@default" /></td>
    		<td style="width:25%">&#160;<xsl:value-of select="@usage" /></td></tr></table></h4>
    		<p><xsl:value-of select="desc" disable-output-escaping="yes" /></p>
    		<xsl:if test="@type='class' or @type='enum' or @type='stringenum'">
    		<table cellpadding="1" cellspacing="0" width="50%">
    		<!-- <tr><th colspan="2" align="left" style="text-align:left;background-color:darkblue;color:white;border:1px black solid">Legal values</th></tr> -->
			<xsl:call-template name="legalvalues">
				<xsl:with-param name="legalvalue" select="./legalvalue"/>
			</xsl:call-template>
			</table>
			</xsl:if>
    	</xsl:for-each>
    </xsl:template>
    
     <xsl:template name="legalvalues">
     	<xsl:param name="legalvalue"/>
     	<xsl:for-each select="$legalvalue">
     		<!--  <tr><td style="width:30%;border-top:1px dashed;font-style:italic;font-weight:bold">&#160;<xsl:value-of select="@name" /></td>
     		<td style="width:70%;border-top:1px dashed"><xsl:value-of select="." /></td></tr>-->
     		<tr><td align="left" style="width:15%;font-weight:bold">&#160;<xsl:value-of select="@name" /></td>
     		<td align="left" style="width:85%"><xsl:value-of select="." /></td></tr>
     	</xsl:for-each>
     </xsl:template>
</xsl:stylesheet> 
