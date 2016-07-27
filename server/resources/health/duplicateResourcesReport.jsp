<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<bs:out value="${healthStatusItem.category.name}"/>
<bs:out value="${healthStatusItem.identity}"/>
