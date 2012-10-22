<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="readLocks" type="java.util.List"--%>
<%--@elvariable id="writeLocks" type="java.util.List"--%>


<tr class="noBorder">
  <td colspan="2">
    <em>
      Locks can be added to the build configuration using Build Parameters:
    </em>
    <ul>
      <li>To add a read lock, create a build parameter <code>
        <nobr>'teamcity.locks.readLock.&lt;lock name&gt;'</nobr>
      </code>.
      </li>
      <li>To add a write lock, create a build parameter <code>
        <nobr>'teamcity.locks.writeLock.&lt;lock name&gt;'</nobr>
      </code>.
      </li>
    </ul>
  </td>
</tr>


<c:choose>
  <c:when test="${not empty readLocks or not empty writeLocks}">
    <tr>
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