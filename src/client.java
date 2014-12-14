import org.omg.SendingContext.RunTime;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog. As well as a menu bar that
 * allows users to do many functions. Two side bars. One that shows
 * all the available channels, and a list of channels user has joined
 */
public class client {
    LinkedList<String> names = new LinkedList<String>();
    //Set<String> names = new HashSet<String>();
    Set<String> channels = new HashSet<String>();
    Set<String> mychannels = new HashSet<String>();
    Set<String> buddylist = new HashSet<String>();
    String current;
    String usersInChannel = null;

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("IRC");
    JTextField textField = new JTextField(80);
    JTextArea messageArea = new JTextArea(8, 80);


    /**
     * All the buttons that are in the menu and in the side bar
     */
    JMenuBar menuBar = new JMenuBar();
    JButton show = new JButton("Show All");
    JButton create = new JButton("Create Channel");
    JButton join = new JButton("Join Channel");
    JPanel panel = new JPanel(new GridLayout(1,2));
    JPanel sidepanel = new JPanel(new GridLayout(2,1));
    JTextArea clist = new JTextArea(8, 40);
    JButton channeluser = new JButton("Show Users on Channel");
    JButton sharefile = new JButton("Upload file");
    JButton leavecurrent = new JButton("Leave Channel");
    JButton sendmult = new JButton("Send to multiple");
    JButton showcurrentchan = new JButton("Show Current");
    JButton pchat = new JButton("Start Private Chat");
    JButton disconnect = new JButton("Disconnect");
    JButton addbuddy = new JButton("Buddy List");
    JButton showbuddy = new JButton("Show Bud List");
    String username = null;

    JTextArea mylist = new JTextArea(8,40);

