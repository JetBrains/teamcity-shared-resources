<%@ include file="/include-internal.jsp" %>


<c:url var="url" value='project.html?projectId=${project.projectId}&tab=sharedResources'/>
<jsp:useBean id="bean" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean" scope="request"/>
<jsp:useBean id="projectBuildTypes" scope="request" type="java.util.List"/>

<div id="sharedResourcesPage">
  <c:choose>
    <c:when test="${not empty bean.sharedResourcesNames}">
      <table style="border: 1px solid;">
        <thead>
        <tr>
          <th colspan="2">Resource name</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="resourceName" items="${bean.sharedResourcesNames}">
          <tr>
            <td>
              <c:out value="${resourceName}"/>
            </td>
            <td>
              <a href="${url}&delete=${resourceName}">X</a>
            </td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:when>
    <c:otherwise>
      <c:out value="There are no resources available. Why don't you add one? =)"/>
    </c:otherwise>
  </c:choose>

  <form action="<c:url value='${url}'/>" method="post">
    <input type="hidden" name="sample" value="true"/>
    <forms:submit name="submitButton" label="Sample"/>
  </form>

</div>