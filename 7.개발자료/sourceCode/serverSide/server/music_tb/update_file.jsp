<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.oreilly.servlet.MultipartRequest"%>
<%@ page import="com.oreilly.servlet.multipart.DefaultFileRenamePolicy"%>
<%@ page import="java.util.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.*"%>
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

String realFolder=""; //temp val for check file path 
String saveFolder="/server/filestorage"; //Folder for save file
String encType="utf-8";
int maxSize = 20*1024*1024; //maximum of file size for now 20Mbyte


JSONObject json = new JSONObject();

ServletContext context = getServletContext();
realFolder = context.getRealPath(saveFolder);

String member_id="";
String music_id = "";
String savedFilename = "";
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
    
    Date now = new Date();
    SimpleDateFormat sf = new SimpleDateFormat("yyyy.MM.dd");
    String today = sf.format(now);

    pstmt = conn.prepareStatement("update music_tb set modify_date = ? where music_id = ?");
    pstmt.setString(1,today);
    pstmt.setString(2,music_id);
    json.put("result","success");
    
}catch(Exception e){
    out.print(e);
    json.put("result","fail");
}finally{
    out.print(json);
}
%>
