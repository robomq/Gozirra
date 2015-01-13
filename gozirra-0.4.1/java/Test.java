import junit.framework.*;
import net.ser1.stomp.Server;
import net.ser1.stomp.Client;
import net.ser1.stomp.Stomp;
import net.ser1.stomp.Listener;
import net.ser1.stomp.Authenticator;
import java.util.*;
import java.io.IOException;
import javax.security.auth.login.LoginException;

// Tests to add:
// * CONNECTED
// * RECEIPT
// * TRANSACTION
public class Test extends TestCase {
  public Test( String name ) {
    super(name);
  }
  
  private Client mkClient() throws Exception {
    Client c = new Client( "localhost", 61626, "ser", "ser" );
    return c;
  }
  private Server mkServer() throws IOException, InterruptedException {
    Server s =new Server( 61626 );
    Thread.yield();            // Give the server a chance to warm up
    Thread.yield();
    Thread.yield();
    return s;
  }
  private Map subscribe( Stomp c, String channel ) {
    final HashMap res = new HashMap();
    StringBuffer buff = new StringBuffer();
    res.put( "MESSAGE", buff );
    c.subscribe( channel, new Listener() {
      public void message( Map h, String b ) {
        ((StringBuffer)res.get("MESSAGE")).append(b);
      }
    });
    return res;
  }
  private void transaction( Stomp sc, Stomp c ) throws Exception {
    Map res = subscribe( c, "a" );
    Thread.sleep(200);

    sc.begin();
    sc.send("a", "1");
    sc.send("a", "2");
    sc.commit();

    Thread.sleep( 500 );
    assertEquals( "12", res.get("MESSAGE").toString() );

    sc.begin();
    sc.send("a", "x");
    sc.send("a", "y");
    sc.commit();

    Thread.sleep( 500 );
    assertEquals( "12xy", res.get("MESSAGE").toString() );

    sc.begin();
    sc.send("a", "a");
    sc.send("a", "b");
    sc.abort();

    Thread.sleep( 500 );
    assertEquals( "12xy", res.get("MESSAGE").toString() );
  }
  private void subscribe( Stomp sc, Stomp c ) throws Exception {
    Map res = subscribe( c, "/a" );
    // Thread 1 sends the subscription; thread 2 will be sending the
    // message.  We have to make sure thread 1 delivers its message before
    // thread 2 sends its message, or else the server will receive them
    // out of order, and the test will fail.
    Thread.sleep(200);

    sc.send( "/a", "x" );
    sc.send( "/a", "y" );

    Thread.sleep( 500 );
    assertEquals( "xy", res.get("MESSAGE").toString() );
  }
  private void multiClient( Stomp sc, Stomp c1, Stomp c2 ) throws Exception {
    Map res1 = subscribe( c1, "/a" );
    Map res2 = subscribe( c2, "/a" );
    Thread.sleep(200);

    sc.send( "/a", "x" );
    sc.send( "/a", "y" );

    Thread.sleep( 500 );
    assertEquals( "xy", res1.get("MESSAGE").toString() );
    assertEquals( "xy", res2.get("MESSAGE").toString() );
  }
  private void multiChannel( Stomp sc, Stomp c1 ) throws Exception {
    Map res1 = subscribe( c1, "/a" );
    Map res2 = subscribe( c1, "/b" );
    Thread.sleep(200);

    sc.send( "/a", "x" );
    sc.send( "/a", "y" );
    sc.send( "/b", "1" );
    sc.send( "/b", "2" );

    Thread.sleep( 500 );
    assertEquals( "xy", res1.get("MESSAGE").toString() );
    assertEquals( "12", res2.get("MESSAGE").toString() );
  }
  private void multiClientMultiChannel( Stomp sc, Stomp c1, Stomp c2 ) 
    throws Exception {
    Map res1 = subscribe( c1, "/a" );
    Map res2 = subscribe( c1, "/b" );
    Map res3 = subscribe( c2, "/a" );
    Map res4 = subscribe( c2, "/c" );
    Thread.sleep(200);

    sc.send( "/a", "x" );
    sc.send( "/a", "y" );
    sc.send( "/b", "1" );
    sc.send( "/b", "2" );

    Thread.sleep( 500 );
    assertEquals( "xy", res1.get("MESSAGE").toString() );
    assertEquals( "12", res2.get("MESSAGE").toString() );
    assertEquals( "xy", res3.get("MESSAGE").toString() );
    assertEquals( "", res4.get("MESSAGE").toString() );
  }
  private void selfSend( Stomp c ) throws Exception {
    Map res1 = subscribe( c, "a" );
    Map res2 = subscribe( c, "b" );
    Thread.sleep(200);

    c.send("a", "x");
    c.send("b", "y");
    Thread.sleep(500);

    assertEquals( "x", res1.get("MESSAGE").toString() );
    assertEquals( "y", res2.get("MESSAGE").toString() );
  }
  private void receipt( Stomp sc, Stomp c ) throws Exception {
    Map res = subscribe( c, "a" );
    Thread.sleep(200);

    HashMap headers = new HashMap();
    headers.put( "receipt", "my-message" );
    sc.send( "a", "Hello world", headers );
    Thread.sleep(500);

    assertTrue( sc.hasReceipt( "my-message" ) );

    headers.put( "receipt", "message-id-123" );
    sc.send( "a", "Foodydoody", headers );

    assertTrue( sc.waitOnReceipt( "message-id-123", 1000 ) );
  }
  private void unsubscribe( Stomp sc, Stomp c ) throws Exception {
    Map res = subscribe( c, "a" );
    StringBuffer buff = (StringBuffer)res.get("MESSAGE");
    Thread.sleep(200);

    sc.send( "a", "Hello world" );
    Thread.sleep(500);
    assertEquals( "Hello world", buff.toString() );

    c.unsubscribe( "a" );
    Thread.sleep(500);
    sc.send( "a", "abc" );
    Thread.sleep(500);
    assertEquals( "Hello world", buff.toString() );
  }
  private class TestAuth implements Authenticator {
    String _token = "lalala";
    public Object connect( String user, String pass ) throws LoginException {
      // Allow ser, ser
      if (user.equals("ser")) {
        if (pass.equals("ser")) return _token;
        throw new LoginException( "Bad password for ser" );
      }
      throw new LoginException( "Unknown user "+user );
    }
    public boolean authorizeSend( Object token, String channel ) {
      // Allow ser, ser to send to b, but not to a
      if (channel.equals("b") && token == _token) return true;
      return false;
    }
    public boolean authorizeSubscribe( Object token, String channel ) {
      // Allow ser, ser to subscribe to a
      if (channel.equals("a") && token == _token) return true;
      return false;
    }
  }



