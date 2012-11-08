<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="readLocks" type="java.util.List"--%>
<%--@elvariable id="writeLocks" type="java.util.List"--%>
<jsp:useBean id="bean" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean" scope="request"/>


<%--<tr class="noBorder">--%>
<%--<td colspan="2">--%>
<%--<em>--%>
<%--Locks can be added to the build configuration using Build Parameters:--%>
<%--</em>--%>
<%--<ul>--%>
<%--<li>To add a read lock, create a build parameter <code>--%>
<%--<nobr>'teamcity.locks.readLock.&lt;lock name&gt;'</nobr>--%>
<%--</code>.--%>
<%--</li>--%>
<%--<li>To add a write lock, create a build parameter <code>--%>
<%--<nobr>'teamcity.locks.writeLock.&lt;lock name&gt;'</nobr>--%>
<%--</code>.--%>
<%--</li>--%>
<%--</ul>--%>
<%--</td>--%>
<%--</tr>--%>


<c:choose>
  <c:when test="${not empty readLocks or not empty writeLocks}">
    <tr class="noBorder">
      <td colspan="2">
        <c:if test="${not empty readLocks}">
          <p>Currently used read locks:</p>
          <ul>
            <c:forEach items="${readLocks}" var="l">
              <li>${l}</li>
            </c:forEach>
          </ul>
        </c:if>

        <c:if test="${not empty writeLocks}">
          <p>Currently used write locks:</p>
          <ul>
            <c:forEach items="${writeLocks}" var="l">
              <li>${l}</li>
            </c:forEach>
          </ul>
        </c:if>
      </td>
    </tr>
  </c:when>
  <c:otherwise>
    <tr>
      <td colspan="2">
        <p>No locks are currently used</p>
      </td>
    </tr>
  </c:otherwise>
</c:choose>


<c:choose>
  <c:when test="${not empty bean.sharedResourcesNames}">
    <%--<tr class="noBorder">--%>
      <%--<td><label for="res">Resource Name</label></td>--%>
      <%--<td><label for="type">Lock Type</label></td>--%>
    <%--</tr>--%>
    <tr class="noBorder">
      <td>
        <forms:select name="res">
          <c:forEach var="resourceName" items="${bean.sharedResourcesNames}">
            <forms:option value="${resourceName}">
              <c:out value="${resourceName}"/>
            </forms:option>
          </c:forEach>
        </forms:select>
      </td>
      <td>
        <forms:select name="type">
          <forms:option value="read"><c:out value="read"/></forms:option>
          <forms:option value="write"><c:out value="write"/></forms:option>
        </forms:select>
        <forms:addButton/>
      </td>
    </tr>
  </c:when>
</c:choose>


