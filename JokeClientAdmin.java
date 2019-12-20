/*--------------------------------------------------------

1. Name / Date: Adam Brown / April 21, 2019

2. Java version used, if not the official version for the class: 11.0.1

3. Precise command-line compilation examples / instructions:

Windows: > javac JokeServer.java
macOS/Unix: $ javac JokerServer.java

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

//This class was built using code from InetClient
public class JokeClientAdmin {

    public static void main(String args[]) {
        String serverName;

        //Used to determine whether the server is running on this machine or remotely
        if(args.length < 1)
            serverName = "localhost";
        else
            serverName = args[0];

        System.out.println("Adam Brown's joke client admin, 11.0.1");
        System.out.println("Using server: " + serverName + ", port: 5050\n");

        //Used to capture user input
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        try {
            String userInput;
            //Loop used to determine whether the client has requested a mode change
            do {
                System.out.println("Enter/Return to change server mode, (quit) to end");
                userInput = in.readLine();

                //Request a mode change if "quit" has not been entered
                if(userInput.indexOf("quit") < 0)
                    requestModeChange(serverName);
            } while(userInput.indexOf("quit") < 0);
            //"quit" has been entered, alert client that program is ending
            System.out.println("Canceled by user request");
        } catch(IOException x) {
            x.printStackTrace();
        }
    }

    //Method used to create a connection with the server and request a mode change
    static void requestModeChange(String serverName) {
        //Will be used to create connection
        Socket sock;
        //Will be used to receive and organize information from the server
        BufferedReader fromServer;
        //Will be used to send information to the server
        PrintStream toServer;
        //Will hold information received from the server
        String textFromServer;

        try {
            //Establish connection
            sock = new Socket(serverName, 5050);
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());

            //Send this message to the server which indicates that a mode changed is desired
            toServer.println("toggle");
            toServer.flush();

            textFromServer = fromServer.readLine();

            if(textFromServer != null)
                System.out.println(textFromServer);

            //End this connection
            sock.close();
        } catch(IOException x) {
            System.out.println("Socket error");
            x.printStackTrace();
        }
    }

}
