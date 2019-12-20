/*--------------------------------------------------------

1. Name / Date: Adam Brown / April 21, 2019

2. Java version used, if not the official version for the class: 11.0.1

3. Precise command-line compilation examples / instructions:

Windows: > javac JokeServer.java
macOS/Unix: $ javac JokeServer.java

4. Precise examples / instructions to run this program:

In separate shell windows:

Windows
> java JokeServer
> java JokeClient
> java JokeClientAdmin

macOS/Unix
$ java JokeServer
$ java JokeClient
$ java JokeClientAdmin

JokeClient and JokeClientAdmin use localhost by default.
A specific server location can be specified by adding a
command line argument.

$ JokeClient localhost
$ JokeClientAdmin localhost

An IP address can be given in place of localhost however this code
has been tested on one machine only.

Consoles will display user command options.

5. List of files needed for running the program.

 a. JokeServer.java
 b. JokeClient.java
 c. JokeClientAdmin.java

5. Notes:

Second server functionality was not implemented.

----------------------------------------------------------*/

import java.io.*;
import java.net.*;

//This class built using code from InetClient
public class JokeClient {

    /*
    Initializes cookie to a value that will indicate that this is a new
    client. Will hold a unique number once one is assigned.
     */
    static int cookie = Integer.MIN_VALUE;

    public static void main(String args[]) {
        String serverName;

        //Used to determine whether the server is running on this machine or remotely
        if(args.length < 1)
            serverName = "localhost";
        else
            serverName = args[0];

        System.out.println("Adam Brown's joke client, 11.0.1");
        System.out.println("Using server: " + serverName + ", port: 4545\n");
        //Can be uncommented to reveal cookie to client console
        //System.out.println("This client's cookie: " + cookie);

        //Used to capture user input
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        //Request client name once only
        System.out.print("Please enter your name: ");

        try {
            String userName = in.readLine();
            String name;

            //Loop used to determine whether a joke or proverb has been requested
            do {
                System.out.println();
                System.out.println("Enter/Return for joke or proverb, (quit) to end");
                System.out.flush();
                name = in.readLine();

                //Request a joke or proverb if "quit" has not been entered by client
                if (name.indexOf("quit") < 0) {
                    getResponse(userName, serverName);
                    //Can be uncommented to reveal cookie to client console
                    //System.out.println("This client's cookie: " + cookie);
                }
            } while(name.indexOf("quit")<0);
            //"quit" has been entered, alert client that program is ending
            System.out.println("Cancelled by user request");
        } catch(IOException x) {
            x.printStackTrace();
        }
    }

    //Method used to create a connection with the server and request a response
    static void getResponse(String name, String serverName) {
        //Will be used to create connection
        Socket sock;
        //Will be used to receive and organize information from the server
        BufferedReader fromServer;
        //Will be used to send information to the server
        PrintStream toServer;
        //Will hold information received from the server
        String textFromServer;

        try {
            //Connect to server
            sock = new Socket(serverName, 4545);
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());

            //Send client name and cookie to server
            toServer.println(name);
            toServer.println(cookie);
            toServer.flush();

            //Assign the server generated cookie to this client
            textFromServer = fromServer.readLine();
            if(textFromServer != null)
                cookie = Integer.parseInt(textFromServer);

            //Print the joke or proverb received from the server to the client console
            textFromServer = fromServer.readLine();
            if(textFromServer != null)
                System.out.println(textFromServer);

            //End this connection
            sock.close();
        } catch(IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }

}
