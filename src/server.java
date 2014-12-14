
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * A multithreaded chat room server.
 * A client connects, gets username, joins channel.
 * Then the server goes through the given processes depending
 * on what the user wants to do
 */

public class server {
    /**
     * channel class so each channel is an object
     * Keeps track of all people and writers in each of the channels
     */
    private static class channel{
        String cname;

        channel(String n){
            this.cname = n;
        }

        public HashSet<String> names = new HashSet<String>();
        public HashSet<PrintWriter> writer = new HashSet<PrintWriter>();
    }

    /**
     * private channel class. Keeps just the info for the 2 users in the chat
     */
    private static class pchannel{
        String cname;
        String user1;
        String user2;
        pchannel(String n, String user1, String user2){
            this.cname = n;
            this.user1 = user1;
            this.user2 = user2;
        }

    }

    /**
     * List of all the channel objects
     */
    private static LinkedList<channel> channels = new LinkedList<channel>();

    /**
     * static port into the server
     */
    private static final int PORT = 9001;

    /**
     * The hashset of all the different users on the server to use
     * to check if users name selection is already in use
     */
    private static HashSet<String> allnames = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * makes it so server can send messages easily
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The IRC server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers handle each clients thread
     * and deal with all of their inputs/outputs
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }



        /**
         * All the important input/output functions
         */
        public void run() {
            try {

                // Create the streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                /**
                 * request name from user that does not exist
                 * if it is not available return error and request
                 * a new user name
                 */
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    while (name == null) {
                        out.println("SUBMITNAME");
                    }
                    while (name != null && allnames.contains(name)){
                        out.println("SUBMITERROR");
                    }
                    synchronized (allnames) {
                        //Add new username to the list of all names
                        if (!allnames.contains(name)) {
                            allnames.add(name);
                            break;
                        }
                    }
                }

                /**
                 * tell client that their username is acceptable
                 */
                out.println("NAMEACCEPTED");

                //Add new user as someone who can write
                writers.add(out);

                /**
                 * if first user then create new channel
                 * if not first, then new user must join another channel
                 */
                String tempname;
                if(channels.isEmpty()){
                    //User creates new channel
                    out.println("CREATE");
                    String temp;
                    temp = in.readLine();
                    //Add new channel to list of channels
                    channels.add(new channel(temp));
                    tempname = temp;
                    //tell all users to add channel to their list of channels
                    for(PrintWriter w: writers){
                       w.println("CHANNELS" + channels.get(0).cname);
                    }
                    channel c = channels.get(0);
                    //for the channel add the name to exist in channel
                    c.names.add(name);
                } else {
                    //tell users what channels are there
                    for(channel c : channels){
                        out.println("CHANNELS" + c.cname);
                    }
                    //tell user to choose from an existing channel
                    out.println("CHOOSE");
                    String temp = in.readLine();
                    boolean joined = false;
                    //while they havent choosen, keep asking for them to choose
                    while(joined == false) {
                        for(channel c : channels){
                            if(c.cname.equals(temp)){
                                joined = true;

                            } else {
                                joined = false;
                            }
                        }
                        if(joined == false) {
                            out.println("CHOOSE");
                        }
                    }
                    channel current = null;
                    //return channel object to get channel info
                    for(channel c : channels){
                        if(c.cname.equals(temp)){
                            current = c;
                        }
                    }
                    //Add new username to channels list of users
                    current.names.add(name);
                    tempname = temp;
                }

                //Once user has joined, then send message to channel about new user
                for(PrintWriter w : writers){
                    w.println("MESSAGE " + "[" + tempname + "]" + name + " has joined the chatroom");
                }

                //Update all user's list of people on the server
                for(PrintWriter w : writers){
                    w.println("USERS" + allnames + " ");
                }

                while(true){
                    String temp = in.readLine();
                    //If user wants to create, then do the work
                    if (temp.startsWith("CREATE")){
                        //Parse input
                        String[] s = temp.split("]");
                        String username = s[0].substring(6, s[0].length());
                        String chan = s[1];
                        //Enter new channel into lists
                        channels.add(new channel(chan));
                        for(PrintWriter w : writers){
                            w.println("CHANNELS" + chan);
                        }
                        //message users of new person on channel
                        for(PrintWriter w : writers){
                            w.println("MESSAGE " + "[" + chan + "]" + name + " has joined the chatroom");
                        }
                        //return channel
                        channel ch = null;
                        for(channel c : channels){
                            if(c.cname.equals(chan)){
                                ch = c;
                            }
                        }
                        //Add new user into channel's userlist
                        ch.names.add(username);
                    }
                    //Parse users message and relay
                    else if(temp.startsWith("[") && temp != null) {
                        String[] parts = temp.split("]");

                        String chan = parts[0].substring(1, parts[0].length());
                        String message = parts[1];
                        if(message == null || message.length()<1){
                            return;
                        }
                        for(PrintWriter writer : writers){
                           // System.err.println("[" + chan + "]" + name + ": " + message);
                            writer.println("MESSAGE " + "[" + chan + "]" + name + ": " + message);
                        }
                    }
                    //Do work to let client join new channel
                    else if(temp.startsWith("JOIN")){
                        String[] p = temp.split("]");
                        String username = p[0].substring(5, p[0].length());
                        String s = p[1];
                        for(PrintWriter w : writers){
                            w.println("MESSAGE " + "[" + s + "]" + username + " has joined the chatroom");
                        }
                        channel current = null;
                        for(channel c : channels){
                            if(c.cname.equals(s)){
                                current = c;
                                current.names.add(username);
                            }
                        }
                    }
                    //parse info, then let people in channel know user has left
                    else if(temp.startsWith("LEFT")){
                        String[] p = temp.split("]");
                        String username = p[0].substring(5, p[0].length());
                        String s = p[1];
                        for(PrintWriter w : writers){
                            w.println("MESSAGE " + "[" + s + "]" + username + " has left the chatroom");
                        }
                        channel current = null;
                        //remove user from list of people in channel
                        for(channel c : channels){
                            if(c.cname.equals(s)){
                                current = c;
                                current.names.remove(username);
                            }
                        }
                    }
                    //Return the list of people in the channel
                    else if(temp.startsWith("WHO")){
                        String[] s = temp.split("]");
                        String channel = s[1];
                        String username = s[0].substring(4, s[0].length());
                        channel chan = null;
                        for(channel c : channels){
                            if(c.cname.equals(channel)){
                                chan = c;
                            }
                        }
                        //Send info to all the clients
                        for(PrintWriter w : writers){
                            if(chan.cname != null && chan.names.toString() != null){
                                w.println("WHO" + "&" + username + "&" + chan.cname + "&" + chan.names.toString());
                            }
                        }
                    }
                    //Server only has to pass along the String Buffer
                    else if(temp.startsWith("FILE")){
                        String[] s = temp.split("&");
                        for(PrintWriter w : writers){
                            if(s[1] != null && s[2] != null && s[3] != null && s[4] != null){
                                String sender = s[1];
                                String chanl = s[2];
                                String filename = s[3];
                                String text = s[4];
                                w.println("FILE" + "&" + sender + "&" + chanl + "&" + filename + "&" + text + "&");
                            }
                        }
                    }
                    //Server sets up a private chat between two specific users
                    else if(temp.startsWith("PCHAT")){
                        String[] s = temp.split("&");
                        String user1 = s[1];
                        String user2 = s[2];
                        pchannel c = new pchannel("Private Chat", s[1], s[2]);
                        for(PrintWriter w : writers){
                            w.println("PCHATCREATED" + "&Private Chat " + user1 + " " + user2 + "&" + user1 + "&" + user2);
                        }
                    }

                }

            } catch (IOException e) {
                //System.err.println(e);

            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    allnames.remove(name);
                    for(PrintWriter w : writers){
                        w.println("USERS" + allnames + " ");
                    }
                    for(channel c : channels){
                        c.names.remove(name);
                    }
                }
                if (out != null) {
                    writers.remove(out);
                }
                //if client dies, tell everyone they left the server
                for(channel c : channels) {
                    for (PrintWriter w : writers) {
                        w.println("MESSAGE " + "[" + c.cname + "]" + name + " has left the server");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {

                }
            }
        }
    }
}