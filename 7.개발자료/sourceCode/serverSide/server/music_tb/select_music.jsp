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
String result = "fail";

String member_id = request.getParameter("member_id");

JSONObject json = new JSONObject();
JSONArray list = new JSONArray();
try{
	Class.forName("com.mysql.jdbc.Driver");
	conn = DriverManager.getConnection(url,root_id,root_pass);
	pstmt = conn.prepareStatement("select * from music_tb where writer_id= ?");
	pstmt.setString(1,member_id);
	rs = pstmt.executeQuery();

	while(rs.next()){
            JSONObject temp = new JSONObject();
            temp.put("music_id",rs.getString("music_id"));
            temp.put("title",rs.getString("title"));
            temp.put("writer",rs.getString("writer_id"));
            temp.put("genre",rs.getString("genre"));
            temp.put("cover_img_url",rs.getString("cover_img_url"));
            temp.put("melody_csv",rs.getString("melody_csv"));
            temp.put("chord_txt",rs.getString("chord_txt"));
            temp.put("scope",rs.getBoolean("scope"));
            temp.put("modify_date",rs.getString("modify_date"));
            list.add(temp);
        }
        result = "success";
	out.clear();
}catch(SQLException e){
    result = "fail";
}catch(Exception e){
    result = "fail";
}finally{
    json.put("result",result);
    json.put("list",list);
    out.print(json);
	if(rs != null)
		try{rs.close();}catch(SQLException ex){}
	if(pstmt != null)
		try{pstmt.close();}catch(SQLException ex){}
	if(conn != null)
		try{conn.close();}catch(SQLException ex){}
}
%>
