package file;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SendFileNames extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req , resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String fileType = req.getParameter("fileType");
		if(fileType==null){
			fileType = "";
		}
		
		PrintWriter out = resp.getWriter(); 
		String sjpath = this.getServletContext().getRealPath("/sj.html");
		File cfgFilesDir = new File(sjpath.substring(0 , sjpath.lastIndexOf(File.separator))+ File.separator+"files" + File.separator + fileType);
		String[] cfgFileNames = cfgFilesDir.list();
		for(int i = 0 ; i<cfgFileNames.length; i++){
			out.println(cfgFileNames[i]);
		}
		out.flush();
		out.close();
	}

	
	
	
	
}
