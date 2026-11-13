<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Announcements">
  <t:when test="${empty announcements}"><p class="empty">No announcements yet.</p></t:when>
  <t:each items="${announcements}" var="a">
    <article class="card"><h3>${f:h(a.title)} <span class="tag">${a.audience}</span></h3><p>${f:h(a.body)}</p><small>${f:time(a.publishedAt)}</small></article>
  </t:each>
  <t:when test="${sessionScope.user.role.staff}">
    <h2>Publish an announcement</h2>
    <form method="post" action="${pageContext.request.contextPath}/announcements" class="card stack"><t:csrf/>
      <label for="title">Title</label><input id="title" name="title" required maxlength="150">
      <label for="body">Message</label><textarea id="body" name="body" rows="3" required maxlength="2000"></textarea>
      <label for="audience">Audience</label>
      <select id="audience" name="audience"><t:each items="${audiences}" var="x"><option>${x}</option></t:each></select>
      <button type="submit">Publish</button>
    </form>
  </t:when>
</t:layout>
