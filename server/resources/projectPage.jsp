<%@ page import="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants" %>
<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:useBean id="project" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="bean" scope="request" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean"/>

<c:set var="PARAM_PROJECT_ID" value="<%=SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID%>"/>
<c:set var="PARAM_RESOURCE_NAME" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME%>"/>
<c:set var="PARAM_RESOURCE_QUOTA" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA%>"/>



<c:url var="url" value='editProject.html?projectId=${project.projectId}&tab=JetBrains.SharedResources'/>

<script type="text/javascript">

  BS.ResourceDialog = OO.extend(BS.AbstractModalDialog, {
    attachedToRoot: false,
    editMode: false,
    getContainer: function() {
      return $('resourceDialog');
    },

    showDialog: function() {
      this.showCentered();
      this.bindCtrlEnterHandler(this.submit.bind(this));
      this.editMode = false;
    },

    showEdit: function(resource_name, resource_quota) {
//      this.showCentered();
//      this.bindCtrlEnterHandler(this.submit.bind(this));
//      this.editMode = true;
    },

    submit: function() {
      if (!this.validate()) return false;
      this.close();
      BS.SharedResourcesActions.addResource();
      return false;
    },

    validate: function() {
      return true;
    },

    toggleQuotaSwitch: function() {
      var show = $j('#use_quota').is(':checked');
      console.log("toggle switch [" + show + "]");
      if (show) {
        this.quoted = true;
        BS.Util.show("quota_row");
      } else {
        this.quoted = false;
        BS.Util.hide("quota_row");
      }
    },

    quoted: false


  });

  //noinspection FunctionWithInconsistentReturnsJS
  BS.SharedResourcesActions = {
    addUrl: window['base_uri'] + "/sharedResourcesAdd.html",
    addResource: function() {
      // if quota checkbox in unchecked, send no quota info
      if (this.quoted) {
        //noinspection JSDuplicatedDeclaration
        BS.ajaxRequest(this.addUrl, {
          parameters: {'${PARAM_PROJECT_ID}':'${project.projectId}', '${PARAM_RESOURCE_NAME}':$j('#resource_name').val()},
          onSuccess: function() {
            window.location.reload();
          }
        });
      } else {
        //noinspection JSDuplicatedDeclaration
        BS.ajaxRequest(this.addUrl, {
          parameters: {'${PARAM_PROJECT_ID}':'${project.projectId}', '${PARAM_RESOURCE_NAME}':$j('#resource_name').val(), '${PARAM_RESOURCE_QUOTA}':$j('#resource_quota').val()},
          onSuccess: function() {
            window.location.reload();
          }
        });
      }



    },

    deleteUrl: window['base_uri'] + "/sharedResourcesDelete.html",
    deleteResource: function(resource_name) {
      //noinspection JSDuplicatedDeclaration
      BS.ajaxRequest(this.deleteUrl, {
        parameters: {'${PARAM_PROJECT_ID}':'${project.projectId}', '${PARAM_RESOURCE_NAME}': resource_name},
        onSuccess: function() {
          window.location.reload();
        }
      });
    }
  };
</script>

<div>
  <forms:addButton id="addNewResource" onclick="BS.ResourceDialog.showDialog();   return false">Add new resource</forms:addButton>
  <bs:dialog dialogId="resourceDialog" title="Resource Management" closeCommand="BS.ResourceDialog.close()">
    <table class="runnerFormTable">
      <tr>
        <th><label for="resource_name">Resource name:</label></th>
        <td>
          <forms:textField name="resource_name" id='resource_name' style="width: 98%" className="longField buildTypeParams" maxlength="40"/>
          <span class="smallNote">Specify name of the resource</span>
        </td>
      </tr>
      <tr>
        <th>Use quota:</th>
        <td>
          <forms:checkbox name="use_quota" id="use_quota" onclick="BS.ResourceDialog.toggleQuotaSwitch()" checked="false"/>
        </td>
      </tr>
      <tr id="quota_row" style="display: none">
        <th><label for="resource_quota">Resource quota:</label> </th>
        <td><forms:textField name="resource_quota"  style="width: 15%" id='resource_quota' className="longField buildTypeParams" maxlength="3"/></td>
      </tr>
    </table>
    <div class="popupSaveButtonsBlock">
      <forms:cancel onclick="BS.ResourceDialog.close()" showdiscardchangesmessage="false"/>
      <forms:submit id="resourcesDialogSubmit" type="button" label="Add Resource" onclick="BS.ResourceDialog.submit()"/>
    </div>
  </bs:dialog>


  <c:choose>
    <c:when test="${not empty bean.resources}">
      <l:tableWithHighlighting style="width: 70%" className="parametersTable" mouseovertitle="Click to edit resource" highlightImmediately="true">
        <%-- title--%>
        <tr>
          <th>Resource name</th>
          <th style="width:40%" colspan="4">Quota</th>
        </tr>
        <%-- /title--%>
        <c:forEach var="resource" items="${bean.resources}">
          <c:set var="onclick" value="BS.ResourceDialog.showEdit('${resource.name}, ${resource.quota}')"/>
          <tr>
            <td class="name highlight" onclick="${onclick}"><c:out value="${resource.name}"/></td>
            <c:choose>
              <c:when test="${resource.infinite}">
                <td class="highlight" onclick="${onclick}">Infinite</td>
              </c:when>
              <c:otherwise>
                <td class="highlight" onclick="${onclick}"><c:out value="${resource.quota}"/></td>
              </c:otherwise>
            </c:choose>
            <td class="highlight" onclick="${onclick}"> [Used in X configurations V] </td>
            <td class="edit highlight" onclick="${onclick}"><a href="#">edit</a> </td>
            <td class="edit"><a href="#" onclick="BS.SharedResourcesActions.deleteResource('${resource.name}')">delete</a> </td>
          </tr>
        </c:forEach>
      </l:tableWithHighlighting>
    </c:when>
    <c:otherwise>
      <p>
        <c:out value="There are no resources available. Why don't you add one? =)"/>
      </p>
    </c:otherwise>
  </c:choose>

</div>

