<%@ page language="java" contentType="text/html;charset=UTF-8"%>
<%@ page import="java.sql.*"%>
<%@ page import="org.json.simple.*"%>

<%
String url = "jdbc:mysql://localhost:3306/muzix";
String root_id = "root";
String root_pass = "essence3";

PreparedStatement pstmt=null;
ResultSet rs = null;
Connection conn = null;

String member_id = request.getParameter("member_id");

try{
	Class.forName("com.mysql.jdbc.Driver");
	conn = DriverManager.getConnection(url,root_id,root_pass);
	pstmt = conn.prepareStatement("select * from member_tb where id= ?");
	pstmt.setString(1,member_id);
	rs = pstmt.executeQuery();

	JSONObject json = new JSONObject();
	
	if(rs.next()){
		json.put("result","success");
		json.put("password",rs.getString("password"));
		json.put("nickname",rs.getString("nickname"));
	}
	else{
		json.put("result","fail");
	}
	
	out.clear();
	out.print(json);
}catch(SQLException e){
	out.println("SQL error");
}catch(Exception e){
	out.println("connection e");
}finally{
	if(rs != null)
		try{rs.close();}catch(SQLException ex){}
	if(pstmt != null)
		try{pstmt.close();}catch(SQLException ex){}
	if(conn != null)
		try{conn.close();}catch(SQLException ex){}
}
%>
