<%@ page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.*" %>
<%
String url = "jdbc:mysql://localhost:3306/muzix";
String id = "root";
String pwd = "essence3";
PreparedStatement pstmt;
Statement stmt= null;
ResultSet rs= null;
Connection conn=null;
JSONObject json = new JSONObject();
try{
  String member_id = request.getParameter("member_id");
  String password = request.getParameter("password");
  String nickname = request.getParameter("nickname");
  
  Class.forName("com.mysql.jdbc.Driver");
  conn = DriverManager.getConnection(url,id,pwd);
  pstmt = conn.prepareStatement("update member_tb set password=?,nickname=? where id=?");
  pstmt.setString(1,password);
  pstmt.setString(2,nickname);
  pstmt.setString(3,member_id);
  pstmt.executeUpdate();
  json.put("result","success");
  out.clear();
}catch(SQLException sqlException){
  json.put("result","fail");
}catch(Exception exception){
  json.put("result","fail");
}finally{
  out.print(json);
  if( rs != null )
   try{ rs.close(); } catch(SQLException ex) {}
  if( stmt != null )
   try { stmt.close(); } catch(SQLException ex) {}
  if( conn != null )
   try{ conn.close(); } catch(Exception ex){}
}
%>
