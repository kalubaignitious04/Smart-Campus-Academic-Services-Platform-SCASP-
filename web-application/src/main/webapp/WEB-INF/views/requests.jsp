<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Service requests">
  <t:when test="${not sessionScope.user.role.staff}">
    <h2>New request</h2>
    <form method="post" action="${pageContext.request.contextPath}/requests" class="card stack"><t:csrf/><input type="hidden" name="action" value="submit">
      <label for="category">Category</label>
      <select id="category" name="category"><t:each items="${categories}" var="c"><option>${c}</option></t:each></select>
      <label for="priority">Priority</label>
      <select id="priority" name="priority"><t:each items="${priorities}" var="p"><option ${p == 'NORMAL' ? 'selected' : ''}>${p}</option></t:each></select>
      <label for="details">Details (max 500 characters)</label>
      <textarea id="details" name="details" rows="3" maxlength="500" required></textarea>
      <button type="submit">Submit request</button>
    </form>
  </t:when>
  <h2>${sessionScope.user.role.staff ? 'All requests (most urgent first)' : 'My requests'}</h2>
  <t:when test="${empty requests}"><p class="empty">No requests yet.</p></t:when>
  <t:when test="${not empty requests}">
  <div class="scroll"><table>
    <thead><tr><th>#</th><th>Student</th><th>Category</th><th>Priority</th><th>Status</th><th>Assigned to</th><th>Details</th><th>Updated</th><t:when test="${sessionScope.user.role.staff}"><th>Actions</th></t:when></tr></thead>
    <tbody>
    <t:each items="${requests}" var="r">
      <tr><td>${r.requestId}</td><td>${f:h(r.studentId)}</td><td>${r.category}</td><td><span class="tag ${r.priority}">${r.priority}</span></td><td>${r.status}</td>
        <td>${empty r.assignedTo ? '-' : f:h(r.assignedTo)}</td><td>${f:h(r.details)}</td><td>${f:time(r.updatedAt)}</td>
        <t:when test="${sessionScope.user.role.staff}"><td>
          <form method="post" action="${pageContext.request.contextPath}/requests" class="inline"><t:csrf/><input type="hidden" name="action" value="assign"><input type="hidden" name="id" value="${r.requestId}">
            <select name="assignee" aria-label="Assign request ${r.requestId}"><t:each items="${staff}" var="s"><option value="${f:h(s.userId)}">${f:h(s.name)}</option></t:each></select><button class="secondary">Assign</button></form>
          <form method="post" action="${pageContext.request.contextPath}/requests" class="inline"><t:csrf/><input type="hidden" name="action" value="advance"><input type="hidden" name="id" value="${r.requestId}">
            <select name="status" aria-label="New status for request ${r.requestId}"><t:each items="${statuses}" var="s"><option>${s}</option></t:each></select><button class="secondary">Move</button></form>
        </td></t:when></tr>
    </t:each>
    </tbody>
  </table></div>
  </t:when>
</t:layout>
