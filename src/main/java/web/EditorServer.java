package web;

import com.mxgraph.canvas.mxGraphicsCanvas2D;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class EditorServer {

    public static int PORT = 8090;

    static {
		mxGraphicsCanvas2D.HTML_SCALE = 0.75;
		mxGraphicsCanvas2D.HTML_UNIT = "px";
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);

        Context context = new Context(server, "/");
        context.addServlet(new ServletHolder(new EchoServlet()), "/save");
        context.addServlet(new ServletHolder(new ExportServlet()), "/export");
        context.addServlet(new ServletHolder(new OpenServlet()), "/open");

        ResourceHandler fileHandler = new ResourceHandler();
        fileHandler.setResourceBase(".");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { fileHandler, context });
        server.setHandler(handlers);

        System.out.println("Go to http://localhost:" + PORT + "/src/main/www/index.html");

        server.start();
        server.join();
    }
}
