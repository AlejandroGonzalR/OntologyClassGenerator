package web;

import com.mxgraph.canvas.mxGraphicsCanvas2D;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class EditorServer {

    public static int PORT = 8080;

    static {
		mxGraphicsCanvas2D.HTML_SCALE = 0.75;
		mxGraphicsCanvas2D.HTML_UNIT = "px";
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);

        ServletContextHandler context = new ServletContextHandler(server, "/");
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
