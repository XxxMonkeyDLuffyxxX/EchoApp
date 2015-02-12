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
    * while the server wants to write.
     * */
    //Maps SocketChannel to a list of ByteBuffer instances
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
     * Method to create a new Selector for the Server. This is how we create a multiplexing system. The selector
     * created by this method will have an empty key set until the last line of the method where the register() method
     * is called and the server channel is added. The key is set to an OP_ACCEPT to wait for a new connection
     */
    private Selector initSelector() throws Exception{
        //Creates a new selector using the system's default provider to do so
        Selector socketSelector  = SelectorProvider.provider().openSelector();

        //Creates a new non-blocking server socket channel
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        //Binds server socket to the specified port and IP
        InetSocketAddress inetSockAddr = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(inetSockAddr);

        //Registers this server channel with the Selector and advises an interest in accepting new connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector; //Returns new Selector object
    }

    /**
     * The heart and soul of the Server program logic. Runs an infinite loop to accept connections and continuously check
     * the Selector keys for the next action to be taken. Based on event type of key, performs required action via
     * sending the key the appropriate method to complete the action(Blocking)
     */
    public void run(){
        while (true){
            try{
                this.selector.select();//Wait for an event on one of the registered channels

                Iterator selectedKeys = this.selector.selectedKeys().iterator();//Creates a key iterator object to cycle

                //Cycle through the queue of keys from the selector
                while(selectedKeys.hasNext()){
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();//Removes the current key so it is not processed again

                    //Check the event type of the current key and use the appropriate method as long as key is valid
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
     * The following blocks of code handle the operations of the server. Using the first method as a template, each
     * creates a server socket channel(only with accept method) or socketChannel to connect to the pending client's
     * socket channel. The backend automatically assigns a random port number to each of these sockets. It then
     * attempts a connection and configures the channel to non-blocking. I have included additional comments where
     * it necessitates
     */
    public void accept(SelectionKey key)throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Registers the channel with the selector and sets a request for any READ operations
        socketChannel.register(this.selector, SelectionKey.OP_READ);

        //Prints to console a status message of a connection
        System.out.println("Received an incoming connection from" + socketChannel.socket().getRemoteSocketAddress());
    }

    public void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.readBuffer.clear();//Clears the readBuffer for new incoming data from the socket channel

        int numRead; // Variable to hold data while we scan it in from the socket channel

        //Attempt to read from the socket channel
        try{
            numRead = socketChannel.read(this.readBuffer); //Scans the bytes from the buffer into numRead
        }catch(IOException ie){
            System.out.println("Well...this happened: " + ie);
            System.out.println("Client forcibly closed connection (likely connection was lost)so canceling the " +
                    "current key and closing channel");

            key.cancel(); //Delete the current key
            socketChannel.close(); //Close the socket's channel
            return;
        }

        //Client shut the connection down cleanly so readBuffer has -1 int
        if (numRead == -1) {
            System.out.println("The remote connection has cleanly shut down. The server is doing the same.");
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
