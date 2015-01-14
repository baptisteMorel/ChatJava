package application;

import java.net.*;
import java.io.*;
import java.util.*;

class ForumServer {

    ServerSocket sock;
    int port = 7777;
    final static String Id = "Chatterbox";        // Chat server name
    ArrayList clients;                // Users on line (each user is represented by a ChatManager object)

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

        Socket sockC;                            // Client socket
        BufferedReader reader;                   // Reader on client socket
        PrintWriter writer;                      // Writer on client socket
        String clientIP;                         // Client machine
        String nickname = "unknown";

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

        /**
         * Send a message to the user
         * @param mess : the message to forward
         */
        public void send(String mess) {            // Send the given message to the client
            writer.println(mess);
        }

        /**
         * Find specified user with his nickname. If applicable, call the send method with the message
         * @param mess : the message to send
         * @param fromPseudo : the nickname of the user who wants to send a message
         * @param toPseudo : the nickname of the recipient user
         */
        public void sendTo(String mess, String fromPseudo, String toPseudo) {
            for (int i = 0; i < clients.size(); i++) {
                ChatManager gct = (ChatManager) clients.get(i);
                if (gct.nickname.equals(toPseudo)) {
                    gct.send("Private message from " + fromPseudo + " >" + mess);
                    break;                         //pas besoin de terminer la boucle
                }
            }
        }
        
        /**
         * For each user, call the send method with the message to broadcat
         * @param mess : The message to broadcast
         */
        public void broadcast(String mess) {    // Send the given message to all the connected users
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
        public void run() {                        // Regular activity (as a thread): treat the command received from the client
            String st;
            try {
                while ((st = reader.readLine()) != null) {
                    switch (st.charAt(0)) {
                        case '?':
                            buildNickname(st);
                            send("> Welcome " + nickname);
                            break;
                        case '!':
                            broadcast(nickname + "> " + st.substring(2));
                            break;
                        case '&':
                            displayHelp();
                            break;
                        case '%':
                            send("> Users connected : " + listClient());
                            break;
                        case '@':
                            String cleanInput = st.substring(2).trim().replaceAll(" +", " ");       //the string containing the name of the recipient and the message, cleaned.
                            int indexBeforeMessage = cleanInput.indexOf(" "); 
                            String[] splitedMessage = st.split(" ");
                            sendTo(cleanInput.substring(indexBeforeMessage+1), nickname, cleanInput.substring(0, indexBeforeMessage));
                            break;
                        default:
                            send("> I don't understand '" + st + "'");
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        private void buildNickname(String st){
            String tampNickname;

            tampNickname = st.substring(2).replaceAll("\\s", "_");

            int i = findDuplicate(tampNickname, 1);
            nickname = tampNickname;
            if (i > 1) {
                nickname += i;
            }
        }

        private int findDuplicate(String inNickname, int j) {
            for (int i = 0; i < clients.size(); i++) {
                ChatManager gct = (ChatManager) clients.get(i);
                if ((j == 1 && gct.nickname.equals(inNickname)) || (j != 1 && gct.nickname.equals(inNickname + j))) {
                    j = findDuplicate(inNickname, j + 1);
                }
            }
            return j;
        }

        /**
         * Find the nickname of each user and put them in a string
         * @return a string containing the nickname of all the connected users
         */
        public String listClient() {
            String st = "";
            for (int i = 0; i < clients.size(); i++) {
                ChatManager gct = (ChatManager) clients.get(i);
                if (gct != null) {
                    st += gct.nickname + " ";
                }
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
