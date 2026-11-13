<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Dashboard">
  <div class="stats">
    <div class="stat"><b>${courseCount}</b><span>courses</span></div>
    <t:when test="${not sessionScope.user.role.staff}"><div class="stat"><b>${myCount}</b><span>my registrations and waiting places</span></div></t:when>
    <div class="stat"><b>${requestCount}</b><span>open service requests</span></div>
  </div>
  <h2>Latest announcements</h2>
  <t:when test="${empty announcements}"><p class="empty">No announcements yet.</p></t:when>
  <t:each items="${announcements}" var="a">
    <article class="card"><h3>${f:h(a.title)}</h3><p>${f:h(a.body)}</p><small>${f:time(a.publishedAt)}</small></article>
  </t:each>
  <t:when test="${sessionScope.user.role.staff}">
    <h2>Recent activity</h2>
    <t:when test="${empty notifications}"><p class="empty">No activity yet. Register a student for a course to see events here.</p></t:when>
    <ul class="plain"><t:each items="${notifications}" var="n"><li><small>${f:time(n.at)}</small> ${f:h(n.message)}</li></t:each></ul>
  </t:when>
</t:layout>