    public client() {

        /**
         * Set the GUI and position everything in the JFrame
         * Set the text field as uneditable until they have a name/channel
         */
        textField.setEditable(false);
        messageArea.setEditable(false);
        clist.setEditable(false);
        mylist.setEditable(false);

        menuBar.add(create);
        menuBar.add(join);
        menuBar.add(show);
        menuBar.add(channeluser);
        menuBar.add(sharefile);
        menuBar.add(leavecurrent);
        menuBar.add(sendmult);
        menuBar.add(showcurrentchan);
        menuBar.add(pchat);
        menuBar.add(addbuddy);
        menuBar.add(showbuddy);
        menuBar.setPreferredSize(new Dimension(80, 30));

        clist.append("AVAILABLE CHANNELS:" + "\n");
        for(String s : channels){
            clist.append(s);
        }

        mylist.append("MY JOINED CHANNELS: " + "\n");
        for(String s : mychannels){
            mylist.append(s + "\n");
        }

        frame.getContentPane().add(menuBar, "North");
        panel.add(new JScrollPane(messageArea));
        sidepanel.add(clist);
        sidepanel.add(mylist);
        panel.add(sidepanel);
        textField.setPreferredSize(new Dimension(80, 50));
        frame.getContentPane().add(panel);
        frame.getContentPane().add(textField, "South");
        frame.pack();
        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * When a user enters a message it is sent as soon they user
             * hits enter. Then sets the message area to empty for the
             * next message to be sent
             */
            public void actionPerformed(ActionEvent e) {
                out.println("[" + current + "]" + textField.getText());
                textField.setText("");
            }
        });

        /**
         * When the user clicks the button it displays the current channel
         * that the user is in and pops open a dialog box
         */
        showcurrentchan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, current, "Current Channel", JOptionPane.PLAIN_MESSAGE);
            }
        });

        showbuddy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, buddylist, "My Buddies", JOptionPane.PLAIN_MESSAGE);
            }
        });


        /**
         * Function for when a user wants to open a private chat with another user
         * If completed it creates a private channel for just the two users specified
         */
        pchat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * Create frame with list of all the users on the server
                 * We have a field, label, and input area, and submit button
                 */
                JFrame f = new JFrame("Start Private Chat");
                f.setVisible(true);
                int num = 0;
                for(String s : names){
                    num++;
                }

                JPanel upanel = new JPanel(new GridLayout(num,1));
                for(String s : names){
                    JLabel l = new JLabel(s);
                    upanel.add(l);
                }
                upanel.setBackground(Color.GRAY);
                JLabel l = new JLabel("Type Name of Person");
                final JTextField jtf = new JTextField();
                JButton b = new JButton("Submit");
                JPanel panel = new JPanel(new GridLayout(4,1));
                panel.add(upanel);
                panel.add(l);
                panel.add(jtf);
                panel.add(b);

                f.getContentPane().add(panel);
                f.pack();

                /**
                 * When a user hits submit button we parse out the fields and
                 * double check the user exists and that we can match the input
                 * to the user so we can tell the server to create a new
                 * private channel
                 */
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String n = jtf.getText().trim();
                        Boolean found = false;
                        for(String s : names) {
                            if (s.contains(n) && !username.equals(n)) {
                                found = true;
                            }
                        }
                        //Send to server new pchat with values for user 1 & 2
                        if(found){
                            out.println("PCHAT" + "&" + username + "&" + n + "&");
                        }
                    }
                });
            }
        });

        addbuddy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog jd = new JDialog();
                int size = names.size();
                JPanel jp = new JPanel(new GridLayout(2,1));
                JPanel butjp = new JPanel(new GridLayout(size, 1));
                JLabel jta = new JLabel("Select people you want in buddy list");
                jp.add(jta);
                final JCheckBox[] jcb = new JCheckBox[size];
                int i = 0;
                for(String s : names){
                    jcb[i] = new JCheckBox(s);
                    butjp.add(jcb[i]);
                    i++;
                }
                JButton subsend = new JButton("Create Buddy List");
                butjp.add(subsend);
                jp.add(butjp);
                jd.add(jp);
                jd.setTitle("Buddy List");
                jd.pack();
                jd.setVisible(true);

                subsend.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Set<String> s = new HashSet<String>();
                        //For each item selected put into set
                        for(JCheckBox j : jcb){
                            if(j.isSelected()){
                                String temp = j.getText().trim();
                                if(!temp.equals(username)) {
                                    s.add(temp);
                                }
                            }
                        }
                        buddylist = s;
                        jd.setVisible(false);
                    }
                });
            }
        });

        /**
         * User can send a message to multiple channels that is has joined
         * Dialog shows users channels, user selects channels to send message
         * user writes the message, then sends info to server then sends message
         * to all the channels selected
         */
        sendmult.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * Set up the dialog box with the checkboxes
                 */
                final JDialog jd = new JDialog();
                int size = mychannels.size();
                JPanel jp = new JPanel(new GridLayout(2,1));
                JPanel butjp = new JPanel(new GridLayout(size, 1));
                final JTextField jta = new JTextField();
                jp.add(jta);
                final JCheckBox[] jcb = new JCheckBox[size];
                int i = 0;
                for(String s : channels){
                    jcb[i] = new JCheckBox(s);
                    butjp.add(jcb[i]);
                    i++;
                }
                JButton subsend = new JButton("Send");
                butjp.add(subsend);
                jp.add(butjp);
                jd.add(jp);
                jd.setTitle("Send message to multiple channels");
                jd.pack();
                jd.setVisible(true);

                /**
                 * For when the user submits the message we have to
                 * check which checkboxes have been selected and sends
                 * regular message with the different channels
                 */
                subsend.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //Put all selections into Set
                        String message = jta.getText();
                        Set<String> s = new HashSet<String>();
                        //For each item selected put into set
                        for(JCheckBox j : jcb){
                            if(j.isSelected()){
                                String temp = j.getText().trim();
                                s.add(temp);
                            }
                        }
                        //For each item send message to that channel
                        for(String t : s){
                            out.println("[" + t + "]" + message);
                        }
                        jd.setVisible(false);
                    }
                });
            }
        });

        /**
         * If user wants to leave channel they hit the leave button
         * However, they must enter the new channel they would like to join
         */
        leavecurrent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(channels.size() > 1) {
                    out.println("LEFT" + "[" + username + "]" + current);
                    mychannels.remove(current);
                    messageArea.setText("");
                    frame.setName("Channel: ");
                    String newchan = chooseChannel();
                    if (!mychannels.contains(newchan)) {
                        mychannels.add(newchan);
                    }
                    current = newchan;
                    frame.setName("Channel: " + current);
                    frame.setName("Channel: " + newchan);
                    frame.setTitle("Channel: " + current);
                    updatemylist();
                } else {
                    System.err.println("Needs to be more than 1 channel to leave");
                }

            }
        });

        /**
         * Button to click when user wants to create a new channel
         * Notify channel user left, then join the newly created channel
         */
        create.addActionListener(new ActionListener() {
               @Override
            public void actionPerformed(ActionEvent e) {
                String newchannel = createChannel();
                //Notify people in channel that user is leaving
                out.println("LEFT" + "[" + username + "]" + current);
                current = newchannel;
                if(!mychannels.contains(newchannel)){
                    mychannels.add(newchannel);
                }
                //Update frame
                frame.setName("Channel: " + current);
                //Tell server to create new channel
                out.println("CREATE" + "[" + username + "]" + current);
                messageArea.setText("");
                updatemylist();
            }
        });

        /**
         * Button to click when user wants to send a .txt file to others
         * in the channel. The program converts the file into a string buffer
         * stream. Then sends the stream through server to users.
         * The users at the receiving end then compile the string buffer back
         * into a new text file that is saved in project directory
         */
        sharefile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Set the Frame
                JFrame parentFrame = new JFrame();
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
                fileChooser.setFileFilter(filter);
                fileChooser.setDialogTitle("Specify a .txt file upload");

                //File is chosen so select users selection
                int userSelection = fileChooser.showSaveDialog(parentFrame);

                //If the user picks then submit
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    String filename = file.getName();
                    String path = file.getAbsolutePath();
                    try{
                        //build string from .txt file
                        Scanner scan = new Scanner(file);
                        StringBuilder tb = new StringBuilder((int)file.length());
                        while(scan.hasNextLine()){
                            String next = scan.nextLine();
                            tb.append(next);
                        }
                        String filetext = tb.toString();
                        scan.close();
                        //Send server the file/info
                        out.println("FILE" + "&" + username + "&" + current + "&" + filename + "&" + filetext + "&");
                    } catch (FileNotFoundException f){
                        System.err.println("File not found");
                    }
                }
            }
        });

        channeluser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.println("WHO" + "[" + username + "]" + current);
            }
        });

        /**
         * Button for user to join new channel from the available list
         */
        join.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //dialog to choose channel to join
                String temp = joinChannel();
                Boolean exists = false;
                for(String s : channels){
                    if(temp.equals(s)){
                        exists = true;
                    }
                }
                //check if selection is an actual channel
                if(channels.contains(temp)) { exists = true; }
                if(channels.toString().contains(temp)){ exists = true ; }
                if(exists) {
                    //tell users user left channel
                    out.println("LEFT" + "[" + username + "]" + current);
                    messageArea.setText("");
                    current = temp;
                    //join the new channel
                    out.println("JOIN" + "[" + username + "]" + current);
                    frame.setTitle("Current: " + current);
                    //update the list of my channels
                    if (!mychannels.contains(temp)) {
                        mychannels.add(temp);
                    }
                    updatemylist();
                }
                else
                {
                    JOptionPane.showMessageDialog(frame, "You didn't enter valid channel name.");
                }
            }
        });

        /**
         * Button to click to see every single user on the server
         */
        show.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Open dialog showing all users gathered
                try {
                    JFrame f = new JFrame("Users in Chatroom");
                    f.setVisible(true);
                    int num = 0;
                    for(String s : names){
                        num++;
                    }

                    JPanel upanel = new JPanel(new GridLayout(num,1));
                    for(String s : names){
                        JLabel l = new JLabel(s);
                        upanel.add(l);
                    }

                    f.getContentPane().add(upanel);
                    f.pack();
                }
                catch(Exception ex){

                }
            }
        });
    }

    /**
     * Function to update side bar with the user's current channels
     */
    private void updatemylist(){
        mylist.setText("");
        mylist.append("MY JOINED CHANNELS: " + "\n");
        for(String s : mychannels){
            mylist.append(s + "\n");
        }
    }

    /**
     * Function Dialog that asks users what channel they want to join
     * @return channel selection
     */
    private String joinChannel(){
        return JOptionPane.showInputDialog(
                frame,
                "Choose channel to join: " + channels.toString() + " ",
                "Channel Name Selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Chatter",
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /** Prompt for a user name that is not taken
    */
    private String getUnusedName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a different screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Function to prompt user for name of new channel
     * @return name of new channel
     */
    private String createChannel() {
        return JOptionPane.showInputDialog(
                frame,
                "Create new channel:",
                "Channel Name Selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Function for user to join channel, prompt for channel choice
     * @return return channel choice
     */
    private String chooseChannel() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose channel to join: " + channels.toString() + " ",
                "Channel Name Selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        try {
            String serverAddress = getServerAddress();
            Socket socket = new Socket(serverAddress, 9001);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch(NullPointerException e){
            System.err.println("Server offline, try again later");
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }

        // Process all messages from server, according to the protocol.
        while (true) {
            String line;
            try {
                line = in.readLine();
            } catch (NullPointerException e){
                System.err.println("Server has gone offline");
                break;
            }
            if(line == null){
                System.err.println("Closing client, server offline");
                break;
            }

            if(line != null) {
                //Client connection to pick username
                if (line.startsWith("SUBMITNAME")) {
                    String s = getName();
                    username = s;
                    out.println(s);
                }
                //If username is taken prompt for new one
                else if (line.startsWith("SUBMITERROR")) {
                    String s = getUnusedName();
                    username = s;
                    out.println(s);
                }
                //Confirmation that username has been selected and is okay
                else if (line.startsWith("NAMEACCEPTED")) {
                    textField.setEditable(true);
                }
                //User is prompted to create new channel
                else if (line.startsWith("CREATE")) {
                    String newc = new String();
                    newc = createChannel();
                    current = newc;
                    out.println(newc);
                    if (!mychannels.contains(current)) {
                        mychannels.add(current);
                    }
                    updatemylist();
                }
                //Process another users message and put into window
                else if (line.startsWith("MESSAGE")) {
                    String msg = line.substring(8) + "\n";
                    String[] parts = msg.split("]");
                    String chan = parts[0].substring(1, parts[0].length());
                    if (parts[1] != null) {
                        String message = parts[1];
                        //Message area in window is updated with new user message
                        if (chan.equals(current) && message != null) {
                            messageArea.append(message);
                        }
                    }
                }
                //Update local clients list of all users on server
                else if (line.startsWith("USERS")) {
                    Set<String> temp = new HashSet<String>();
                    temp.add(line.substring(6, line.length() - 2));
                    String s = temp.toString();
                    String[] stemp = s.split(",");
                    for(String x : stemp){
                        x = x.trim();
                        if(x.startsWith("[")){
                            x = x.substring(1, x.length());
                        }
                        if(x.endsWith("]")){
                            x = x.substring(0, x.length() - 1);
                        }
                        if(!names.contains(x)) {
                            names.add(x);
                        }
                    }

                    //names = temp;
                }
                //if a new channel is created update local users list of
                //available channels
                else if (line.startsWith("CHANNELS")) {
                    clist.append(line.substring(8, line.length()) + "\n");
                    String temp = line.substring(8, line.length()) + "\n";
                    if (!channels.contains(temp)) {
                        channels.add(temp);
                    }
                    frame.setTitle("Channel: " + current);
                }
                //Update users joined channels
                else if (line.startsWith("CHOOSE")) {
                    current = chooseChannel();
                    if (!mychannels.contains(current)) {
                        mychannels.add(current);
                    }
                    updatemylist();
                    frame.setTitle("Channel: " + current);
                    out.println(current);
                }
                //To check see which users are currently in the channel
                else if (line.startsWith("WHO")) {
                    String[] s = line.split("&");
                    String user = s[1];
                    String chan = s[2];
                    String names = s[3];
                    if (chan.equals(current)) {
                        usersInChannel = null;
                        usersInChannel = s[3];
                    }
                    //If everything checks out, open new window with list
                    if (user.equals(username)) {
                        try {
                            //Update new dialog with users in the channel
                            JFrame f = new JFrame("Users in Chatroom");
                            f.setVisible(true);
                            JTextArea jta = new JTextArea();
                            jta.append(usersInChannel);
                            f.getContentPane().add(jta);
                            f.pack();
                        } catch (Exception ex) {

                        }
                    }
                }
                //If someone sent a file, parse server message
                else if (line.startsWith("FILE")) {
                    String[] s = line.split("&");
                    String ch = s[2];
                    if (ch.equals(current) && !username.equals(s[1])) {
                        //Dialog asking if user wants to download the file sent
                        JFrame f = new JFrame();
                        int response = JOptionPane.showConfirmDialog(f,
                                "You have been sent a file by: " + s[1] + "\n" +
                                        "Would you like to download: " + s[3] + " ?",
                                "Download File?",
                                JOptionPane.YES_NO_OPTION
                        );
                        //If user says yes, continue downloading
                        if (response == JOptionPane.YES_OPTION) {
                            String text = s[4];
                            File file = new File(s[3]);
                            try {
                                //Recompile File bytes back into a new .txt file
                                StringReader stringReader = new StringReader(text);
                                BufferedReader bufferedReader = new BufferedReader(stringReader);
                                FileWriter fileWriter = new FileWriter(file);
                                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                                for (String lines = bufferedReader.readLine(); lines != null; lines = bufferedReader.readLine()) {
                                    bufferedWriter.write(lines);
                                    bufferedWriter.newLine();
                                }
                                bufferedReader.close();
                                bufferedWriter.close();
                                //Open the new file in notepad
                                Runtime rt = Runtime.getRuntime();
                                Process p = rt.exec("notepad " + file);
                            } catch (IOException io) {
                                System.err.println("Error compiling file");
                            }
                        }
                    }
                }
                //Someone wants to create new private chat with us!!!
                else if (line.startsWith("PCHATCREATED")) {
                    String[] s = line.split("&");
                    String pchatname = s[1];
                    //if the chat is with us, then we set up new channel
                    if (s[2].equals(username) || s[3].equals(username)) {
                        if (!channels.contains(pchatname)) {
                            channels.add(pchatname);
                        }
                        clist.append(pchatname);
                        updatemylist();
                    }
                }
            }
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        client client = new client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}