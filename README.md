# Gozirra
This is a fork of the Gozirra 0.4.1 project adding additional functionality and maintaining it actively for [STOMP](https://stomp.github.io/) specifications.  

Gozirra is a lightweight implementation of the Stomp version 1.0. The original Gozirra project can be found at <http://www.germane-software.com/software/Java/Gozirra/>. The author haven't updated it since the initial publish years ago and although Gozirra is one of most popular Java libraries for STOMP, there still could be a TODO list for it.  

Therefore, [robomq.io](http://www.robomq.io/) decided to create this fork on GitHub and maintain it. [Robomq.io](http://www.robomq.io/) provides cloud hosted Message Queue as a Service platform bundled with management interface, dashboards, analytics and SDK. STOMP is just one of the message queue protocols we support on our rental broker.  Welcome to visit our website at <http://www.robomq.io/> and full documentation at <http://robomq.readthedocs.org/>.   
  
The original Gozirra has one **limitation**. It doesn't allow you to specify the vhost to connect to. This issue has been fixed by us. We added one more argument in the constructor of Client class. It is now  

	public Client(String server, int port, String login, String pass, String vhost)

For now, we didn't change anything else in the library. You may read [the documentation](http://robomq.readthedocs.org/en/latest/user-guide/stomp/STOMP/#java) for use guide, [robomq.io](http://www.robomq.io/) provides explicit explanation and example code.  

We provide both source code and jar package in this repository. Import this library in your program	`import net.ser1.stomp.*;` and compile your source code along with gozirra.jar.  
