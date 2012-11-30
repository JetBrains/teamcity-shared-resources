<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:useBean id="project" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="bean" scope="request" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean"/>

<c:url var="url" value='editProject.html?projectId=${project.projectId}&tab=JetBrains.SharedResources'/>

<script type="text/javascript">

  BS.ResourceDialog = OO.extend(BS.AbstractModalDialog, {
    attachedToRoot: false,
    getContainer: function() {
      return $('resourceDialog');
    },

    showDialog: function() {
      this.showCentered();
      this.bindCtrlEnterHandler(this.submit.bind(this));
    },
    submit: function() {
      if (!this.validate()) return false;
      this.close();
      BS.SharedResourcesActions.addResource();
      return false;
    },

    validate: function() {
      return true;
    }
  });

</script>

<div>

  <forms:addButton id="addNewResource" onclick="BS.ResourceDialog.showDialog();   return false">Add new resource</forms:addButton>
  <bs:dialog dialogId="resourceDialog" title="Resource Managemet" closeCommand="BS.ResourceDialog.close()">
    <table class="runnerFormTable">
      <tr>
        <th><label for="new_resource">Resource name:</label></th>
        <td><forms:textField name="new_resource" id='new_resource' style="width: 98%" className="longField buildTypeParams" maxlength="80"/></td>
      </tr>
      <tr>
        <th><label for="new_resource_quota">Resource quota:</label> </th>
        <td><forms:textField name="new_resource_quota"  style="width: 15%" id='new_resource_quota' className="longField buildTypeParams" maxlength="3"/></td>
      </tr>
    </table>
    <div class="popupSaveButtonsBlock">
      <forms:cancel onclick="BS.ResourceDialog.close()" showdiscardchangesmessage="false"/>
      <forms:submit id="locksDialogSubmit" type="button" label="Add Lock" onclick="BS.ResourceDialog.submit()"/>
    </div>
  </bs:dialog>

  <p>
    <c:choose>
      <c:when test="${not empty bean.resources}">
        <l:tableWithHighlighting className="dark borderBottom"  highlightImmediately="true">
          <%-- title--%>
          <tr>
            <th>Resource name</th>
            <th>Quota</th>
            <th style="width:20%">Usage</th>
            <th style="width:15%" colspan="2">Operations</th>
          </tr>
          <%-- /title--%>
          <c:forEach var="resource" items="${bean.resources}">
            <tr>
              <td><c:out value="${resource.name}"/></td>
              <c:choose>
                <c:when test="${resource.infinite}">
                  <td>Infinite</td>
                </c:when>
                <c:otherwise>
                  <td><c:out value="${resource.quota}"/></td>
                </c:otherwise>
              </c:choose>
              <td> [Used in X configurations V] </td>
              <td class="edit"><a href="#">edit</a> </td>
              <td class="remove"><a href="#">delete</a> </td>
            </tr>
          </c:forEach>
        </l:tableWithHighlighting>
      </c:when>
      <c:otherwise>
        <c:out value="There are no resources available. Why don't you add one? =)"/>
      </c:otherwise>
    </c:choose>
  </p>

  <div>

    <%--<forms:submit name="submitButton" onclick="BS.SharedResourcesActions.addResource()" label="Add"/>--%>
  </div>


  </p>
</div>


<script type="text/javascript">
  //noinspection FunctionWithInconsistentReturnsJS
  BS.SharedResourcesActions = {
    addUrl: window['base_uri'] + "/sharedResourcesAdd.html",
    addResource: function() {
      BS.ajaxRequest(this.addUrl, {
        parameters: {'project_id':  '${project.projectId}', 'new_resource': $j('#new_resource').val(), 'new_resource_quota': $j('#new_resource_quota').val()},
        onSuccess: function() {
          window.location.reload();
        }
      });
    }
  };
</script>