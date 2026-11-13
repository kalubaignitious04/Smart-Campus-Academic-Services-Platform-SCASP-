<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="f" uri="urn:scasp" %>
<t:layout title="Campus services">
  <t:each items="${places}" var="p">
    <article class="card"><h3>${f:h(p.name)}</h3><p>${f:h(p.description)}</p>
      <a href="https://www.openstreetmap.org/?mlat=${p.latitude}&amp;mlon=${p.longitude}#map=18/${p.latitude}/${p.longitude}" target="_blank" rel="noopener noreferrer">Open map (${p.latitude}, ${p.longitude})</a></article>
  </t:each>
</t:layout>
