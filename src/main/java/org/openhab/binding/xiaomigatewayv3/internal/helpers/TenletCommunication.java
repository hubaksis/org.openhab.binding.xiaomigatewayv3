package org.openhab.binding.xiaomigatewayv3.internal.helpers;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import java.util.Base64;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


//This code is based on https://github.com/AlexxIT/XiaomiGateway3/ gateway3.py and shell.py (as of 20/03/2021).
//If something is not working - compare the files in the same folder with a newer version on GitHub.


public class TenletCommunication {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    PrintWriter out = null;
    BufferedReader in = null;

    private static List<Entry<String, String>> BT_MD5 = List.of(new SimpleEntry<String, String>("1.4.6_0012", "367bf0045d00c28f6bff8d4132b883de"),
                                                                new SimpleEntry<String, String>("1.4.6_0043", "c4fa99797438f21d0ae4a6c855b720d2"),
                                                                new SimpleEntry<String, String>("1.4.7_0115", "be4724fbc5223fcde60aff7f58ffea28"),
                                                                new SimpleEntry<String, String>("1.4.7_0160", "9290241cd9f1892d2ba84074f07391d4")                                              
                                                                );

    String DOWNLOAD = "(wget -O /data/%1$s http://master.dl.sourceforge.net/project/mgl03/%2$s/%1$s?viasf=1 && chmod +x /data/%1$s)";

    public void EnableMqttExternalServer(String host){
        Socket pingSocket = null;

        logger.info("Connecting via telnet to {}", host);
        try {
            pingSocket = new Socket(host, 23);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            pingSocket.setSoTimeout(5000);           

            Thread.sleep(2000);

            String raw="";
            readUntil("login: ");            
            //readAll();

            sendCommandReadResult("admin");            
            raw = readAll();
            if(raw.contains("Password:")){
                logger.error("Telnet with password don't supported");                
            } else{
                String version = getVersion();
                logger.info("Gateway firmware version: {}", version);

                String runningProcesses = getRunningPs();
                //logger.info("Running processes: {}", runningProcesses);

                if(!runningProcesses.contains("mosquitto -d")){
                    logger.info("Running public mosquitto");
                    sendCommandReadResult("killall mosquitto");
                    sendCommandReadResult("mosquitto -d");
                    sendCommandReadResult("killall zigbee_gw");
                }

                if(!runningProcesses.contains("ntpd")){
                    sendCommandReadResult("ntpd -l");
                }

                fixBluetooth(version, runningProcesses);


                sendCommandReadResult("exit");                    
            }

            out.close();
            in.close();
            pingSocket.close();    

        } catch (Exception e) {
            logger.error("Error enabling external MQTT server", e);
            return;
        }    
    }

    private void fixBluetooth(String version, String processes) throws InterruptedException{
        Entry<String, String> md5 = BT_MD5.stream().filter(b -> b.getKey().equals(version)).findFirst().get();
        if(md5 == null)
        {
            logger.warn("Fixed BT is not supported");
            return;
        }

        sendCommand("md5sum /data/silabs_ncp_bt");
        Thread.sleep(2000);

        //logger.info("BT: {}", md5.getValue());            

        String ms5sum = readAll();
        if(!ms5sum.contains(md5.getValue())){
            logger.info("Download fixed BT");

            sendCommand("rm /data/silabs_ncp_bt");
            Thread.sleep(2000);
            readAll();

            sendCommand(String.format(DOWNLOAD, "silabs_ncp_bt", md5.getValue()));
            Thread.sleep(120000);
            readAll();

            sendCommand("md5sum /data/silabs_ncp_bt");
            Thread.sleep(2000);
            ms5sum = readAll();
        }

        //Here BT should be downloaded from the previous method
        if(ms5sum.contains(md5.getValue()) && !processes.contains("-t log/ble")){
            logger.info("Run fixed BT");
            sendCommand("killall silabs_ncp_bt; pkill -f log/ble; /data/silabs_ncp_bt /dev/ttyS1 1 2>&1 >/dev/null | mosquitto_pub -t log/ble -l &");            
            Thread.sleep(2000);
            readAll();
        }
        
    }

    private String getVersion(){
        String versionFile = readFile("/etc/rootfs_fw_info", true);
        Pattern pattern = Pattern.compile("version=([0-9._]+)"); 
        Matcher matcher = pattern.matcher(versionFile);
        String res = "";

        while (matcher.find()){
            res = matcher.group();
            logger.info("Gateway version: {}", matcher.group());
        }

        res = res.replace("version=", "");

        return res;
    }

    private String getRunningPs() throws InterruptedException{
        sendCommand("ps -w");
        Thread.sleep(2000);

        String res = readAll();        
        return res;
    }

    private String readFile(String filePath, Boolean asBase64){
        try {
            String command = String.format("cat %s", filePath);
            if(asBase64)
                command = command + " | base64";
            
            sendCommand(command);

            Thread.sleep(2000);        
            String raw = readUntil(command);            
            raw = readUntil("#");            
            logger.info("file raw: {}", raw);
    
            if(asBase64)
            {
                byte[] decodedBytes = Base64.getDecoder().decode(raw.replace("\n", ""));
                String decodedString = new String(decodedBytes);
                return decodedString;
            } else {
                return raw;
            }            
        } catch (Exception e) {
            logger.warn("Error reading file %s", filePath, e);            
        }
        
        return null;
    }

    private String readUntil(String readUntil) {
        String res = "";
        try {            
            while(in.ready() || readUntil != null){
                String newLine = in.readLine();            
                res += newLine + "\n";
                //logger.info("Response: {}", newLine);
                if(readUntil != null && newLine.contains(readUntil)){
                    //logger.info("found");
                    break;
                }            
            }    
        } catch (Exception e) {
            if(readUntil != null)   //log exception only if we were looking for something
                logger.info("Response read timeout");
        }                
        return res;
    }

    private String readAll(){        
        return readUntil(null);
    }

    private void sendCommand(String command){
        out.println(command);      
        out.flush();                  
    }
    private void sendCommandReadResult(String command) throws InterruptedException, IOException{
        sendCommand(command);

        Thread.sleep(2000);
        
        try {            
            readAll();
        } catch (Exception e) {
            logger.info("Response read timeout");
        }
    };



    // private void printResult(BufferedReader in) throws IOException{
    //     // StringBuilder res = new StringBuilder();
    //      String line;         
         
    //     while(in.ready()){
    //         line = in.readLine();
    //         logger.info("Response: {}", line);
    //         maxLineCount++;
    //     }

    //     // try {
    //     //     while((line = in.readLine()) != null && maxLineCount<20) {
    //     //         res.append(line);
    //     //         maxLineCount ++;
    //     //     }
    //     //     logger.info("Response: {}", res.toString());
    //     // }  catch (Exception e) {
    //     //     logger.error("Error reading response from telnet: ", e);
    //     //     return;
    //     // }    
    // }
}
