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

import java.nio.channels.Selector;
import java.util.*;

public class EchoClient{

    public static void main(String args[]) throws Exception{
        String input;
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please type a message to echo");
        input = scanner.nextLine();

        System.out.println("You typed: " + input);

        Messenger message = new Messenger(input);

        Thread thread = new Thread(message);
        thread.start();
    }

    static class Messenger implements Runnable{
        private String message = "";
        private Selector selector;

        public Messenger(String message){
            this.message = message;
        }

        public void run(){

        }

        private void close(){

        }

        private void read
    }

}