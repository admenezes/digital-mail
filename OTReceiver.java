/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coe817_project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Scanner;
import javax.crypto.Cipher;

/**
 *
 * @author Allan Truong and Alston Menezes
 */
public class OTReceiver{
    int binaryVal;
    byte[] randVal = null, encResp = null;
    Cipher cipher = null;
    RSAPublicKey publicKey = null;

    public OTReceiver(int randNum) {
        binaryVal = randNum;
    }
  
    public void encrypt(){
        SecureRandom rgen = new SecureRandom();

        byte[][] randValArray = new byte[2][];
        randValArray[0] = load("Random Value 1");
        randValArray[1] = load("Random Value 2");
        int bLen;
		if (randValArray[0].length > randValArray[1].length ){
			bLen = randValArray[0].length;
		}
		else{
			bLen = randValArray[1].length;
		}

        byte[] encrpyt = load("Encrpyt");

        // Load public key from bytes encoding
        try {
            cipher = Cipher.getInstance("RSA/ECB/NoPadding"); 
            publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encrpyt));
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (Exception e){ 
            throw new RuntimeException(e);
        }

        // Create random value randVal and encrypted response encResp.
        do {
            randVal = new byte[bLen];
            rgen.nextBytes(randVal);
            try { 
                encResp = xor(cipher.doFinal(randVal), randValArray[binaryVal]); //encrypting randVal using the xor function
                cipher.doFinal(xor(encResp, randValArray[1-binaryVal]));
            } catch (Exception e){ 
                System.out.println(e.getMessage());
            }
        }while (encResp == null);
        
        save("Encrypted Response", encResp); //store the encrypted response in a file named Encrypted Response
    }
    
    public String decrypt() {
        byte[][] decValArray = new byte[2][];
        //load decrypted values stored by Sender
        decValArray[0] = load("Decrypted Value 1"); 
        decValArray[1] = load("Decrypted Value 2");
        return byteConverter(xor(randVal, decValArray[binaryVal])); //decrypt using byteConverter method.
    }
    
    /*Saves the array passed in the paramter in a file with name fileName*/
    static void save(String fileName, byte[] data) {
        try {
            Files.write(Paths.get(fileName), data);
        }
        catch(Exception e) {
            System.err.println("Error encountered, could not write data to file: " + fileName);
            System.exit(1);
        }
    }
    
    /*Loads the file based on the name passed in the parameter*/
    static byte[] load(String fileName) {
        try {
            return Files.readAllBytes(Paths.get(fileName));
        }
        catch(Exception e) {
            System.err.println("File: " + fileName + " could not be read.");
            System.exit(1);
            return null; // stupid compiler doesn't understand System.exit
        }
    }
    
    /*SCANNER METHOD*/
    private static String read(String input){
        System.out.println(input);
        Scanner scan = new Scanner(System.in);
        String msg = scan.nextLine();
        return msg;
    }
    
    /*Waits for user input (ENTER) before proceeding*/
    private static void pause(String pauseMsg){
        System.out.println(pauseMsg);
        Scanner scan = new Scanner(System.in);
        String pauseInput = scan.nextLine();
    }

    /*XOR operation between two byte arrays*/
    static byte[] xor(byte[] x, byte[] y) {
        byte[] z = new byte[Math.min(x.length, y.length)];
        for (int i=0; i<z.length; ++i)
            z[i] = (byte)(x[i] ^ y[i]);
        return z;
    }
    
    /*String to Byte converter with length l*/
    static byte[] stringConverter(String str, int len) {
        byte[] bArray = str.getBytes(StandardCharsets.UTF_8);
        if (bArray.length > len) {
            System.err.println("Error encountered, the message was too long and could not be encoded in " + len + " bytes.");
            System.exit(1);
        }
        return Arrays.copyOf(bArray, len);
    }
 
    /*Byte to String converter*/
    static String byteConverter(byte[] x) {
        String str = new String(x, StandardCharsets.UTF_8);
        int len = str.length();
        for (; len > 0 && str.charAt(len-1) == '\0'; --len);
        return str.substring(0, len);
    }
     
    public static String randomString(int size){
        String rand = "0123456789abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"; // String rand can contain any alpha-numeric value
        StringBuilder sb = new StringBuilder(size); //building a string of length 'size'
        for (int i = 0; i < size; i++){ // generate a random number between 0 to the length of variable size
            int index = (int)(rand.length() * Math.random());
            sb.append(rand.charAt(index));// adds character at index of rand to sb
        }
        return sb.toString();
    }
    
    public static void main(String[] args){ 
        int port = 6000;
        try {  
            /*SOCKET CREATION & CONNECTION*/
            ServerSocket ss = new ServerSocket(port); //create a new socket
            Cipher c = Cipher.getInstance("DES");
            System.out.println("Awaiting connection with client...");
            Socket rec = ss.accept(); //output message when socket is connected
            System.out.println("Connection established at: " + rec.getRemoteSocketAddress()); //connecting to client
           
            /*DEFINING INPUT AND OUTPUT STREAMS*/
            DataInputStream dataIn = new DataInputStream(rec.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(rec.getOutputStream());

            String sig = read("\nWould you like to receive a message from the Sender?\n");
            dataOut.writeUTF(sig);

            if (sig.toLowerCase().equals("yes")){
                String msgSelect = read("\nEnter 0/1 to receive the first/second message. ");
                int choice = Integer.valueOf(msgSelect);
                
                if (choice != 0 && choice != 1) {
                    System.out.println("Invalid input, you did not select 0 or 1.");
                    
                    /*CLOSING THE SOCKET AND DATASTREAMS*/
                    dataIn.close();
                    dataOut.close();
                    rec.close();
                }
                else{
                    dataOut.writeUTF(msgSelect);
                    OTReceiver otr = new OTReceiver(choice);
                    otr.encrypt();
                    System.out.println("\nMessage choice was sent to the Sender. Waiting to receive selected message...");
                    pause("Press 'ENTER' after the Sender has sent their response to obtain your decrypted message.");
                    String selectedMsg = otr.decrypt();
                    System.out.println("Your decrypted message is: " + selectedMsg);
                }
                
                /*CLOSING THE SOCKET AND DATASTREAMS*/
                dataIn.close();
                dataOut.close();
                rec.close();
            }
            else if (sig.toLowerCase().equals("no")){
                System.out.println("The user does not want to receive any messages.");
                System.out.println("Thank you for using our services.");
                
                /*CLOSING THE SOCKET AND DATASTREAMS*/
                dataIn.close();
                dataOut.close();
                rec.close();
            }
            else{
                System.out.println("Invalid Input!");
                System.out.println("Thank you for using our services.");
                
                /*CLOSING THE SOCKET AND DATASTREAMS*/
                dataIn.close();
                dataOut.close();
                rec.close();
            }
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
}