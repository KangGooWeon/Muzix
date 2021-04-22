<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.oreilly.servlet.MultipartRequest"%>
<%@ page import="com.oreilly.servlet.multipart.DefaultFileRenamePolicy"%>
<%@ page import="java.util.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.*"%>

<%
String url = "jdbc:mysql://localhost:3306/muzix";
String id = "root";
String pwd = "essence3";
PreparedStatement pstmt =null;
Statement stmt= null;
ResultSet rs= null;
Connection conn=null;

String realFolder=""; //temp val for check file path 
String saveFolder="/server/filestorage"; //Folder for save file
String encType="utf-8";
int maxSize = 30*1024*1024; //maximum of file size for now 20Mbyte


JSONObject json = new JSONObject();

ServletContext context = getServletContext();
realFolder = context.getRealPath(saveFolder);

String member_id="";
String savedFilename = "";
String music_id= "";
try{
    Class.forName("com.mysql.jdbc.Driver");
    conn = DriverManager.getConnection(url,id,pwd);
    
    //file will save when multipart is create

    MultipartRequest multi = null;
    multi = new MultipartRequest(request,realFolder,maxSize,encType,new DefaultFileRenamePolicy());
    //now file already saved

    //get parameters from file of multipart
    Enumeration params = multi.getParameterNames();

    while(params.hasMoreElements()){
        String name = (String)params.nextElement(); //get parametername
        if(name.equals("writer_id"))
            member_id = multi.getParameter(name); //get value
        else if(name.equals("music_id"))
            music_id = multi.getParameter(name);
    }

    Enumeration files = multi.getFileNames();

    while(files.hasMoreElements()){
        String name = (String)files.nextElement();
        savedFilename = multi.getFilesystemName(name);
    }
    
    File f = new File(realFolder+"/"+savedFilename);
    String savedFolder = realFolder +"/"+member_id;
    File targetDir = new File(savedFolder);

    if(!targetDir.exists()){
  	targetDir.mkdirs();
    }

    if(f.exists()){
    byte[] buf = new byte[1024];
	FileInputStream fin = null;
	FileOutputStream fout = null;
    	if(!f.renameTo(new File(savedFolder+"/"+savedFilename))){
    	   buf = new byte[1024];
	   try{
                  fin = new FileInputStream(realFolder+"/"+savedFilename);
                  fout = new FileOutputStream(savedFolder+"/"+savedFilename);
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
    json.put("result","success");
    
    pstmt = conn.prepareStatement("update music_tb set cover_img_url = ? where music_id = ?");
    pstmt.setString(1,"http://54.180.95.158:8080/server/filestorage/"+member_id+"/"+savedFilename);
    pstmt.setString(2,music_id);
    pstmt.executeUpdate();

    pstmt = conn.prepareStatement("select cover_img_url from music_tb where music_id= ?");
    pstmt.setString(1,music_id);
    rs = pstmt.executeQuery();

    if(rs.next()){
        json.put("cover_img",rs.getString("cover_img_url"));
    }
    
}catch(Exception e){
    json.put("result","fail");
}finally{
    out.print(json);
}
%>
