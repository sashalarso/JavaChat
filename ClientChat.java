

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

import java.io.IOException;


public class ClientChat extends Thread {

	private static final boolean JAVACARDMODE = false;
    
    boolean DISPLAY = true;

	private Socket socket;

	private Scanner inConsole, inNetwork;
	private PrintWriter outConsole, outNetwork;

	// Client command
	private static final int MSG = 0; // msg
	private static final int SENDFILE = 1; // sendFile login filename
	private static final int LOGOUT = 2; // /logout or /exit
	private static final int LIST = 15;
	private static final int PRIVMSG = 16;

	// secureApp.Server command
	private static final int CONNECTED = 3;
	private static final int ALREADYCONNECTED = 4;
	private static final int REGISTERED = 5;
	private static final int ERR_REGISTERED = 6;
	private static final int DISCONNECTED = 7;
	private static final int SENDFILESTOP = 9;
	private static final int FILETRANSFERMODEON = 10;
	private static final int FILETRANSFERMODEOFF = 11;
	private static final int ISUSERCONNECTED = 12;
	private static final int RECEIVERISCONNECTED = 13;
	private static final int RECEIVERISNOTCONNECTED = 14;

	private boolean isClientConnected = false;

	private boolean fileTransferMode = false;
	private boolean isReceiverConnected = false;
	private boolean checkReceiverState = false;

	private FileOutputStream fout = null;
	private static final int DMS_SENDFILE = 100000;

	private static final byte CLA = (byte) 0x90;
	private static final byte P1 = (byte) 0x00;
	private static final byte P2 = (byte) 0x00;
	private final static byte INS_GET_PUBLIC_RSA_KEY = (byte)0xFE;
	private final static byte INS_GENERATE_RSA_KEY = (byte)0xF6;
	private static final byte INS_RSA_ENCRYPT = (byte) 0xA0;
	private static final byte INS_RSA_DECRYPT = (byte) 0xA2;

	private static short DMS_DES = 248; // DATA MAX SIZE for DES
    private static final byte INS_DES_DECRYPT = (byte) 0xB0;
    private static final byte INS_DES_ENCRYPT = (byte) 0xB2;

    public ClientChat(String host, int port) throws IOException {
		initStream(host, port);
		authentication();
		start();
		listenConsole();
    }

    

	


    /************************************************
     * *********** BEGINNING OF TOOLS ***************
     * **********************************************/


    



	

    /******************************************
     * *********** END OF TOOLS ***************
     * ****************************************/


    


