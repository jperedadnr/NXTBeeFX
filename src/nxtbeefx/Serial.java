package nxtbeefx;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Serial connect with RXTXcomm library to specified serial port
 * 
 * @author José Pereda Llamas
 * Created on 04-dic-2012 - 19:32:10
 */

public class Serial {
    
    private SerialPort m_serialPort;
    private InputStream in;
        
    private final static IntegerProperty intStatus=new SimpleIntegerProperty(0);
    public static IntegerProperty getIntStatus() { return intStatus; }
    
    public void connect( String portName ) throws Exception {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier( portName );
        if( portIdentifier.isCurrentlyOwned() ) {
            System.out.println( "Error: Port is currently in use" );
        } else {
            m_serialPort = (SerialPort) portIdentifier.open( this.getClass().getName(), 2000 );

            m_serialPort.setSerialPortParams( 9600, SerialPort.DATABITS_8,
                                            SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );

            in = m_serialPort.getInputStream();
        }
    }
    
    /*
     * Thread for reading serial port indefinetely,
     * started from service
     */
    public void read(){
        String sz="";
        byte[] buffer = new byte[ 1024 ];
        int len = -1;
        try {
          while( in!=null && (len = in.read( buffer ) ) > -1) {
            if(len>0){
                // string of space-separated hexadecimal numbers                        
                for(int i=0; i<len; i++){
                    sz=sz.concat(" ").concat(dec2hexStr(buffer[i]));                    
                }

                if(sz.length()>6){
                    if(sz.trim().endsWith("0D 0A")){
                        String sz2=hex2ReadStr(sz);
                        // sz2 should be a number
                        intStatus.set(new Integer(sz2).intValue());
                        // reset sz string, and continue reading
                        sz="";
                    }
                }
            }
          }
        } catch( IOException e ) {
          System.out.println("Error reading: "+e.getMessage());
        } finally {
            try{
                System.out.println("In closing");
                in.close();
            } catch(IOException ioe){ 
            System.out.println("Error reading close: "+ioe.getMessage());} 
        }
    }
    
    /*
     * close inputstream, from service
     */
    public void disconnectInput(){
        
        try {
            if(in!=null){
                System.out.println("Closing InputStream");
                in.close();
            }
        } catch (IOException ex) {
            System.out.println("Error InputStream "+ex.getMessage());
        } finally {
            in=null;
        }
        System.out.println("InputStream closed");
    }
    
    /*
     * Thread to close serial port
     */
    public void disconnect(){
    
        try {
            new Thread(){
                @Override
                public void run(){
                    System.out.println("Removing listener from serial port");
                    if(m_serialPort!=null){
                        m_serialPort.removeEventListener();
                    }
                    System.out.println("Closing Serial port...");                
                    if(m_serialPort!=null){
                        m_serialPort.close();
                    }
                    System.out.println("Serial port closed");
                }
            }.start();
        } catch (Exception ex) {
            System.out.println("Error Serial port "+ex.getMessage());
        } finally {
            m_serialPort=null;
        }        
    }
    
    /*
     * write commands to NXT
     */
    public void write(String s){
        String sz=String2Hex(s);
        try {
            OutputStream out = m_serialPort.getOutputStream();
            try {
                for(byte b: toBytes(sz) ) {
                    out.write( b );
                }
            } catch( IOException e ) {
                System.out.println( "Error bytes: "+e.getMessage());
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            System.out.println( "Error output: "+ex.getMessage());
        }
    }
    
    
    /*
     * Conversion methods
     */
    public static String dec2hexStr(byte b){
        return Integer.toString((b&0xff)+0x100,16).substring(1).toUpperCase();
    }
    public static String dec2hexStr(int b){
        return Integer.toString((b&0xff)+0x100,16).substring(1).toUpperCase();
    }
    public static String hex2String(String s) {
        int decimal = Integer.parseInt(s, 16);        
         if(decimal==0){
            return "0";
        }
       return String.valueOf((char)decimal);
    }
    
    /*
     * Convert string of space-separated hexadecimal numbers into a readable string
     * decoding all the digit bytes plus '.' and ','
     */
    public static String hex2ReadStr(String hexSz){
        String out="";
        if(hexSz.trim().equals("")){
            return out;
        }
        String[] bytes = (hexSz.trim()).split("\\s");   
        for(int i=0; i<bytes.length; i++){
            char theChar=(char) Byte.decode("0x"+bytes[i]).byteValue();
            if(Character.isLetterOrDigit(theChar)){
                out=out.concat(hex2String(bytes[i]));
            } else if(theChar=='.' || theChar==','){
                out=out.concat(hex2String(bytes[i]));                
            }
        }
        return out;
    }
    
    public static String String2Hex(String s){
        String out="";
        for(int i=0; i<s.length(); i++){
            out=out.concat(" ").concat(dec2hexStr((int)s.charAt(i)));
        }
        return out.trim();
    }
    
    /**
      * Convert a string of space-separated hexadecimal numbers into an array of bytes.
      * @param hexString the string to convert.
      * @return the resulting array of bytes.
    */
     public static byte[] toBytes(String hexString)
     {
         String[] bytes = hexString.split("\\s");        
         return toBytes(bytes);
     }
     
     public static byte[] toBytes(String[] bytes)
     {
        List<Byte> list = new ArrayList<Byte>();
        for (String bStr: bytes){
            int n=0;
            if(!(bStr.equals("0") || bStr.equals("00"))){
                n = Byte.parseByte(bStr.substring(0, 1), 16);
                n = 16 * n + Byte.parseByte(bStr.substring(1), 16);
            }
            list.add(new Byte((byte) n));
        }
        byte[] result = new byte[list.size()];
        for (int i=0; i<list.size(); i++){
            result[i] = list.get(i);
        }
        return result;
     }
}
