<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="bean" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean" scope="request"/>
<jsp:useBean id="projectId" type="java.lang.String" scope="request"/>


<form>

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
              <input type="button" value="Delete" class="btn" onclick="BS.SharedResourcesActions.deleteResource('${projectId}', '${resourceName}');"/>
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
</form>


<script type="text/javascript">
  BS.SharedResourcesActions = {
    url: window['base_uri'] + "/sharedResourcesDelete.html",

    deleteResource: function(project_id, shared_resource) {
      if (!confirm('Are you sure you want to delete this shared resource?')) return false;

      BS.ajaxRequest(this.url, {
        parameters: {'project_id':  project_id,
          'delete': shared_resource},
        onSuccess: function() {
          window.location.reload();
        }
      });
    }
  };
</script>