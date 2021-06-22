/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coe817_project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.Cipher;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 *
 * @author Allan Truong and Alston Menezes
 */

public class OTSender{
    // Variable declaration
    int modLength = 1024;
    byte[] msg0 = null, msg1 = null, randVal1 = null, randVal2 = null, decVal1=null, decVal2=null;
    KeyPair keys = null;
    BigInteger m = null;

    public OTSender(String msg0, String msg1) {
        // OTSender constructor
        this.msg0 = stringConverter(msg0, 128);
        this.msg1 = stringConverter(msg1, 128); 
    }

    // Encryption method
    public void encrypt(){
        SecureRandom randKey = new SecureRandom();
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } 
        catch (NoSuchAlgorithmException e) {
            Logger.getLogger(OTSender.class.getName()).log(Level.SEVERE, null, e);
        }
        keyGen.initialize(modLength, randKey);

        // Generate pair of keys and the top 2 bits of RSA modulus must equal 1
        do {
          keys = keyGen.generateKeyPair();
          m = ((RSAPublicKey)keys.getPublic()).getModulus();
        } while (!m.testBit(modLength-2));

        randVal1 = new byte[128];
        randKey.nextBytes(randVal1);
        randVal2 = new byte[128];
        randKey.nextBytes(randVal2);

        // Store values in files listed below
        save("Random Value 1", randVal1);
        save("Random Value 2", randVal2);
        save("Encrpyt", keys.getPublic().getEncoded());
     }

    // Decryption method
    public void decrypt() {
        byte[] encResp = load("Encrypted Response");

        try {
          Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
          cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
          decVal1 = xor(cipher.doFinal(xor(encResp, randVal1)), msg0);
          decVal2 = xor(cipher.doFinal(xor(encResp, randVal2)), msg1);
        }
        catch (Exception e) { 
            throw new RuntimeException(e); 
        }

        save("Decrypted Value 1", decVal1);
        save("Decrypted Value 2", decVal2);
    }
    
    // Save message data into a file with provided file names
    static void save(String fileName, byte[] data) {
        try {
            Files.write(Paths.get(fileName), data);
        }
        catch(Exception e) {
            System.err.println("Error encountered, could not write data to file: " + fileName);
            System.exit(1);
        }
    }

    // Loads file based on the parameter "fileName" and return file contents as a byte array
    static byte[] load(String fileName) {
        try {
            return Files.readAllBytes(Paths.get(fileName));
        }
        catch(Exception e) {
            System.err.println("File: " + fileName + "could not be read.");
            System.exit(1);
            return null;
        }
    }
    
    // Read method outputs a string prompt and receives user input
    private static String read(String input) {
        System.out.println(input);
        Scanner scan = new Scanner(System.in);
        String msg = scan.nextLine();
        return msg;
    }
    
    // Pause method outputs string prompt and waits for user input before proceeding
    private static void pause(String pauseMsg) {
        System.out.println(pauseMsg);
        Scanner scan = new Scanner(System.in);
        String pauseInput = scan.nextLine();
    }
    
    // XOR operation between 2 byte-arrays
    static byte[] xor(byte[] x, byte[] y) {
        byte[] z = new byte[Math.min(x.length, y.length)];
        for (int i=0; i<z.length; ++i)
            z[i] = (byte)(x[i] ^ y[i]);
        
        return z;
    }
 
    // Convert String to byte with length l
    static byte[] stringConverter(String str, int len) {
        byte[] bArray = str.getBytes(StandardCharsets.UTF_8);
        if (bArray.length > len) {
            System.err.println("Error encountered, the message was too long and could not be encoded in " + len + " bytes.");
            System.exit(1);
        }
        
        return Arrays.copyOf(bArray, len);
    }
 
    // Convert byte to String and removes padding
    static String byteConverter(byte[] x) {
        String str = new String(x, StandardCharsets.UTF_8);
        int len = str.length();
        for (; len > 0 && str.charAt(len-1) == '\0'; --len);
        return str.substring(0, len);
    }
    
    // Create randomized string for random DES key generation
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
        String randomMasterKey = randomString(56);
        try {
            byte[] mBytes =  randomMasterKey.getBytes();
            SecretKeyFactory mk = SecretKeyFactory.getInstance("DES");
            SecretKey desK = mk.generateSecret(new DESKeySpec(mBytes)); //DESKeySpec creates random master key desK 
            Cipher c = Cipher.getInstance("DES"); //DES cipher creation
            
            /*SOCKET CREATION & CONNECTION*/
            Socket sender = new Socket("localhost", port);
            System.out.println("Connection established at: " + sender.getRemoteSocketAddress());
        
            /*DEFINING INPUT AND OUTPUT STREAMS*/
            DataOutputStream dataOut = new DataOutputStream(sender.getOutputStream());
            DataInputStream dataIn = new DataInputStream(sender.getInputStream());
            
            String sig = dataIn.readUTF(); //signature from Receiver
            
            if(sig.toLowerCase().equals("yes")){
                String msg0 = read("\nPlease enter the first message: ");
                String msg1 = read("Please enter the second message: ");
                OTSender ots = new OTSender(msg0, msg1);
                ots.encrypt();
                System.out.println("\nInitial data generated."); 
                System.out.println("Waiting for Receiver to select a message...\n");
                String selectedMsg = dataIn.readUTF();
                System.out.println("The Receiver chose: " + selectedMsg);
                pause("Press 'ENTER' to proceed and send the selected message.");
                ots.decrypt();
                System.out.println("The selected message was sent successfully.");
            }
            else{
                System.out.println("A valid signature was not received from the Receiver.");
                
                /*CLOSING THE SOCKET AND DATASTREAMS*/
                    dataIn.close();
                    dataOut.close();
                    sender.close();  
            }
            
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
}