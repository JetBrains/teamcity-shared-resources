<%--@elvariable id="buildId" type="java.lang.String"--%>
<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr class="noBorder">
  <th><label for="${keys.resourceKey}">Resource name:</label></th>
  <td>
    <props:multilineProperty name="${keys.resourceKey}" linkTitle="Enter shared resource name(s)" cols="49" rows="3" value="${propertiesBean.properties['resource-name']}" expanded="${true}"/>
    <span class="error" id="error_${keys.resourceKey}"></span>
    <span class="smallNote">Specify shared resource name(s)</span>
  </td>
</tr>

<tr class="noBorder">
  <td colspan="2">
    <p>Please specify shared resource(s) that must be locked during build</p>
  </td>
</tr>