  /*
  public void testServerDisconnect() {
    try {
      Server s = mkServer();
      Client c = mkClient();

      s.stop();
      Thread.yield();
      assertFalse( c.isConnected() );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  */






  public void testNet() {
    Server s = null;
    Stomp sc = null;
    Client c = null;
    try {
      s = mkServer();
      sc = s.getClient();
      c = mkClient();

      subscribe( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testNetSelfSend() {
    Server s = null;
    Stomp c = null;
    try {
      s = mkServer();
      c = mkClient();

      selfSend( c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      c.disconnect();
      s.stop();
    }
  }


  public void testNetMultClient() {
    Server s =  null; 
    Stomp sc =  null;
    Client c1 = null;
    Client c2 = null;
    try {
      s = mkServer();
      sc = s.getClient();
      c1 = mkClient();
      c2 = mkClient();

      multiClient( sc, c1, c2 );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c1.disconnect();
      c2.disconnect();
      s.stop();
    }
  }


  public void testNetMultChannel() {
    Server s  = null; 
    Stomp sc  = null; 
    Client c1 = null;
    try {
      s = mkServer();
      sc = s.getClient();
      c1 = mkClient();

      multiChannel( sc, c1 );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c1.disconnect();
      s.stop();
    }
  }


  public void testNetMultChannelMultClient() {
    Server s =  null; 
    Stomp sc =  null;
    Client c1 = null;
    Client c2 = null;
    try {
      s = mkServer();
      sc = s.getClient();
      c1 = mkClient();
      c2 = mkClient();

      multiClientMultiChannel( sc, c1, c2 );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c1.disconnect();
      c2.disconnect();
      s.stop();
    }
  }


  public void testNetUnsubscribe() {
    Server s = null; 
    Stomp sc = null;
    Stomp c =  null;
    try {
      s = mkServer();
      sc = s.getClient();
      c = mkClient();

      unsubscribe( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testNetTransaction() {
    Server s = null; 
    Stomp sc = null; 
    Client c = null; 
    try {
      s = mkServer();
      sc = s.getClient();
      c = mkClient();

      transaction( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testNetReceipt() {
    Server s = null; 
    Stomp sc = null;
    Stomp c =  null;
    try {
      s = mkServer();
      sc = s.getClient();
      c = mkClient();

      receipt( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testNetAuthenticationPass() {
    Server s = null;
    Stomp c =  null;
    try {
      s = new Server( 61626, new TestAuth() );
      Thread.yield();            // Give the server a chance to warm up
      Thread.yield();

      c = new Client( "localhost", 61626, "ser", "ser" );
      c.subscribe( "a", new Listener() {
        public void message( Map h, String b ) { }
      });
      assertNull( c.nextError() );
      c.send( "b", "foo" );
      assertNull( c.nextError() );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      c.disconnect();
      s.stop();
    }
  }


  public void testNetAuthenticationFail() {
    Server s = null;
    Stomp c =  null;
    try {
      s =new Server( 61626, new TestAuth() );
      Thread.yield();            // Give the server a chance to warm up
      Thread.yield();

      try {
        c = new Client( "localhost", 61626, "ser", "foo" );
        c.disconnect();
        fail( "Should have gotten an authentication error");
      } catch (LoginException e) {
      } finally {
        if (c != null) c.disconnect();
      }

      c = new Client( "localhost", 61626, "ser", "ser" );

      c.send( "a", "foo" );
      Thread.sleep(500);
      assertNotNull( c.nextError() );

      c.subscribe("b",new Listener(){public void message(Map h,String b){}});
      Thread.sleep(500);
      assertNotNull( c.nextError() );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      c.disconnect();
      s.stop();
    }
  }




  public void testLocal() {
    Server s = null;
    Stomp sc = null;
    Stomp c = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c = s.getClient();

      subscribe( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testLocalSelfSend() {
    Server s = null;
    Stomp c = null;
    try {
      s = new Server();
      Thread.yield();
      c = s.getClient();

      selfSend( c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      c.disconnect();
      s.stop();
    }
  }


  public void testLocalMultClient() {
    Server s = null;
    Stomp sc = null;
    Stomp c1 = null;
    Stomp c2 = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c1 = s.getClient();
      c2 = s.getClient();

      multiClient( sc, c1, c2 );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c1.disconnect();
      c2.disconnect();
      s.stop();
    }
  }


  public void testLocalMultChannel() {
    Server s = null;
    Stomp sc = null;
    Stomp c = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c = s.getClient();

      multiChannel( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testLocalMultChannelMultClient() {
    Server s = null;
    Stomp sc = null;
    Stomp c1 = null;
    Stomp c2 = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c1 = s.getClient();
      c2 = s.getClient();

      multiClientMultiChannel( sc, c1, c2 );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c1.disconnect();
      c2.disconnect();
      s.stop();
    }
  }


  public void testLocalUnsubscribe() {
    Server s = null;
    Stomp sc = null;
    Stomp c = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c = s.getClient();

      unsubscribe( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testLocalTransaction() {
    Server s = null;
    Stomp sc = null;
    Stomp c = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c = s.getClient();

      transaction( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testLocalReceipt() {
    Server s = null;
    Stomp sc = null;
    Stomp c = null;
    try {
      s = new Server();
      Thread.yield();
      sc = s.getClient();
      c = s.getClient();

      receipt( sc, c );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      sc.disconnect();
      c.disconnect();
      s.stop();
    }
  }


  public void testLocalAuthenticationPass() {
    Server s = null;
    Stomp c =  null;
    try {
      s =new Server( 61626, new TestAuth() );
      Thread.yield();            // Give the server a chance to warm up
      Thread.yield();

      c = s.getClient();
      c.subscribe( "a", new Listener() {
        public void message( Map h, String b ) { }
      });
      assertNull( c.nextError() );
      c.send("a", "foo");
      assertNull( c.nextError() );
    } catch (Exception e) {
      e.printStackTrace(System.err);
      fail( e.getMessage() );
    } finally {
      c.disconnect();
      s.stop();
    }
  }

  class SThread extends Thread {
    Stomp _c; Listener _l;
    public SThread( Stomp c, Listener l ) {
      super();
      _c = c; _l = l;
    }
    public void run() {
      for (int i=0; i<20; i++) {
        try {Thread.sleep( (long)Math.random()*5000 );} catch (Exception e) {}
        //System.err.println("Subscribing "+_l);
        _c.subscribe("a", _l);
      }
    }
  }
  class EThread extends Thread {
    Stomp _c; Listener _l;
    public EThread( Stomp c, Listener l ) {
      super();
      _c = c; _l = l;
    }
    public void run() {
      for (int i=0; i<20; i++) {
        try {Thread.sleep( (long)Math.random()*5000 );} catch (Exception e) {}
        //System.err.println("Unsubscribing "+_l);
        _c.unsubscribe("a", _l);
      }
    }
  }
  
  public void testNetVigorously() {
    try {
      Server s = new Server(61626);
      final Stomp c1 = s.getClient();
      Thread.sleep(500);
      Stomp c = new Client("localhost",61626,"","");

      Listener x = new Listener() {
        public void message( Map h, String b ) {
          //System.err.println("x :: "+b);
        }
      };
      Listener y = new Listener() {
        public void message( Map h, String b ) {
          //System.err.println("y :: "+b);
        }
      };

      Thread xs = new SThread(c,x);
      Thread xe = new EThread(c,x);
      Thread ys = new SThread(c,y);
      Thread ye = new EThread(c,y);
      Thread m = new Thread() {
        public void run() {
          for (int i=0;i<20;i++) {
            try {Thread.sleep( (long)Math.random()*5000 );} catch (Exception e) {}
            c1.send( "a", "test"+i);
          }
        }
      };
      xs.start();
      xe.start();
      ys.start();
      ye.start();
      m.start();
    } catch (Exception e){ 
      e.printStackTrace();
    }
  }
  public void testVigorously() {
    Server s = new Server();
    final Stomp c = s.getClient();

    Listener x = new Listener() {
      public void message( Map h, String b ) {
        //System.err.println("x :: "+b);
      }
    };
    Listener y = new Listener() {
      public void message( Map h, String b ) {
        //System.err.println("y :: "+b);
      }
    };

    Thread xs = new SThread(c,x);
    Thread xe = new EThread(c,x);
    Thread ys = new SThread(c,y);
    Thread ye = new EThread(c,y);
    Thread m = new Thread() {
      public void run() {
        for (int i=0;i<20;i++) {
          try {Thread.sleep( (long)Math.random()*1000 );} catch (Exception e) {}
          c.send( "a", "test"+i);
        }
      }
    };
    xs.start();
    xe.start();
    ys.start();
    ye.start();
    m.start();
  }



  public static void main(String args[]) { 
    TestSuite suite = null;
    if (args.length > 0) {
      suite = new TestSuite();
      for (int i=0;i<args.length;i++) {
        suite.addTest( new Test(args[i]) );
      }
    } else {
      suite = new TestSuite(Test.class);
    }
    junit.textui.TestRunner.run(suite);
  }
}
