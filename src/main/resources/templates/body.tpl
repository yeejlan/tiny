<%=view.render("header")%>
this is body ${wall}
call helper: <%=SquareHelper.getSquare(15)%>
<%=view.render("footer")%>