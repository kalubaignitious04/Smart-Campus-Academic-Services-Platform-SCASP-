<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Sign in" plain="${true}">
  <t:when test="${not empty loginError}"><p class="msg bad" role="alert">${f:h(loginError)}</p></t:when>
  <form method="post" action="${pageContext.request.contextPath}/login" class="card narrow">
    <label for="userId">User ID</label>
    <input id="userId" name="userId" value="${f:h(userId)}" required maxlength="20" autofocus>
    <label for="password">Password</label>
    <input id="password" name="password" type="password" required maxlength="100">
    <button type="submit">Sign in</button>
    <p class="hint">Demo accounts: S1001 to S1005 (students), T2001 (academic staff), A3001 (admin). The demo password is in the README.</p>
  </form>
</t:layout>
