import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*
 * Run as java ClientBoard
 * GUI pops up to connect to server, once user hits CONNECT:
 * another GUI pops up that handles requests for POST, GET, PIN/UNPIN, and
 * DISCONNECT, and CLEAR)
 */
public class ClientBoard extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
	static String serverAddress;
	static Socket socket;
	
	// Streams for conversing with server
	static BufferedReader input;
	static PrintWriter output;
	
	// create the frame for GUI
	static JFrame frame = new JFrame ("Post-It Board Client");
	
	public static void main(String[] args) throws Exception, IOException {
		
		// set up JFrame
		frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add (new ClientBoard());
        frame.pack();
        frame.setVisible (true);
		
	}
	
	// construct components
	private static JTextArea initialInfo = new JTextArea();
	private JLabel ipLabel = new JLabel ("Enter the IP Address of the server:");
	private JTextField ipAddress = new JTextField();
	private JLabel portLabel = new JLabel ("Enter the port number:");
	private JTextField portNumber = new JTextField();
	private JButton connectButton = new JButton ("Connect");
		
	private JLabel requestLabel = new JLabel ("Requests:");
	private JButton postButton = new JButton ("POST");
	private JButton getButton = new JButton ("GET");
	private JButton pinUnpinButton = new JButton ("PIN/ UNPIN");
	private JButton clearButton = new JButton("CLEAR");
	private JButton disconnectButton = new JButton("DISCONNECT");
		
	private JLabel resultLabel = new JLabel ("Result of Request:");
	private JTextArea result = new JTextArea();
	

	// output the GUI once client is connected
	public ClientBoard() {
		setPreferredSize(new Dimension (820, 300));
	    setLayout(null);
		   
		setUpBounds();
		addConnectComponents();
		setUpListeners();
	} 
	
	private void setUpBounds() {

		//set component bounds
		initialInfo.setBounds(15, 20, 795, 90);
		ipLabel.setBounds (235, 20, 350, 30);
		ipAddress.setBounds (235, 50, 350, 30);
		portLabel.setBounds (235, 90, 350, 30);
		portNumber.setBounds (235, 120, 350, 30);
		connectButton.setBounds (235, 170, 350, 40);
		    
		requestLabel.setBounds (15, 110, 150,40);
		postButton.setBounds (15, 150, 150, 40);
		getButton.setBounds (175, 150, 150, 40);
		pinUnpinButton.setBounds (335, 150, 150, 40);
		clearButton.setBounds(495, 150, 150, 40);
		disconnectButton.setBounds(655, 150, 150, 40);
		    
		resultLabel.setBounds (15, 200, 250, 30);
		result.setBounds (15, 230, 795, 45);
		
	}
		
	private void addConnectComponents() {
		//add components needed for connection to server
		add (ipLabel);
		add (ipAddress);
		add (portLabel);
		add (portNumber);
		add (connectButton);
		   
	}
		
	private void removeConnectComponents() {
		//remove components
		remove (ipLabel);
		remove (ipAddress);
		remove (portLabel);
		remove (portNumber);
		remove (connectButton);
	}
		
	private void addRequestComponents() {
		//add components needed for the various request
		initialInfo.setBackground(Color.BLACK);
		initialInfo.setForeground(Color.GREEN);
		add (initialInfo);
		add (requestLabel);
		add (postButton);
		add (getButton);
		add (pinUnpinButton);
		add (clearButton);
		add (disconnectButton);
		add (resultLabel);
		result.setBackground(Color.BLACK);
		result.setForeground(Color.GREEN);
		add (result);
	}

	private void removeRequestComponents(){
		// remove components need for connection
		remove (initialInfo);
		remove (requestLabel);
		remove (postButton);
		remove (getButton);
		remove (pinUnpinButton);
		remove (clearButton);
		remove (disconnectButton);
		remove (resultLabel);
		remove (result);
	}
	
	private void requestResponse(String requestInput){
		
		try {
		output.println(requestInput); //send to server
		String response = (input.readLine()); // print that the message has been received from the server
		result.setText(response);
		}catch (Exception e){
			result.setText("ERROR Occured!");
		}
	}
		
	private void setUpListeners() {
		
		connectButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent click){
				String ip = ipAddress.getText();
				String port = portNumber.getText();
				
				if (ip.isEmpty() || port.isEmpty()) {
					JOptionPane.showMessageDialog(frame, "ERROR: Complete input was not provided in the CONNECT request!","Connection Error",JOptionPane.ERROR_MESSAGE);
				} else {

					int portNum1 = Integer.parseInt(port);

					ServerBoard server = new ServerBoard();
					if (portNum1 == server.getPortNum(portNum1) && (ip.equals("localhost") || ip.equals("127.0.0.1"))) {
						try {
							socket = new Socket(ip, portNum1);
							input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							output = new PrintWriter(socket.getOutputStream(), true);
					
							// display welcome message from server
							String welcomeMsg; 
							welcomeMsg = (input.readLine()); // prints hello message
							welcomeMsg = welcomeMsg + "\n" + (input.readLine()); // prints dimensions of board
							welcomeMsg = welcomeMsg + "\n" + (input.readLine()); // print available colors:
							welcomeMsg = welcomeMsg + "\n" + (input.readLine()); // prints string of colors available
							welcomeMsg = welcomeMsg + "\n" + (input.readLine()); // prints input format for POST
							initialInfo.setText(welcomeMsg);

							frame.setVisible (false);
							removeConnectComponents();
							result.setText("");
							addRequestComponents();
							frame.setVisible (true);

						} catch (Exception e) {
							JOptionPane.showMessageDialog(frame, "ERROR: Ensure that you are connecting to the correct IP and port number!", "Connection Error", JOptionPane.ERROR_MESSAGE);
						}

					}else{
						JOptionPane.showMessageDialog(frame, "ERROR: Ensure that you are connecting to the correct IP and port number!","Connection Error",JOptionPane.ERROR_MESSAGE);
					} 
				}
 
			}
		});
			
		postButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent click){
				String requestInput = (String) JOptionPane.showInputDialog(frame, "Enter POST request details:\n" + 
						"Format = POST,x,y,width,height,color,message", 
						"POST Request", JOptionPane.INFORMATION_MESSAGE, null, null, null);
			
				if (requestInput.isEmpty()) {
					result.setText("ERROR: No input was provided in the POST request!");
				}else{
					requestResponse(requestInput);
				}
			}
		});
			
		getButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent click) {
				String requestInput = (String) JOptionPane.showInputDialog(frame, "Enter GET request details:\n" + 
						"Format: " + "\n" + "GET,PINS" + "\n" + "OR" + "\n" + "GET,color=color,contains=x&y,refersTo=string:", 
						"GET Request", JOptionPane.INFORMATION_MESSAGE, null, null, null);

				if (requestInput.isEmpty()) {
					result.setText("ERROR: No input was provided in the GET request!");
				}else{
					requestResponse(requestInput);
				}
			}
				
		});
			
		pinUnpinButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent click) {
				String requestInput = (String) JOptionPane.showInputDialog(frame, "Enter PIN/UPIN request details:\n" + 
						"Format = PIN,x,y or UNPIN,x,y :", 
						"PIN/ UPIN Request", JOptionPane.INFORMATION_MESSAGE, null, null, null);

				if (requestInput.isEmpty()) {
					result.setText("ERROR: No input was provided in the PIN/UNPIN request!");
				}else{
					requestResponse(requestInput);
				}
			}
		});
		
		clearButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent click) {
				String requestInput = "CLEAR";
				requestResponse(requestInput);
			}
		});
		
		disconnectButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent click) {
				try{
				socket.close();
				result.setText("Disconnected from server!");
				}catch (Exception e){
					result.setText("ERROR Occured!");
				}
				frame.setVisible (false);
				removeRequestComponents();
				addConnectComponents();
				frame.setVisible (true);
			}
		});
	}

}