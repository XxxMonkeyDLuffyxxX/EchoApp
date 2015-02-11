/**------------------------------------------------------------------------------
 | Author : Dontae Malone
 | Company: EAI Design Services LLC
 | Project: Simple Multiplexing TCP/IP Echo application
 | Copyright (c) 2015 EAI Design Services LLC
 ------------------------------------------------------------------------------ */
/**---------------------------------------------------------------------------------------------
 | Classification: UNCLASSIFIED
 |
 | Abstract: this application is still apart of the chattyKathy application as it will also have
 |echo functionality
 |
 \---------------------------------------------------------------------------------------------*/
/**---------------------------------------------------------------------------------------------
 VERSION HISTORY:
 1.0  - 02102015 - Initial Creation

 \---------------------------------------------------------------------------------------------**/

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.lang.*;

public class EchoServer implements Runnable{

    private InetAddress hostAddress = InetAddress.getLocalHost(); //IP Address of server using Inet GetHost method
    private ServerSocketChannel serverChannel; //A socket for the server to connect
    private Selector selector; //A Selector object for multiplexing
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192); //A Buffer object for for the channels to write to
    private int port; //port used to connect the sockets

    /**
    * Keeps track of the data that will be written to the clients because read/write asynchronously and might be reading
    * while the server wants to write.*/
    private Map<SocketChannel,byte[]> dataTracking = new HashMap<SocketChannel, byte[]>();

    /**
     * Main method. Launches thread with instance of EchoServer and moves control throughout program
     *
     */
    public static void main(String args[]){

        System.out.println("Hello and welcome to EAI Design's Echo Server application"); //Status message for log/console

        //TODO verify user data and get IP Address
        //while((port < 10000) || ())

        try{
            new Thread(new EchoServer(null, 10000)).start(); //Starts a new thread which launches an instance of EchoServer
        }catch(IOException ie) {
            System.out.println("Well...this happened: " + ie);
        }catch(Exception e){
            System.out.println("Well...this happened: " + e);
        }
   }

    /**
     * Abstraction for commonly used(and mandatory) parts of a Client/Server paradigm. trying to learn about reusable
     *code
     */
    public EchoServer(InetAddress hostAddress, int port )throws Exception{
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
    }

    /**
     * Method to create a Selector, Server socket channel, and  Inet Address socket( IP address) objects--set it up
     * correctly, then return the object to the main program
     */
    private Selector initSelector() throws Exception{
        Selector socketSelector  = SelectorProvider.provider().openSelector(); //creates a socketSelector and set to open

        this.serverChannel = ServerSocketChannel.open(); //Sets the global variable to local
        serverChannel.configureBlocking(false); //Set server channel to non-blocking (Multiplexing) locally

        InetSocketAddress inetSockAddr = new InetSocketAddress(this.hostAddress, this.port);//Sets IP Address locally
        serverChannel.socket().bind(inetSockAddr); //Binds server socket to Inet Socket object

        //Registers this server channel with the Selector. Also tells Selector that channel is set to expect connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector; //Returns new Selector object
    }

    /**
     * The heart and soul of the Server program logic. Runs an infinite loop to accept connections and continusly check
     * the Selector keys for the next action to be taken
     */
    public void run(){
        while (true){
            try{
                this.selector.select();

                Iterator selectedKeys = this.selector.selectedKeys().iterator();//Creates a key iterator object to cycle

                //Looks at the next key in the set for another action. Checks key type and sends to appropriate method
                while(selectedKeys.hasNext()){
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid()){
                        continue; //If the key IS NOT valid breaks out of loop
                    }

                    if(key.isAcceptable()){
                        this.accept(key); //Are we connecting?
                    }

                    else if(key.isReadable()){
                        this.read(key); //Are we reading?
                    }

                    else if (key.isWritable()){
                        this.write(key); //Are we writing?
                    }
                }
            }catch (Exception e){
                System.out.println("Well...this happened: " + e);
            }

        }
    }

    /**
     * Accept Method to "accept" a new connection, register it with a socket channel, and alert the selector to the
     * next action. Configures socket to non-blocking. Pretty much every action method will hav this similar framework.
     * I have included additional comments where it necessitates
     */
    public void accept(SelectionKey key)throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    public void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.readBuffer.clear();//Clears the information that maybe left in the buffer

        int numRead; //Instance variable to hold data while we scan it in from the Client

        try{
            numRead = socketChannel.read(this.readBuffer); //Scans the bytes from the channel into numRead
        }catch(IOException ie){
            System.out.println("Well...this happened: " + ie);
            System.out.println("Client forcibly closed connection (likely connection was lost)so canceling the " +
                    "current key and closing channel");

            key.cancel(); //Delete the current key
            socketChannel.close(); //Close the channel
            return;
        }

        //Checks to make sure there is data in the read buffer. Closes channel and deletes key if not
        if (numRead == -1){
            key.channel().close();
            key.cancel();
            return;
        }

        this.readBuffer.flip(); //Prepares readBuffer to take in new data

        byte[] data = new byte[1000]; //Creates a new Byte array to send the data read in

        this.readBuffer.get(data, 0, numRead); //Reads data from numRead into Byte array
        System.out.println("I got: " + new String (data)); //Prints to console/log what was received

        echo(key, data); //Sends Selection key and Byte array to echo() method to be reproduced and "echoed" back to Client
    }

    public void write(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        byte[] data = dataTracking.get(socketChannel); //Creates a Byte array that is linked to a hashmap for continuity
        dataTracking.remove(socketChannel);

        socketChannel.write(ByteBuffer.wrap(data)); //Writes data to current socket Channel via a wrapper method
    }

    public void echo(SelectionKey key, byte[] data){
        SocketChannel socketChannel = (SocketChannel) key.channel();

        dataTracking.put(socketChannel, data); //Sends the bytes in the hashmap to the Client

        key.interestOps(SelectionKey.OP_WRITE); //Sets the current key to the writing method
    }

    public void closeConnection() throws IOException{
        if (selector != null){
            try {
                selector.close();
                serverChannel.socket().close();
                serverChannel.close();
            } catch (IOException ie) {
                System.out.println("Well...this happened: " + ie);
            }
        }
    }
}
