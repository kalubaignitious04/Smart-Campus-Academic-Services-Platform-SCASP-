<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Courses">
  <form method="get" action="${pageContext.request.contextPath}/courses" class="inline">
    <input name="q" value="${f:h(q)}" placeholder="Search code or title" aria-label="Search courses" maxlength="50">
    <select name="sort" aria-label="Sort by">
      <option value="code" ${sort == 'code' ? 'selected' : ''}>Sort by code</option>
      <option value="title" ${sort == 'title' ? 'selected' : ''}>Sort by title</option>
      <option value="seats" ${sort == 'seats' ? 'selected' : ''}>Most seats left</option>
    </select>
    <button type="submit">Search</button>
  </form>
  <t:when test="${empty rows}"><p class="empty">No courses match your search.</p></t:when>
  <t:when test="${not empty rows}">
  <div class="scroll"><table>
    <thead><tr><th>Code</th><th>Title</th><th>Seats</th><th>Waiting</th><th>Status</th><th>Action</th></tr></thead>
    <tbody>
    <t:each items="${rows}" var="r">
      <tr>
        <td>${f:h(r.course.code)}</td><td>${f:h(r.course.title)}</td>
        <td>${r.registered} / ${r.course.capacity}</td><td>${r.waiting}</td><td><span class="tag ${r.course.status}">${r.course.status}</span></td>
        <td>
          <t:when test="${not sessionScope.user.role.staff}">
            <t:when test="${r.myState == 'REGISTERED'}">
              <form method="post" action="${pageContext.request.contextPath}/registrations"><t:csrf/><input type="hidden" name="action" value="withdraw"><input type="hidden" name="code" value="${f:h(r.course.code)}"><button class="secondary">Withdraw</button></form>
            </t:when>
            <t:when test="${r.myState == 'WAITLISTED'}">
              Waiting #${r.myPosition}
              <form method="post" action="${pageContext.request.contextPath}/registrations"><t:csrf/><input type="hidden" name="action" value="withdraw"><input type="hidden" name="code" value="${f:h(r.course.code)}"><button class="secondary">Leave list</button></form>
            </t:when>
            <t:when test="${empty r.myState and r.course.status == 'OPEN'}">
              <form method="post" action="${pageContext.request.contextPath}/registrations"><t:csrf/><input type="hidden" name="action" value="register"><input type="hidden" name="code" value="${f:h(r.course.code)}"><button>${r.seatsLeft > 0 ? 'Register' : 'Join waiting list'}</button></form>
            </t:when>
          </t:when>
          <t:when test="${sessionScope.user.role.staff}">
            <form method="post" action="${pageContext.request.contextPath}/courses" class="inline"><t:csrf/><input type="hidden" name="action" value="status"><input type="hidden" name="code" value="${f:h(r.course.code)}">
              <select name="status" aria-label="New status for ${f:h(r.course.code)}"><option>OPEN</option><option>CLOSED</option><option>CANCELLED</option></select><button class="secondary">Update</button></form>
          </t:when>
        </td>
      </tr>
    </t:each>
    </tbody>
  </table></div>
  </t:when>
  <t:when test="${sessionScope.user.role.staff}">
    <h2>Add or update a course</h2>
    <form method="post" action="${pageContext.request.contextPath}/courses" class="card inline"><t:csrf/><input type="hidden" name="action" value="save">
      <input name="code" placeholder="Code, e.g. CS301" required maxlength="20" aria-label="Course code">
      <input name="title" placeholder="Title" required maxlength="150" aria-label="Course title">
      <input name="capacity" type="number" min="1" max="1000" placeholder="Capacity" required aria-label="Capacity">
      <button type="submit">Save course</button>
    </form>
  </t:when>
</t:layout>
