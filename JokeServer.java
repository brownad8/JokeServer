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
import java.util.ArrayList;
import java.util.Random;

/*
A thread spawned by JokeServer to handle JokeClient requests. This code
taken, for the most part, from InetServer.
 */
class Worker extends Thread {
    Socket sock;

    Worker(Socket s) {
        sock = s;
    }

    @Override
    public void run() {
        PrintStream out = null;
        BufferedReader in = null;

        try {
            //Object used to receive information from client
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            //Object used to send information to client
            out = new PrintStream(sock.getOutputStream());
            try {
                //Get client name
                String name;
                name = in.readLine();
                //Assign cookie to client if necessary
                String c;
                c = in.readLine();
                int cNum = updateCookie(out, c, name);

                //Output to server console
                if(JokeServer.proverbModeOn == false)
                    System.out.println("Sending joke to " + name);
                else
                    System.out.println("Sending proverb to " + name);

                //Send joke or proverb to client
                printResponse(out, name, cNum);
            } catch (IOException x) {
                System.out.println("Server read error");
                x.printStackTrace();
            }
            //End connection with client
            sock.close();
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    /*
    Method used to assign a cookie to a client if it does not yet have one.
    Uses JokeServer's cookieCounter to determine the specific cookie to assign.
    If a cookie needs to be assigned then the addClient method is called which
    creates a new ClientState for this client and stores it.
    */
    static int updateCookie(PrintStream out, String cookie, String name) {
        int clientCookie = Integer.parseInt(cookie);
        int updatedCookie;
        int toReturn;

        if(clientCookie == Integer.MIN_VALUE) {
            updatedCookie = JokeServer.cookieCounter;
            JokeServer.cookieCounter++;
            out.println(updatedCookie);
            addClient(updatedCookie, name);
            toReturn = updatedCookie;
        } else {
            out.println(clientCookie);
            toReturn = clientCookie;
        }
        return toReturn;
    }

    /*
    Method used to create a new ClientState and store in JokeServer's clients
    ArrayList. This method is called by updateCookie for new clients.
    */
    static void addClient(int cookie, String name) {
        if(cookie == 1) {
            JokeServer.clients.add(0, new ClientState(""));
            JokeServer.clients.add(cookie, new ClientState(name));
        } else
            JokeServer.clients.add(cookie, new ClientState(name));
    }

    /*
    Method which uses the PrintStream created in the Worker thread to send responses
    to the client. Calls the client's getJoke or getProverb method depending on the
    server's current state.
     */
    static void printResponse(PrintStream out, String name, int cookie) {
        String joke = "";
        String proverb = "";

        if(JokeServer.proverbModeOn) {
            proverb = JokeServer.clients.get(cookie).getProverb();
            out.println(proverb);
        } else {
            joke = JokeServer.clients.get(cookie).getJoke();
            out.println(joke);
        }
    }
}

/*
This class is essentially the same as Worker with some small variations.
A thread spawned by AdminLooper to handle requests from JokeClientAdmins.
 */
class AdminWorker extends Thread {
    Socket sock;

    AdminWorker(Socket s) { sock = s; }

    @Override
    public void run() {
        PrintStream out = null;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintStream(sock.getOutputStream());
            try {
                //Change server mode if requested
                String modeChangeRequest;
                modeChangeRequest = in.readLine();
                if(modeChangeRequest.equals("toggle"))
                    changeMode(out);
            } catch(IOException x) {
                System.out.println("Server read error");
                x.printStackTrace();
            }
            //End connection with client
            sock.close();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }

    /*
    Method called by AdminWorker to change the server's state from joke mode to
    proverb mode or from proverb mode to joke mode. Flips JokeServer's boolean
    variable proverbModeOn and sends a corresponding message to the server console.
     */
    static void changeMode(PrintStream out) {
        if(JokeServer.proverbModeOn) {
            JokeServer.proverbModeOn = false;
            System.out.println("Server now in joke mode");
            out.println("Server now in joke mode");
        } else {
            JokeServer.proverbModeOn = true;
            System.out.println("Server now in proverb mode");
            out.println("Server now in proverb mode");
        }
    }
}

/*
This class taken from joke-threads.html. A separate server created by JokeServer
which spawns AdminWorkers.
 */
class AdminLooper implements Runnable {
    //Boolean variable that is flipped if a mode change is requested
    public static boolean adminControlSwitch = true;

    @Override
    public void run() {
        //Can be uncommented to test that AdminLooper has been created successfully
        //System.out.println("In the admin looper thread");

        int q_len = 6;
        //Distinct port used for client connections
        int port = 5050;
        Socket sock;

        try {
            //Wait for connections and spawn adminWorkers
            ServerSocket servSock = new ServerSocket(port, q_len);
            while(adminControlSwitch) {
                sock = servSock.accept();
                new AdminWorker(sock).start();
            }
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }
}

/*
This object is created and stored for each new JokeClient. It contains the
jokes & proverbs and randomizes their orders when appropriate. It also keeps
track of the responses the client has seen and determines which one to send next.
The randomization technique used here will not scale up efficiently but works
well for small sets of responses.
 */
class ClientState {
    String JA; String JB; String JC; String JD;
    String PA; String PB; String PC; String PD;

    //Will hold the responses in randomized orders
    String[] jokes = new String[5];
    String[] proverbs = new String[5];

    //Will be used to keep track of which responses have been seen by the client
    Boolean[] jokesSeen = new Boolean[5];
    Boolean[] proverbsSeen = new Boolean[5];

    //Will hold the client's name
    String name = "";

    public ClientState(String name) {

        //Establish the jokes and proverbs

        //From the movie What We do in the Shadows
        this.JA = "JA " + name + ": We're werewolves not swearwolves";
        //From comedian Mitch Hedberg
        this.JB = "JB " + name + ": The easiest way to collect stamps is to not mail anything";
        //From the sitcom Seinfeld
        this.JC = "JC " + name + ": I love a good nap. Sometimes it's the only thing getting me out of bed in the morning";
        //From the comedian Steven Wright
        this.JD = "JD " + name + ": Everywhere is within walking distance if you have the time";

        this.PA = "PA " + name + ": You can't put the toothpaste back in the tube";
        this.PB = "PB " + name + ": A watched pot never boils";
        this.PC = "PC " + name + ": Fortune favors the bold";
        this.PD = "PD " + name + ": All publicity is good publicity";

        //Assists with randomizing
        String[] jokeInitializer = {"", JA, JB, JC, JD};
        String[] proverbInitializer = {"", PA, PB, PC, PD};

        this.jokes[0] = "";
        this.proverbs[0] = "";

        Random R = new Random();
        Boolean availableIndexFound = false;

        ArrayList<Integer> initialJokeOrder = new ArrayList<Integer>();
        initialJokeOrder.add(0, 0);

        //Randomize initial joke order
        for(int i=1; i<jokes.length; i++) {
            while(availableIndexFound == false) {
                int ji = R.nextInt(5);
                if(!initialJokeOrder.contains(ji)) {
                    this.jokes[ji] = jokeInitializer[i];
                    initialJokeOrder.add(ji);
                    availableIndexFound = true;
                }
            }
            availableIndexFound = false;
        }

        ArrayList<Integer> initialProverbOrder = new ArrayList<Integer>();
        initialProverbOrder.add(0, 0);

        //Randomize initial proverb order
        for(int i=1; i<proverbs.length; i++) {
            while(availableIndexFound == false) {
                int pi = R.nextInt(5);
                if(!initialProverbOrder.contains(pi)) {
                    this.proverbs[pi] = proverbInitializer[i];
                    initialProverbOrder.add(pi);
                    availableIndexFound = true;
                }
            }
            availableIndexFound = false;
        }

        for(int i=0; i<jokesSeen.length; i++)
            jokesSeen[i] = false;

        for(int i=0; i<proverbsSeen.length; i++)
            proverbsSeen[i] = false;

        this.name = name;
    }

    //Determine which joke to send and re-randomize the order if necessary
    public String getJoke() {
        String joke = "";
        String[] jokeInitializer = {"", JA, JB, JC, JD};

        if(jokesSeen[4] == true) {
            for (int i = 1; i < jokesSeen.length; i++)
                jokesSeen[i] = false;

            Random r = new Random();
            ArrayList<Integer> newJokeOrder = new ArrayList<Integer>();
            newJokeOrder.add(0, 0);
            Boolean availableIndexFound = false;

            for(int i=1; i<jokes.length; i++) {
                while(availableIndexFound == false) {
                    int ji = r.nextInt(5);
                    if(!newJokeOrder.contains(ji)) {
                        jokes[ji] = jokeInitializer[i];
                        newJokeOrder.add(ji);
                        availableIndexFound = true;
                    }
                }
                availableIndexFound = false;
            }
        }

        for(int i=1; i<jokesSeen.length; i++) {
            if(i == 4)
                //Server output indicating the end of a joke cycle
                System.out.println("JOKE CYCLE COMPLETED");
            if (jokesSeen[i] == false) {
                joke = jokes[i];
                jokesSeen[i] = true;
                break;
            }
        }
        return joke;
    }

    //Determine which proverb to send and re-randomize if necessary
    public String getProverb() {
        String proverb = "";
        String[] proverbInitializer = {"", PA, PB, PC, PD};

        if(proverbsSeen[4] == true) {
            for (int i = 1; i < proverbsSeen.length; i++)
                proverbsSeen[i] = false;

            Random r = new Random();
            ArrayList<Integer> newProverbOrder = new ArrayList<Integer>();
            newProverbOrder.add(0, 0);
            Boolean availableIndexFound = false;

            for(int i=1; i<proverbs.length; i++) {
                while(availableIndexFound == false) {
                    int pi = r.nextInt(5);
                    if(!newProverbOrder.contains(pi)) {
                        proverbs[pi] = proverbInitializer[i];
                        newProverbOrder.add(pi);
                        availableIndexFound = true;
                    }
                }
                availableIndexFound = false;
            }
        }     

        for(int i=1; i<proverbsSeen.length; i++) {
            if(i == 4)
                //Server output indicating the end of a proverb cycle
                System.out.println("PROVERB CYCLE COMPLETED");
            if(proverbsSeen[i] == false) {
                proverb = proverbs[i];
                proverbsSeen[i] = true;
                break;
            }
        }
        return proverb;
    }
    
}

/*
Server that creates AdminLooper and Worker threads. Much of this code taken
from InetServer.
 */
public class JokeServer {

    //Used to toggle between joke and proverb modes
    public static boolean proverbModeOn = false;

    //Used to assign cookies to clients
    static int cookieCounter = 1;

    //Used to hold the state of JokeClients
    static ArrayList<ClientState> clients = new ArrayList<ClientState>();

    public static void main(String a[]) throws IOException {
        int q_len = 6;
        //Distinct port for client connections
        int port = 4545;
        Socket sock;

        /*The following three lines were taken from joke-threads.html.
        Create a server to handle JokeClientAdmins and run it.
         */
        AdminLooper AL = new AdminLooper();
        Thread t = new Thread(AL);
        t.start();

        ServerSocket servSock = new ServerSocket(port, q_len);

        System.out.println("Adam Brown's joke server 11.0.1 starting up, listening at port 4545.\n");

        //Wait for client connections
        while(true) {
            sock = servSock.accept();
            new Worker(sock).start();
        }
    }
}