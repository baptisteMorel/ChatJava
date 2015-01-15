package application;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

public class ForumClient extends Applet {
    Socket sock;							// Chat socket
    static int port = 7777;						// Chat port
    boolean connected;							// Connection state
    BufferedReader reader;						// Reader on socket
    PrintWriter writer;							// Writer on socket

    Frame window;							// Chatting framework
    final static String title = "CHAT AREA";                            // Forum title
    TextField myText;							// User's text zone
    int myTextDimension = 40;                                           // Maximal length for user's text
    TextArea chat;							// Chat display zone
    String message;							// Screen message
    int messBeg = 10;							// Messaging starting column
    final static String police = "Monospaced";                          // Characters type
    int size = 11;							// Characters size

    Button logInButton;							// Log in button
    Button logOutButton;						// Log out button
    Panel pan = new Panel();                                            // Buttons supporting panel

    Dimension screenSize;						// Screen dimension
    Dimension frameSize;						// Dimension of the chatting frame

    /*	*******************************	*/
    /*	DEFINING THE FORUM FRAMEWORK	*/
    /*	*******************************	*/
    @Override
    public void init() {
        window = new Frame(title);
        window.setLayout(new BorderLayout());
        chat = new TextArea();
        chat.setEditable(false);
        chat.setFont(new Font(police, Font.PLAIN, size));
        message = "Chat room opened !";
        window.add(BorderLayout.NORTH, chat);
        pan.add(logInButton = new Button("Enter"));
        logInButton.setEnabled(true);
        logInButton.requestFocus();
        logInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                login();
                logInButton.setEnabled(false);
                logOutButton.setEnabled(true);
                myText.requestFocus();
            }
        });
        pan.add(logOutButton = new Button("Exit"));
        logOutButton.setEnabled(false);
        logOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logout();
                logInButton.setEnabled(true);
                logOutButton.setEnabled(false);
                logInButton.requestFocus();
            }
        });
        pan.add(new Label("Your message:"));
        myText = new TextField(myTextDimension);
        myText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (connected) {
                    writer.println(myText.getText());
                    myText.setText("");
                }
            }
        });
        pan.add(myText);
        window.add(BorderLayout.SOUTH, pan);
        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                ForumClient.this.window.setVisible(false);
                ForumClient.this.window.dispose();
                logout();
            }
        });
        window.pack();									// Causes window to be sized to fit
        // the preferred size and layouts of its subcomponents

        // Let's place the chatting framework at the center of the screen
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frameSize = window.getSize();
        int X = (screenSize.width - frameSize.width) / 2;
        int Y = (screenSize.height - frameSize.height) / 2;
        window.setLocation(X, Y);

        window.setVisible(true);
        repaint();
    }

    /*	***********************	*/
    /*	IN AND OUT OF THE FORUM	*/
    /*	***********************	*/
    private void login() {
        if (connected) {
            return;
        }
        try {
            sock = new Socket("localhost", port);
            showStatus("CONNECTION DONE !!!");
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            writer = new PrintWriter(sock.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println(e);
        }
        connected = true;
        new Thread(new Runnable() {
            public void run() {
                String st;
                try {
                    while (connected && ((st = reader.readLine()) != null)) {
                        chat.append(st + "\n");
                    }
                } catch (IOException e) {
                    showStatus("CONNECTION LOST !!!");
                    return;
                }
            }
        }).start();
    }

    private void logout() {
        if (!connected) {
            return;
        }
        connected = false;
        try {
            if (sock != null) {
                sock.close();
            }
        } catch (IOException e) {
            System.err.println("SOME PROBLEM OCCURS !!!");
        }
    }

    @Override
    public void paint(Graphics g) {
        g.fillRect(0, 0, screenSize.width, screenSize.height);
        g.setColor(Color.black);
        g.drawString(message, messBeg, getSize().height / 2);
    }

}
