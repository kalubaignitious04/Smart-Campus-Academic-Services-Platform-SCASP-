<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Reports">
  <h2>Course capacity</h2>
  <div class="scroll"><table><thead><tr><th>Code</th><th>Title</th><th>Registered</th><th>Capacity</th><th>Full</th><th>Waiting</th><th>Status</th></tr></thead><tbody>
    <t:each items="${capacity}" var="c"><tr><td>${f:h(c.code)}</td><td>${f:h(c.title)}</td><td>${c.registered}</td><td>${c.capacity}</td><td>${c.percentFull}%</td><td>${c.waiting}</td><td>${c.status}</td></tr></t:each>
  </tbody></table></div>
  <h2>Waiting lists</h2>
  <t:when test="${empty waiting}"><p class="empty">Nobody is waiting for a seat.</p></t:when>
  <ul class="plain"><t:each items="${waiting}" var="c"><li>${f:h(c.code)}: ${c.waiting} waiting</li></t:each></ul>
  <h2>Request workload</h2>
  <ul class="plain"><t:each items="${byStatus.entrySet()}" var="e"><li>${e.key}: ${e.value}</li></t:each></ul>
  <p>Open requests by staff member:</p>
  <t:when test="${empty byAssignee}"><p class="empty">No open requests.</p></t:when>
  <ul class="plain"><t:each items="${byAssignee.entrySet()}" var="e"><li>${f:h(e.key)}: ${e.value}</li></t:each></ul>
  <h2>Student registrations</h2>
  <div class="scroll"><table><thead><tr><th>Student</th><th>Name</th><th>Registered</th><th>Waiting</th></tr></thead><tbody>
    <t:each items="${students}" var="s"><tr><td>${f:h(s.studentId)}</td><td>${f:h(s.name)}</td><td>${s.registered}</td><td>${s.waiting}</td></tr></t:each>
  </tbody></table></div>
  <h2>Audit trail (latest 15)</h2>
  <div class="scroll"><table><thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Entity</th></tr></thead><tbody>
    <t:each items="${audit}" var="a"><tr><td>${f:time(a.occurredAt)}</td><td>${f:h(a.actor)}</td><td>${f:h(a.action)}</td><td>${f:h(a.entityType)}/${f:h(a.entityId)}</td></tr></t:each>
  </tbody></table></div>
  <h2>System</h2>
  <p>Request processor: ${stats.submitted()} submitted, ${stats.succeeded()} succeeded, ${stats.failed()} failed, ${stats.rejected()} rejected.
     TCP gateway port ${app.gatewayPort()}, RMI registry port ${app.rmiPort()}.</p>
</t:layout>
