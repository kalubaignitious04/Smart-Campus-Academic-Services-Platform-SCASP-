<%@ tag body-content="scriptless" pageEncoding="UTF-8" description="Loops over a collection (replaces JSTL forEach)" %>
<%@ attribute name="items" required="true" type="java.util.Collection" %>
<%@ attribute name="var" required="true" rtexprvalue="false" %>
<%@ variable name-from-attribute="var" alias="cur" scope="NESTED" %>
<% if (items != null) { for (Object o : items) { jspContext.setAttribute("cur", o); %><jsp:doBody/><% } } %>
