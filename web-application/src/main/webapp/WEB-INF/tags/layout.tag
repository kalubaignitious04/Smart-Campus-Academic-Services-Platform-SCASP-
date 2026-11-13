<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="plain" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${f:h(title)} | SCASP</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css">
</head>
<body>
<t:when test="${not empty sessionScope.user}">
  <header class="top">
    <a class="brand" href="${pageContext.request.contextPath}/dashboard">SCASP</a>
    <nav>
      <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
      <a href="${pageContext.request.contextPath}/courses">Courses</a>
      <t:when test="${not sessionScope.user.role.staff}"><a href="${pageContext.request.contextPath}/registrations">My registrations</a></t:when>
      <a href="${pageContext.request.contextPath}/requests">Requests</a>
      <a href="${pageContext.request.contextPath}/announcements">Announcements</a>
      <a href="${pageContext.request.contextPath}/campus">Campus</a>
      <t:when test="${sessionScope.user.role.staff}"><a href="${pageContext.request.contextPath}/reports">Reports</a></t:when>
    </nav>
    <form class="who" method="post" action="${pageContext.request.contextPath}/logout">
      <span>${f:h(sessionScope.user.name)} <small>(${sessionScope.user.role})</small></span>
      <t:csrf/><button type="submit" class="link">Log out</button>
    </form>
  </header>
</t:when>
<main>
  <h1>${f:h(title)}</h1>
  <t:when test="${not empty flash}"><p class="msg ok" role="status">${f:h(flash)}</p></t:when>
  <t:when test="${not empty flashError}"><p class="msg bad" role="alert">${f:h(flashError)}</p></t:when>
  <jsp:doBody/>
</main>
<footer>Smart Campus Academic Services Platform - BoCS3100 group project. Fictional demo data only.</footer>
</body>
</html>
