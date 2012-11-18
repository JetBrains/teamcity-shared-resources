<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="locks" scope="request" type="java.util.Map"/>




<script type="text/javascript">
  //noinspection JSUnusedGlobalSymbols
  BS.AddLockDialog = OO.extend(BS.AbstractModalDialog, {

    attachedToRoot: false,

    getContainer: function() {
      return $('addLockDialog');
    },

    showDialog: function() {
      $('newLockName').value = "";

      this.showCentered();
      this.bindCtrlEnterHandler(this.submit.bind(this));
    },

    /**
     * Shows dialog used to edit lock
     * @param lockName name of the lock
     * @param lockType type of the lock
     */
    showEdit: function(lockName, lockType) {
      $('newLockName').value = lockName;
      $j('#newLockType option').each(function() {
        var self = $j(this);
        //noinspection JSUnresolvedFunction
        self.prop("selected", self.val() == lockType);
      });
      this.showCentered();
      this.bindCtrlEnterHandler(this.submit.bind(this));
    },

    submit: function() {
      if (!this.validate()) return false;
      var element = $('${keys.locksFeatureParamKey}');
      var lockName = $('newLockName').value;
      //noinspection JSUnresolvedFunction
      var lockType = $j('#newLockType option:selected').val();
      var val = element.value;
      if (val[val.length - 1] !== '\n') {
        val += '\n'
      }
      val += lockName;
      val += ' ';
      val += lockType;
      val += '\n';
      element.value = val;
      this.addLockToTakenLocks(lockName, lockType);
      this.close();
      return false;
    },

    validate: function() {
      var _name = $('newLockName').value;
      if (_name.length === 0) {
        alert('Please enter lock name');
        return false;
      }
      return true;
    },

    addLockToTakenLocks: function(lockName, lockType) {
      var row = "<tr><td>" + lockName + "</td><td>" + lockType +"</td>";
      row += "<td class=\"remove\"><a href=\"#\" onclick=\"BS.AddLockDialog.deleteLockFromTakenLocks  (' + lockName + '); return false\">delete</a></td>";
      row += "</tr>";
      $j('#locksTaken tbody tr:last').after(row);
    },

    deleteLockFromTakenLocks: function(lockName) {
      console.log("Deleting lock: [" + lockName + "]");
    }
  });



</script>

<tr>
  <td colspan="2" style="padding:0; margin:0;">
    <table id="locksTaken" class="dark borderBottom">
      <tr>
        <th>Lock Name</th>
        <th style="width: 10%">Lock Type</th>
        <th style="width: 5%">&nbsp;</th>
      </tr>

      <c:forEach var="item" items="${locks}">
        <tr>
          <td><c:out value="${item.key}"/></td>
          <td><c:out value="${item.value}"/></td>
          <td class="remove"><a href="#" onclick="BS.AddLockDialog.deleteLockFromTakenLocks('${item.key}'); return false">delete</a></td>
        </tr>
      </c:forEach>
    </table>
  </td>
</tr>

<tr class="noBorder" style="display: none">
  <th><label for="${keys.locksFeatureParamKey}">Resource name:</label></th>
  <td>
    <props:multilineProperty name="${keys.locksFeatureParamKey}" linkTitle="Enter shared resource name(s)" cols="49" rows="5" expanded="${false}"/>
    <span class="error" id="error_${keys.locksFeatureParamKey}"></span>
    <span class="smallNote">Please specify shared resources that must be locked during build</span>
  </td>
</tr>

<tr class="noBorder">
  <td colspan="2">
    <forms:addButton id="addNewLock" onclick="BS.AddLockDialog.showDialog(); return false">Add lock</forms:addButton>
    <%--<forms:addButton id="editLock1" onclick="BS.AddLockDialog.showEdit('name1', 'readLock'); return false">Edit read lock</forms:addButton> &lt;%&ndash;edit read lock &ndash;%&gt;--%>
    <%--<forms:addButton id="editLock2" onclick="BS.AddLockDialog.showEdit('name2', 'writeLock'); return false">Edit write lock</forms:addButton> &lt;%&ndash; edit write lock &ndash;%&gt;--%>

    <bs:dialog dialogId="addLockDialog" title="Add Lock" closeCommand="BS.AddLockDialog.close()">
      <table class="runnerFormTable">
        <tr>
          <th><label for="newLockName">Lock name:</label></th>
          <td>
            <forms:textField id="newLockName" name="newLockName" style="width: 98%" className="longField buildTypeParams" defaultText=""/>
          </td>
        </tr>
        <tr>
          <th><label for="newLockType">Lock type:</label></th>
          <td>
            <forms:select name="newLockType" id="newLockType" style="width: 60%">
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
    <%--<script type="text/javascript">--%>
    <%--BS.MultilineProperties.updateVisible();--%>
    <%--</script>--%>
  </td>
</tr>


