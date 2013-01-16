package log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetLogging extends HttpServlet{

	private static final long serialVersionUID = 1L;
	

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String log = req.getParameter("log");
		if(log!=null){
			
			String sjpath = this.getServletContext().getRealPath("/sj.html");
			String filename = req.getRemoteAddr();
			File logFile = new File(sjpath.substring(0 , sjpath.lastIndexOf(File.separator))+ File.separator+"files" + File.separator + "log" +  File.separator + filename +".log");
			FileOutputStream os = new FileOutputStream(logFile , true);
			os.write(new Date().toString().getBytes());
			os.write('\n');
			os.write(log.getBytes());
			os.write('\n');
			os.close();
		}
	}



	
}
