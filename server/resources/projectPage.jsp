<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/include-internal.jsp" %>

<c:url var="url" value='project.html?projectId=${project.projectId}&tab=sharedResources'/>
<jsp:useBean id="bean" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean" scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>
<jsp:useBean id="projectBuildTypes" scope="request" type="java.util.List"/>


<div id="sharedResourcesPage">

  <c:choose>
    <c:when test="${not empty bean.sharedResourcesNames}">
      <l:tableWithHighlighting className="dark borderBottom"  highlightImmediately="true">
        <%-- title--%>
        <thead>
        <tr>
          <th>Resource name</th>
          <th>Usage</th>
          <th>Operations</th>
        </tr>
        </thead>
        <%-- /title--%>
        <tbody>
        <c:forEach var="resourceName" items="${bean.sharedResourcesNames}">
        <tr>
          <td><c:out value="${resourceName}"/></td>
          <td> ... </td>
          <td><span onclick="BS.SharedResourcesActions.deleteResource('${project.projectId}', '${resourceName}');">Delete</span></td>
        </tr>
        </c:forEach>
        </tbody>
      </l:tableWithHighlighting>
    </c:when>
    <c:otherwise>
      <c:out value="There are no resources available. Why don't you add one? =)"/>
    </c:otherwise>
  </c:choose>


  <script type="text/javascript">
    //noinspection FunctionWithInconsistentReturnsJS
    BS.SharedResourcesActions = {
      deleteUrl: window['base_uri'] + "/sharedResourcesDelete.html",
      deleteResource: function(project_id, shared_resource) {
        if (!confirm('Are you sure you want to delete this shared resource?')) return false;

        BS.ajaxRequest(this.deleteUrl, {
          parameters: {'project_id':  project_id, 'delete': shared_resource},
          onSuccess: function() {
            window.location.reload();
          }
        });
      }
    };
  </script>


  <%--<form action="<c:url value='${myUrl}'/>" method="post">--%>
    <%--<input type="hidden" name="sample" value="true"/>--%>
    <%--<forms:submit name="submitButton" label="Sample"/>--%>
  <%--</form>--%>

  <form action="<c:url value='${url}'/>" method="post">
    <forms:textField name="new_resource" className="longField" maxlength="80"/>
    <forms:submit name="submitButton" label="Add"/>
  </form>

  <%--mouseovertitle="${linkTitle}"--%>


</div>