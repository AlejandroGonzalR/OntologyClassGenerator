package web;

import utils.Constants;
import utils.Utilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;

public class EchoServlet extends HttpServlet {

    Utilities util = new Utilities();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getContentLength() < Constants.MAX_REQUEST_SIZE) {
            String filename = request.getParameter("filename");
            String xml = request.getParameter("xml");

            if (filename == null) {
                filename = "export";
            }

            if (xml != null && xml.length() > 0) {
                String format = request.getParameter("format");

                if (format == null) {
                    format = "xml";
                }

                if (!filename.toLowerCase().endsWith("." + format)) {
                    filename += "." + format;
                }

                if (xml != null && xml.startsWith("%3C")) {
                    xml = URLDecoder.decode(xml, "UTF-8");
                }

                response.setContentType("text/plain");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);
                response.setStatus(HttpServletResponse.SC_OK);

                OutputStream out = response.getOutputStream();
                out.write(xml.getBytes("UTF-8"));

                // Generate ontology model
                try {
                    util.manageApp(xml);
                } catch (Exception e){
                    e.printStackTrace();
                }

                out.flush();
                out.close();
            }
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
        else {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        }
    }
}
