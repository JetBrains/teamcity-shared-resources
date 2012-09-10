<%--@elvariable id="buildId" type="java.lang.String"--%>
<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>


<tr class="noBorder">
  <th><label for="${keys.resourceKey}">Resource name:</label></th>
  <td>
    <props:textProperty name="${keys.resourceKey}" className="longField"/>
    <span class="error" id="error_${keys.resourceKey}"></span>
    <span class="smallNote">Specify shared resource name</span>
  </td>
</tr>

<props:hiddenProperty name="${keys.buildIdKey}" value="${buildId}"/>

<tr class="noBorder">
  <td colspan="2">
    <p>Please specify a shared resource that must be locked during build</p>
  </td>
</tr>