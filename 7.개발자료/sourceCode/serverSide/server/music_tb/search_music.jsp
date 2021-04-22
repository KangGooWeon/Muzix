<%@ page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.*" %>
<%@ page import="java.util.ArrayList"%>
<%
String url = "jdbc:mysql://localhost:3306/muzix";
String id = "root";
String pwd = "essence3";
PreparedStatement pstmt;
Statement stmt= null;
ResultSet rs= null;
ResultSet av_rs= null;
Connection conn=null;
JSONObject json = new JSONObject();
JSONArray list = new JSONArray();
String member_id = request.getParameter("member_id");
String keyword = request.getParameter("keyword");
try{
  Class.forName("com.mysql.jdbc.Driver");
  conn = DriverManager.getConnection(url,id,pwd);
  if(keyword.equals("N")){
      pstmt = conn.prepareStatement("select * from music_tb where scope NOT IN(0) and writer_id NOT IN(?)");
      pstmt.setString(1, member_id);
  }
  else{
      pstmt = conn.prepareStatement("select * from music_tb where scope NOT IN(0) and writer_id NOT IN(?) and title like ? ");
      pstmt.setString(1, member_id);
      pstmt.setString(2,"%"+keyword+"%");
  }
  rs=pstmt.executeQuery();
  while(rs.next()){
	JSONObject temp = new JSONObject();
    	temp.put("writer",rs.getString("writer_id"));
        temp.put("music_id",rs.getString("music_id"));
    	temp.put("title",rs.getString("title"));
        temp.put("genre",rs.getString("genre"));
        temp.put("cover_img",rs.getString("cover_img_url"));
        temp.put("melody_csv",rs.getString("melody_csv"));
        temp.put("chord_txt",rs.getString("chord_txt"));
    	temp.put("scope",rs.getBoolean("scope"));
    	temp.put("modify_date",rs.getString("modify_date"));
        list.add(temp);
 }
json.put("result","success");
json.put("list",list);
  out.clear();
}catch(SQLException sqlException){
   //out.print(sqlException);
  json.put("result","fail");
}catch(Exception exception){
  // out.print(exception);
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
