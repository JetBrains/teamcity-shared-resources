<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>


<script type="text/javascript">
  /**
   * contains UI actions for feature editing dialog
   */
  BS.AddLockDialog = OO.extend(BS.AbstractModalDialog, {
    attachedToRoot: false,

    getContainer: function() {
      console.log("getContainer");
      return $('addLockDialog');
    },

    showDialog: function() {
      console.log("showDialog!");
      $('newLockName').value = "";
      this.showCentered();
      this.bindCtrlEnterHandler(this.submit.bind(this));
    },

    submit: function() {
      console.log("submit");
      if (!this.validate()) return false;
      var val = $('LOCKS').value;
      if (val[val.length - 1] !== '\n') {
        val += '\n'
      }
      val += $('newLockName').value;
      val += ' ';
      val += $('newLockType').options[$('newLockType').selectedIndex].value;

      $('LOCKS').value = val;
      this.close();
      return false;
    },

    validate: function() {
      console.log("validate");
      /* 1) name not empty
       * 2) name is not repeated
       */

      return true;
    }
  });
</script>




<tr class="noBorder">
  <th><label for="${keys.locksFeatureParamKey}">Resource name:</label></th>
  <td>
    <props:multilineProperty name="${keys.locksFeatureParamKey}" linkTitle="Enter shared resource name(s)" cols="49" rows="3" value="${propertiesBean.properties['resource-name']}" expanded="${true}"/>
    <span class="error" id="error_${keys.locksFeatureParamKey}"></span>
    <span class="smallNote">Specify shared resource name(s)</span>
  </td>
</tr>

<tr class="noBorder">
  <td colspan="2">
    <p>Please specify shared resource(s) that must be locked during build</p>
  </td>
</tr>

<tr class="noBorder">
  <td colspan="2">
    <forms:addButton id="addNewLock" onclick="BS.AddLockDialog.showDialog(); return false">Add lock</forms:addButton>
    <bs:dialog dialogId="addLockDialog" title="Add Lock" closeCommand="BS.AddLockDialog.close()">
      <table class="runnerFormTable">
        <tr>
          <td>
            <forms:textField name="newLockName" style="width: 98%" className="buildTypeParams" defaultText=""/>
          </td>
        </tr>
        <tr>
          <td>
            <forms:select name="newLockType">
              <forms:option value="readLock">Read Lock</forms:option>
              <forms:option value="writeLock">Write Lock</forms:option>
            </forms:select>
          </td>
        </tr>

      </table>

      <div class="popupSaveButtonsBlock">
        <forms:cancel onclick="BS.AddLockDialog.close()" showdiscardchangesmessage="false"/>
        <forms:submit type="button" label="Add Lock" onclick="BS.AddLockDialog.submit()"/>
      </div>
    </bs:dialog>
    <script type="text/javascript">
      BS.MultilineProperties.updateVisible();
    </script>
  </td>
</tr>


