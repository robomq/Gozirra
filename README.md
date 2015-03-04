# Gozirra-robomq
This is a fork of the Gozirra 0.4.1 project adding additional functionality and maintaining it actively for [STOMP](https://stomp.github.io/) specifications.  

Gozirra is a lightweight implementation of the Stomp version 1.0. The original Gozirra project can be found at <http://www.germane-software.com/software/Java/Gozirra/>. The author haven't updated it since the initial publish years ago and although Gozirra is one of most popular Java libraries for STOMP, there still could be a TODO list for it.  

Therefore, [robomq.io](http://www.robomq.io/) decided to create this fork on GitHub and maintain it. [Robomq.io](http://www.robomq.io/) provides cloud hosted Message Queue as a Service platform bundled with management interface, dashboards, analytics and SDK. STOMP is just one of the message queue protocols we support on our rental broker.  Welcome to visit our website at <http://www.robomq.io/> and full documentation at <http://robomq.readthedocs.org/>.   
  
Gozirra-robomq have fixed several bugs based on the original Gozirra and they are all listed bellow.  

1. Added one more argument "vhost" in the constructor of Client class. The interface is now  

		public Client(String server, int port, String login, String pass, String vhost)

2. Added an isSockConnected() function in Client class because the original Gozirra library fails to detect broker-down. You may want to run a loop at the end of your client program as bellow.

		while (true) {
			if (!client.isSockConnected()) {
				//do something... reconnect on connection lost
			}
			Thread.sleep(2000); //check interval must be short enough
		}

3. Added time out for connecting. Maximal number of attempts is 20 now. You will make the call whether to infinitely retry it or stop at some point.  
4. Changed the default Encoding from US-ASCII to UTF-8.  

You may read [the documentation](http://robomq.readthedocs.org/en/latest/STOMP/#java) for use guide, [robomq.io](http://www.robomq.io/) provides explicit explanation and example code for Gozirra-robomq.  

We provide both source code and jar package in this repository. Import this library in your program	`import net.ser1.stomp.*;` and compile your source code along with gozirra-robomq.jar.  