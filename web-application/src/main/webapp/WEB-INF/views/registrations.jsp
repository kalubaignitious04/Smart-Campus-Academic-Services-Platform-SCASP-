<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="My registrations">
  <t:when test="${empty rows}"><p class="empty">You have no registrations yet. Visit <a href="${pageContext.request.contextPath}/courses">Courses</a> to register.</p></t:when>
  <t:when test="${not empty rows}">
  <div class="scroll"><table>
    <thead><tr><th>Code</th><th>Title</th><th>State</th><th>Action</th></tr></thead>
    <tbody>
    <t:each items="${rows}" var="r">
      <tr><td>${f:h(r.course.code)}</td><td>${f:h(r.course.title)}</td>
        <td>${r.myState == 'WAITLISTED' ? 'Waiting list, position '.concat(r.myPosition) : 'Registered'}</td>
        <td><form method="post" action="${pageContext.request.contextPath}/registrations"><t:csrf/><input type="hidden" name="action" value="withdraw"><input type="hidden" name="from" value="mine"><input type="hidden" name="code" value="${f:h(r.course.code)}"><button class="secondary">${r.myState == 'WAITLISTED' ? 'Leave list' : 'Withdraw'}</button></form></td></tr>
    </t:each>
    </tbody>
  </table></div>
  </t:when>
</t:layout>
