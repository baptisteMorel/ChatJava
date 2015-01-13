package application;

import java.net.*;
import java.io.*;
import java.util.*;

class ForumServer {

    ServerSocket sock;
    int port = 7777;
    final static String Id = "Chatterbox";		// Chat server name
    ArrayList clients;				// Users on line (each user is represented by a ChatManager object)

    /*	***********	*/
    /*	CONSTRUCTOR	*/
    /*	***********	*/
    ForumServer() {
        clients = new ArrayList();
        try {
            sock = new ServerSocket(port);
            System.out.println("Server is running on " + port);
        } catch (IOException e) {
            System.err.println("Launching error !!!");
            System.exit(0);
        }
    }

    /*	***************	*/
    /*	CLIENT MANAGER	*/
    /*	***************	*/
    class ChatManager extends Thread {

        Socket sockC;							// Client socket
        BufferedReader reader;                                          // Reader on client socket
        PrintWriter writer;						// Writer on client socket
        String clientIP;						// Client machine
        String nickname = "";

        ChatManager(Socket sk, String ip) {
            sockC = sk;
            clientIP = ip;
            try {
                reader = new BufferedReader(new InputStreamReader(sk.getInputStream()));
                writer = new PrintWriter(sk.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("IO error !!! on server");
            }
        }

        public void send(String mess) {			// Send the given message to the client
            writer.println(mess);
        }

        public void broadcast(String mess) {	// Send the given message to all the connected users
            synchronized (clients) {
                for (int i = 0; i < clients.size(); i++) {
                    ChatManager gct = (ChatManager) clients.get(i);
                    if (gct != null) {
                        gct.send(mess);
                    }
                }
            }
        }

        @Override
        public void run() {						// Regular activity (as a thread): treat the command received from the client
            String st;
            try {
                while ((st = reader.readLine()) != null) {
                    switch (st.charAt(0)) {
                        case '?':
                            nickname = st.substring(2);
                            send("> Welcome " + nickname);
                            break;
                        case '!':
                            broadcast(nickname+"> " + st.substring(2));
                            break;
                        case '&':
                            displayHelp();
                            break;
                        case '%':
                            send("> Users connected : "+ listClient());
                            break;
                        default:
                            send("> I don't understand '" + st + "'");
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        public String listClient(){
            String st = "";
            for (int i = 0; i < clients.size(); i++) {
                ChatManager gct = (ChatManager) clients.get(i);
                st += gct.nickname+" ";
            }
            return st;
        }

        public void close() {
            if (sockC == null) {
                return;
            }
            try {
                sockC.close();
                sockC = null;
            } catch (IOException e) {
                System.err.println("Connection closing error with " + clientIP);
            }
        }

        public void displayHelp() {
            StringBuilder help = new StringBuilder();
            help.append("HELP : \n");
            help.append("! message : broadcast message headed by sender's name\n");
            help.append("@ name message : send message to the user identified by name\n");
            help.append("? name : rename the user with this new name and let all know\n");
            help.append("& : display help on communication codes\n");
            help.append("% : display the names of all the users that are currently connected\n");
            
            send(help.toString());
        }
    }

    /*	***********	*/
    /*	LAUNCHING	*/
    /*	***********	*/
    public static void main(String[] arg) {
        ForumServer me = new ForumServer();
        me.process();
    }

    public void process() {
        try {
            while (true) {
                Socket userSock = sock.accept();
                String userName = userSock.getInetAddress().getHostName();
                ChatManager user = new ChatManager(userSock, userName);
                synchronized (clients) {
                    clients.add(user);
                    user.start();
                    user.send(userName + " : client " + clients.size() + " is on line");
                    user.send("to display the help, type \"&\"");
                    user.send("First, enter you nickname prefixed by \"? \"");
                }
            }
        } catch (IOException e) {
            System.err.println("Server error !!!");
            System.exit(0);
        }
    }
}
