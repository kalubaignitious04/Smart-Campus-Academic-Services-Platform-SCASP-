<%@ tag body-content="scriptless" pageEncoding="UTF-8" description="Renders its body when test is true (replaces JSTL if)" %>
<%@ attribute name="test" required="true" type="java.lang.Boolean" %>
<% if (test != null && test) { %><jsp:doBody/><% } %>
