<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<%@ page isErrorPage="true" %>
<t:layout title="${empty errorStatus ? 'Something went wrong' : 'Error '.concat(errorStatus)}">
  <p>${empty errorMessage ? 'The page could not be found or something went wrong.' : f:h(errorMessage)}</p>
  <p><a href="${pageContext.request.contextPath}/dashboard">Back to the dashboard</a></p>
</t:layout>
