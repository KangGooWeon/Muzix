<%@ page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.json.simple.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.*" %>

<%
String url = "jdbc:mysql://localhost:3306/muzix";
String id = "root";
String pwd = "essence3";
PreparedStatement pstmt;
Statement stmt= null;
ResultSet rs= null;
Connection conn=null;
int no = 0;
String music_id = "mid";
String userDir = "http://54.180.95.158:8080/server/filestorage/";
JSONObject json = new JSONObject();
try{
  String member_id = request.getParameter("member_id");
  String title = request.getParameter("title");
  String genre = request.getParameter("genre");

  userDir = userDir + member_id+"/";
  
  Class.forName("com.mysql.jdbc.Driver");
  conn = DriverManager.getConnection(url,id,pwd);
  pstmt = conn.prepareStatement("select no from music_tb order by no");
  rs = pstmt.executeQuery();
  while(rs.next()){
    if(no == rs.getInt("no"))
        no++;
    else
        break;
  }

  String folderPath = "/opt/tomcat/server/webapps/ROOT/server/filestorage/"+member_id+"/";
  music_id = music_id + Integer.toString(no);
  File f = new File(folderPath+"record_audiorecordmid.csv");
  String savedFileName = folderPath +music_id;


    if(f.exists()){
    byte[] buf = new byte[1024];
	FileInputStream fin = null;
	FileOutputStream fout = null;
    	if(!f.renameTo(new File(savedFileName+".csv"))){
    	   buf = new byte[1024];
	   try{
                  fin = new FileInputStream(folderPath+"record_audiorecordmid.csv");
                  fout = new FileOutputStream(savedFileName+".csv");
                  int read = 0;
                  while((read=fin.read(buf,0,buf.length))!=-1){
                        fout.write(buf,0,read);
                  }
                  fin.close();
                  fout.close();
		  f.delete();
	      }catch(IOException e){
	      	e.printStackTrace();
	      }
    	}	
    }

  f = new File(folderPath+"record_audiorecord.txt");
  if(f.exists()){
    byte[] buf = new byte[1024];
	FileInputStream fin = null;
	FileOutputStream fout = null;
    	if(!f.renameTo(new File(savedFileName+".txt"))){
    	   buf = new byte[1024];
	   try{
                  fin = new FileInputStream(folderPath+"record_audiorecord.txt");
                  fout = new FileOutputStream(savedFileName+".txt");
                  int read = 0;
                  while((read=fin.read(buf,0,buf.length))!=-1){
                        fout.write(buf,0,read);
                  }
                  fin.close();
                  fout.close();
		  f.delete();
	      }catch(IOException e){
	      	e.printStackTrace();
	      }
    	}	
    }

  Date now = new Date();
  SimpleDateFormat sf = new SimpleDateFormat("yyyy.MM.dd");
  String today = sf.format(now);
  
  pstmt = conn.prepareStatement("insert into music_tb values(?,?,?,?,?,?,?,?,?,?)");
  pstmt.setInt(1,no);
  pstmt.setString(2,music_id);
  pstmt.setString(3,member_id);
  pstmt.setString(4,title);
  pstmt.setString(5,genre);
  pstmt.setNull(6,Types.VARCHAR);
  pstmt.setString(7,userDir+music_id+".csv");
  pstmt.setString(8,userDir+music_id+".txt");
  pstmt.setBoolean(9,true);
  pstmt.setString(10,today);
  pstmt.executeUpdate();
  out.clear();
  json.put("result","success");

  pstmt = conn.prepareStatement("select * from music_tb where music_id = ?");
  pstmt.setString(1,music_id);
  rs = pstmt.executeQuery();

  if(rs.next()){
      json.put("music_id",rs.getString("music_id"));
      json.put("writer_id",rs.getString("writer_id"));
      json.put("title",rs.getString("title"));
      json.put("genre",rs.getString("genre"));
      json.put("music_id",rs.getString("music_id"));
      json.put("cover_img",rs.getString("cover_img_url"));
      json.put("melody_csv",rs.getString("melody_csv"));
      json.put("chord_txt",rs.getString("chord_txt"));
      json.put("scope",rs.getBoolean("scope"));
      json.put("modify_date",rs.getString("modify_date"));
  }
  
}catch(SQLException sqlException){
  out.print(sqlException);
  json.put("result","fail");
}catch(Exception exception){
  out.print(exception);
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
