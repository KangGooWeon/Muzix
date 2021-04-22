<%@ page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.*" %>
<%
String url = "jdbc:mysql://localhost:3306/muzix";
String id = "root";
String pwd = "essence3";
PreparedStatement pstmt =null;
Statement stmt= null;
ResultSet rs= null;
Connection conn=null;
JSONObject json = new JSONObject();
try{
  String update_type = request.getParameter("update_type");
  String music_id = request.getParameter("music_id");
  String title = request.getParameter("title");
  String genre = request.getParameter("genre");
  String cover_img_url = request.getParameter("cover_img_url");
  String scope = request.getParameter("scope");
  
  
  Class.forName("com.mysql.jdbc.Driver");
  conn = DriverManager.getConnection(url,id,pwd);
  if(update_type.equals("change_scope")){
      pstmt = conn.prepareStatement("update music_tb set scope = ? where music_id = ?");
      pstmt.setBoolean(1,Boolean.parseBoolean(scope));
      pstmt.setString(2,music_id);
  }
  else if(update_type.equals("change_title")){
      pstmt = conn.prepareStatement("update music_tb set title = ? where music_id = ?");
      pstmt.setString(1,title);
      pstmt.setString(2,music_id);
  }
  else if(update_type.equals("change_genre")){
      pstmt = conn.prepareStatement("update music_tb set genre = ? where music_id = ?");
      pstmt.setString(1,genre);
      pstmt.setString(2,music_id);
  }
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