	/* secureApp.Server init & close */
	public void initStream(String host, int port) throws IOException {
		this.inConsole = new Scanner(System.in);
		this.outConsole = new PrintWriter(System.out);

		try {
			this.socket = new Socket(host, port);
			this.inNetwork = new Scanner(socket.getInputStream());
			this.outNetwork = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeNetwork() throws IOException {
		this.outNetwork.close();
		this.socket.close();
		System.exit(0);
	}

	private void closeConsole() throws IOException {
		this.outConsole.close();
	}
	/*********/

	/* Tools */
	private void displayConsole(String raw) {
		this.outConsole.println(raw);
		this.outConsole.flush();
	}

	private void sendServer(String raw) {
		this.outNetwork.println(raw);
		this.outNetwork.flush();
	}

	private static byte[] shortToByteArray(short s) {
		return new byte[] { (byte) ((s & (short) 0xff00) >> 8), (byte) (s & (short) 0x00ff) };
	}

	private String toLowerCases(String s) {
		String r = "";
		for(char c: s.toCharArray()) {
			r += Character.toLowerCase(c);
		}
		return r;
	}

	private void displayBytes(byte[] bytes){
		int i = 0;
		for (byte b : bytes) {
			System.out.printf("%02X ", b);
			if (++i%8 == 0)
				System.out.println("");
		}
	}

	private static short byteToShort(byte b) {
		return (short) (b & 0xff);
	}

	private static short byteArrayToShort(byte[] ba, short offset) {
		return (short) (((ba[offset] << 8)) | ((ba[(short) (offset + 1)] & 0xff)));
	}

	private static byte[] addPadding(byte[] data, long fileLength) {
		short paddingSize = (short) (8 - (fileLength % 8));
		byte[] paddingData = new byte[(short) (data.length + paddingSize)];

		System.arraycopy(data, 0, paddingData, 0, (short) data.length);
		for (short i = (short) data.length; i < (data.length + paddingSize); ++i)
			paddingData[i] = shortToByteArray(paddingSize)[1];

		return paddingData;
	}

	private static byte[] removePadding(byte[] paddingData) {
		short paddingSize = byteToShort(paddingData[paddingData.length - 1]);
		if (paddingSize > 8)
			return paddingData;

		/* check if padding exists */
		for (short i = (short) (paddingData.length - paddingSize); i < paddingData.length; ++i)
			if (paddingData[i] != (byte) paddingSize)
				return paddingData;

		/* Remove padding */
		short dataLength = (short) (paddingData.length - paddingSize);
		byte[] data = new byte[dataLength];
		System.arraycopy(paddingData, 0, data, 0, (short) dataLength);

		return data;
	}

	/*********/

	private boolean isUserconnected(String raw){
		String[] splitRaw = raw.split(" ");
		if (splitRaw.length == 3) {
			sendServer("<SYSTEM> [SENDFILE]: " + "ISUSERCONNECTED" + " " + splitRaw[1].trim() + " " + splitRaw[2].trim());
			return true;
		}else {
			displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
			return false;
		}
	}

	/* Features */
	private void sendFile(String raw) throws IOException {
        System.out.println("test");
		if(!checkReceiverState){
			this.checkReceiverState = isUserconnected(raw);
		} else {
			if (this.isReceiverConnected) {
                System.out.println("test");
				String[] splitRaw = raw.split(" ");
                displayConsole(splitRaw[5]);
				if (splitRaw.length == 8) {
					File f = new File("" + splitRaw[7].trim());
					if (!f.exists()) {
						displayConsole("<SYSTEM> [SENDFILE]: File doesn't exist");
					} else {
						sendServer("/sendfile " + splitRaw[5] + " " + splitRaw[7]);
						FileInputStream fin = new FileInputStream(f);
						int by = 0; int i = 0;
						StringBuilder sb = new StringBuilder();
						while (by != -1) {
							by = fin.read(); ++i;
							sb.append(String.valueOf(by)+";");

							if (by != -1 && i == DMS_SENDFILE) {
								sendServer("<SYSTEM> [SENDFILE] " + sb.toString());
								i = 0;
								sb = new StringBuilder();
							}
							else if (by == -1 && i > 1)
								sendServer("<SYSTEM> [SENDFILE] " + sb.toString());		
						}
						
						sendServer("<SYSTEM> [SENDFILE]: " + "SENDFILESTOP");
					}

					this.checkReceiverState = false;
					this.isReceiverConnected = false;
				} else
					displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
                    displayConsole(splitRaw[0]+" "+ splitRaw[1]+" "+splitRaw[2]);
			}
		}
	}

	private synchronized void retrieveFile(String raw) throws IOException {
		if(raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART")){
			String[] splitRaw = raw.split(" ");
			this.fout = new FileOutputStream("retrieved_" + splitRaw[4]);
		} else if (raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTOP")) {
			this.fileTransferMode = false;
			this.fout.close();
		} else {
			if (raw.startsWith("<SYSTEM> [SENDFILE]")) {
				String[] byteValue = raw.split(" ")[2].split(";");
				for (int i = 0; i < byteValue.length -1; ++i)
					this.fout.write(Byte.parseByte(String.valueOf(shortToByteArray(Short.parseShort(byteValue[i]))[1]), 10));
			} else
				displayConsole(raw);
		}
	}

	private void logout() {
		sendServer("/logout");
	}

	private void authentication() throws IOException {
		while(this.inNetwork.hasNextLine()) {
			String raw = this.inNetwork.nextLine().trim();
			displayConsole(raw);
			
			if (raw.startsWith("<SYSTEM> Enter your username") || raw.startsWith("Enter password:") || raw.startsWith("Confirm password:")){
				sendServer(this.inConsole.nextLine().trim());
			} else if(raw.startsWith("<SYSTEM> Connected as:")){
				this.isClientConnected = true;
				return;
			} else if (raw.startsWith("<SYSTEM> User connected limit reached") || raw.startsWith("<SYSTEM> Registration Successful") || raw.startsWith("<SYSTEM> Username or password is incorrect") || raw.startsWith("<SYSTEM> User already connected")){
				closeConsole();
				closeNetwork();
				return;
			}
		}
	}

	

	

	

	

	
	

	

	/*********/

	private int serverParser(String text){
		if(text.startsWith("<SYSTEM> Disconnecting..."))
			return DISCONNECTED;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART"))
			return FILETRANSFERMODEON;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTOP"))
			return FILETRANSFERMODEOFF;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: User is connected")) // for sendfile receiver
			return RECEIVERISCONNECTED;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: User is not connected")) //for sendfile receiver
			return RECEIVERISNOTCONNECTED;
		else
			return MSG;
	}

	private void listenNetwork() throws IOException {
		while(this.inNetwork.hasNextLine()) {
			String raw = this.inNetwork.nextLine().trim();
			
			switch (serverParser(raw)) {
				case FILETRANSFERMODEON:
					this.fileTransferMode = true;
					break;
				case RECEIVERISCONNECTED:
					this.isReceiverConnected = true;
					sendFile(raw);
					break;
				case RECEIVERISNOTCONNECTED:
					this.isReceiverConnected = false;
					this.checkReceiverState = false;
					break;
				case DISCONNECTED:
					
					closeConsole();
					closeNetwork();
					break;
				default:
                    displayConsole(raw);
					if (!this.fileTransferMode){
						if(!raw.startsWith("<SYSTEM>") && !raw.startsWith("-") && !raw.startsWith("<") && raw.split(" ").length > 1 && !raw.startsWith("[ADMIN]") && !JAVACARDMODE)
							
						displayConsole(raw);
					}
			}
			if(this.fileTransferMode){
				retrieveFile(raw);
			}
		}
	}

	private int commandParser(String text){
		String command = toLowerCases(text.split(" ")[0]);
		if (command.equals("/sendfile"))
			return isClientConnected ? SENDFILE : MSG;
		else if(command.equals("/exit") || command.equals("/logout"))
			return isClientConnected ? LOGOUT : MSG;
		else if(command.equals("/list")){
            
			return isClientConnected ? LIST : MSG;
        }
		else if(command.equals("/msg"))
			return isClientConnected ? PRIVMSG : MSG;
		else
			return MSG;
	}

	private void listenConsole() throws IOException {
		while(this.inConsole.hasNextLine()){
			String raw = this.inConsole.nextLine().trim();
			switch (commandParser(raw)) {
				case SENDFILE: sendFile(raw); break;
				case MSG:  sendServer(raw); break;
				case LIST: sendServer(raw); break;
				case PRIVMSG: sendServer(raw); break;
				case LOGOUT: logout(); break;
			}
		}
	}

	@Override
	public void run() {
		try {
			listenNetwork();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void main( String[] args ) throws InterruptedException, IOException {
		
		try {
	    	new ClientChat("127.0.0.1", 7777);
		} catch (Exception e) {}
    }
}