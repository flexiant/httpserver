import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

import com.sun.net.httpserver.*;

/* Patching 1.6 sun httpserver to fix the following bug
 * http://hg.openjdk.java.net/jdk7/jsn/jdk/rev/b0378bb50d83
 * 
 * 
 * 
 */

public class TestHttp {
 
     public static void main (String[] args) throws Exception {
         Handler handler = new Handler();
         InetSocketAddress addr = new InetSocketAddress (0);
         HttpServer server = HttpServer.create (addr, 0);
         System.out.println(server);
         HttpContext ctx = server.createContext ("/test", handler);
         ExecutorService executor = Executors.newCachedThreadPool();
         server.setExecutor (executor);
         server.start ();
 
         URL url = new URL ("http://localhost:" + server.getAddress().getPort() +"/test/foo.html");
         HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
         try {
             InputStream is = (InputStream) urlc.getInputStream();
             int c = 0;
             while (is.read()!= -1) {
                 c++ ;
             }
             System.out.println ("OK");
         } catch (IOException e) {
             System.out.println ("exception");
             error = true;
         }
         server.stop(2);
         executor.shutdown();
         if (error) {
             throw new RuntimeException ("Test failed");
         }
     }
 
     public static boolean error = false;
 
     /* this must be the same size as in ChunkedOutputStream.java
      */
     final static int CHUNK_SIZE = 4096;
 
     static class Handler implements HttpHandler {
         int invocation = 1;
         public void handle (HttpExchange t)
             throws IOException
         {
             InputStream is = t.getRequestBody();
             Headers map = t.getRequestHeaders();
             Headers rmap = t.getResponseHeaders();
             while (is.read () != -1) ;
             is.close();
             /* chunked response */
             t.sendResponseHeaders (200, 0);
             OutputStream os = t.getResponseBody();
             byte[] first = new byte [CHUNK_SIZE * 2];
             byte[] second = new byte [2];
             os.write (first);
             os.write ('x');
             os.write ('x');
             /* An index out of bounds exception will be thrown
              * below, which is caught by server, and connection
              * will be closed. resulting in IOException to client
              * - if bug present
              */
             os.write ('x');
             os.write ('x');
             os.write ('x');
             t.close();
         }
     }
 }
