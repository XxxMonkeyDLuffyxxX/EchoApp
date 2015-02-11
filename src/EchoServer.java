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

    private InetAddress hostAddress = InetAddress.getLocalHost();
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private int port;

    private Map<SocketChannel,byte[]> dataTracking = new HashMap<SocketChannel, byte[]>();

    public static void main(String args[]){

        System.out.println("Hello and welcome to EAI Design's Echo Server application");

        //TODO verify user data and get IP Address
        //while((port < 10000) || ())
        try{
            new Thread(new EchoServer(null, 10000)).start();
        }catch(IOException ie) {
            System.out.println("Well...this happened: " + ie);
        }catch(Exception e){
            System.out.println("Well...this happened: " + e);
        }
   }

    public EchoServer(InetAddress hostAddress, int port )throws Exception{
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
    }



    private Selector initSelector() throws Exception{
        Selector socketSelector  = SelectorProvider.provider().openSelector();

        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress inetSockAddr = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(inetSockAddr);

        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    public void run(){
        while (true){
            try{
                this.selector.select();

                Iterator selectedKeys = this.selector.selectedKeys().iterator();

                while(selectedKeys.hasNext()){
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid()){
                        continue;
                    }

                    if(key.isAcceptable()){
                        this.accept(key);
                    }

                    else if(key.isReadable()){
                        this.read(key);
                    }

                    else if (key.isWritable()){
                        this.write(key);
                    }
                }
            }catch (Exception e){
                System.out.println("Well...this happened: " + e);
            }

        }
    }

    public void accept(SelectionKey key)throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    public void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.readBuffer.clear();

        int numRead;
        try{
            numRead = socketChannel.read(this.readBuffer);
        }catch(IOException ie){
            System.out.println("Well...this happened: " + ie);
            System.out.println("Client forcibly closed connection (likely connection was lost)so canceling the " +
                    "current key and closing channel");

            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1){
            key.channel().close();
            key.cancel();
            return;
        }

        this.readBuffer.flip();

        byte[] data = new byte[1000];

        this.readBuffer.get(data, 0, numRead);
        System.out.println("I got: " + new String (data));

        echo(key, data);
    }

    public void write(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        byte[] data = dataTracking.get(socketChannel);
        dataTracking.remove(socketChannel);

        socketChannel.write(ByteBuffer.wrap(data));
    }

    public void echo(SelectionKey key, byte[] data){
        SocketChannel socketChannel = (SocketChannel) key.channel();

        dataTracking.put(socketChannel, data);

        key.interestOps(SelectionKey.OP_WRITE);
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
