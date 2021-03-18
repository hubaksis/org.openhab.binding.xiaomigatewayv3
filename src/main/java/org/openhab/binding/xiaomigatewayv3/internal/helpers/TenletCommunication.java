package org.openhab.binding.xiaomigatewayv3.internal.helpers;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenletCommunication {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void EnableMqttExternalServer(String host){
        Socket pingSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        logger.info("Connecting via telnet to {}", host);
        try {
            pingSocket = new Socket(host, 23);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
        
            Thread.sleep(2000);

            sendCommandReadResult(out, "admin\n", in);
            sendCommandReadResult(out, "killall mosquitto\n", in);
            sendCommandReadResult(out, "mosquitto -d\n", in);
            sendCommandReadResult(out, "killall zigbee_gw\n", in);
            sendCommandReadResult(out, "exit\n", in);                    

            out.close();
            in.close();
            pingSocket.close();    

        } catch (Exception e) {
            logger.error("Error enabling external MQTT server", e);
            return;
        }    
    }

    private void sendCommandReadResult(PrintWriter out, String command, BufferedReader in) throws InterruptedException, IOException{
        out.println(command);                        
        Thread.sleep(2000);
        printResult(in);
    };

    private void printResult(BufferedReader in) throws IOException{
        // StringBuilder res = new StringBuilder();
         String line;
        // int maxLineCount = 0;                
        
        line = in.readLine();
        logger.info("Response: {}", line);

        // try {
        //     while((line = in.readLine()) != null && maxLineCount<20) {
        //         res.append(line);
        //         maxLineCount ++;
        //     }
        //     logger.info("Response: {}", res.toString());
        // }  catch (Exception e) {
        //     logger.error("Error reading response from telnet: ", e);
        //     return;
        // }    
    }
}
