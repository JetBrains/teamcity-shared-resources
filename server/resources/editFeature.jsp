<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>

<tr class="noBorder">
  <th><label for="${keys.resourceKey}">Resource name:</label></th>
  <td>
    <props:textProperty name="${keys.resourceKey}" className="longField"  style="width: 100%;"/>
    <%--suppress CheckEmptyScriptTag --%>
    <span class="error" id="error_${keys.resourceKey}" />
    <span class="smallNote">Specify shared resource name</span>
  </td>
</tr>

<tr class="noBorder">
  <td colspan="2">
    <p>Please specify a shared resource that must be locked during build</p>
  </td>
</tr>