package parser;

import cc.parser.JavaParserArg;
import cc.parser.JavaParserService;
import cc.parser.JavaParsingResult;
import com.google.common.primitives.Ints;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;

import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Implementation of the Java parsing service. This is the entry point of
 * Java source and Jar parsing.
 */
public class JavaParserServiceImpl implements JavaParserService.Iface {

  private final int initPort;
  private TServer server;
  private boolean isStarted;

  public JavaParserServiceImpl(final int initPort) {
    this.initPort = initPort;
    this.server = null;
    this.isStarted = false;
  }

  void start() {
    try {
      // starting Thrift server
      TServerSocket serverTransport = new TServerSocket(0);

      JavaParserService.Processor processor = new JavaParserService.Processor(
              new JavaParserServiceImpl(initPort));

      server = new TSimpleServer(new TSimpleServer.Args(serverTransport).
              processor(processor));

      int port = serverTransport.getServerSocket().getLocalPort();
      System.out.println("JavaParser: Starting server on port " + port + "...");

      // send back the obtained port number
      System.out.println("JavaParser: Socker port: " + initPort);
      Socket initSocket = new Socket("127.0.0.1", initPort);
      DataOutputStream out = new DataOutputStream(initSocket.getOutputStream());

      out.writeInt(port);
      out.flush();

      out.close();
      initSocket.close();

      isStarted = true;
    } catch (Exception ex) {
      System.out.println("JavaParser: " + ex.getMessage());
      ex.printStackTrace();
    }
  }

  void run() {
    if (!isStarted) {
      start();
    }

    server.serve();
  }

  public JavaParsingResult parse(JavaParserArg arg) {
    Parser parser = new Parser();
    return parser.parse(arg);
  }

  public void stop() {
    // server.stop();
    isStarted = false;

    System.exit(0);
  }

  public static void main(String[] args) {
    if (args.length < 1 || null == Ints.tryParse(args[0])) {
      System.out.println("JavaParser: Port number must be given for initializer communication.");
      System.exit(1);
    }

    JavaParserServiceImpl jpsi = new JavaParserServiceImpl(Integer.parseInt(args[0]));
    jpsi.run();
  }
}
