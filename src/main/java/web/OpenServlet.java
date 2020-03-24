package web;

import utils.Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class OpenServlet extends HttpServlet {
    private static final long serialVersionUID = -4442397463551836919L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        OutputStream out = response.getOutputStream();
        String encoding = request.getHeader("Accept-Encoding");

        if (encoding != null && encoding.contains("gzip")) {
            response.setHeader("Content-Encoding", "gzip");
            out = new GZIPOutputStream(out);
        }

        PrintWriter writer = new PrintWriter(out);
        writer.println("<html>");
        writer.println("<head>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<script type=\"text/javascript\">");

        try {
            if (request.getContentLength() < Constants.MAX_REQUEST_SIZE) {
                Map<String, String> post = parseMultipartRequest(request);
                String xml = new String(post.get("upfile").getBytes(ENCODING), "UTF-8");
                String filename = post.get("filename");

                writer.println("window.parent.openFile.setData(decodeURIComponent('" + encodeURIComponent(xml) + "'), '" + filename + "');");
            }
            else {
                error(writer, "drawingTooLarge");
            }
        }
        catch (Exception e) {
            error(writer, "invalidOrMissingFile");
        }

        writer.println("</script>");
        writer.println("</body>");
        writer.println("</html>");

        writer.flush();
        writer.close();
    }

    public static void error(PrintWriter w, String key) {
        w.println("window.parent.openFile.error(window.parent.mxResources.get('" + key + "'));");
    }

    public static String encodeURIComponent(String s) {
        String result = null;

        try {
            result = URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!").replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
        }

        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    protected static final String ENCODING = "ISO-8859-1";

    protected Map<String, String> parseMultipartRequest(HttpServletRequest request) throws IOException {
        Map<String, String> result = new Hashtable<String, String>();
        String contentType = request.getHeader("Content-Type");

        if (contentType != null && contentType.indexOf("multipart/form-data") == 0) {
            int boundaryIndex = contentType.indexOf("boundary=");
            String boundary = "--" + contentType.substring(boundaryIndex + 9).trim();

            Iterator<String> iterator = splitFormData(readStream(request.getInputStream()), boundary).iterator();

            while (iterator.hasNext()) {
                parsePart(iterator.next(), result);
            }
        }

        return result;
    }

    protected void parsePart(String part, Map<String, String> into) {
        String[] lines = part.split("\r\n");

        if (lines.length > 1) {
            String[] tokens = lines[1].split(";");

            String name = null;

            for (String tmp : tokens) {
                int index = tmp.indexOf("=");
                if (index >= 0) {
                    String key = tmp.substring(0, index).trim();
                    String value = tmp.substring(index + 2, tmp.length() - 1);
                    if (key.equals("name")) {
                        name = value;
                    } else {
                        into.put(key, value);
                    }
                }
            }

            if (name != null && lines.length > 2) {
                boolean active = false;
                StringBuffer value = new StringBuffer();
                for (int i = 2; i < lines.length; i++) {
                    if (active) {
                        value.append(lines[i]);
                    }
                    else if (!active) {
                        active = lines[i].length() == 0;
                    }
                }

                into.put(name, value.toString());
            }
        }
    }

    protected List<String> splitFormData(String formData, String boundary) {
        List<String> result = new LinkedList<String>();
        int nextBoundary = formData.indexOf(boundary);

        while (nextBoundary >= 0) {
            if (nextBoundary > 0) {
                result.add(formData.substring(0, nextBoundary));
            }

            formData = formData.substring(nextBoundary + boundary.length());
            nextBoundary = formData.indexOf(boundary);
        }

        return result;
    }

    protected String readStream(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder buffer = new StringBuilder();
            try {
                Reader in = new BufferedReader(new InputStreamReader(is, ENCODING));
                int ch;

                while ((ch = in.read()) > -1) {
                    buffer.append((char) ch);
                }
            }
            finally {
                is.close();
            }

            return buffer.toString();
        }
        else {
            return "";
        }
    }
}
