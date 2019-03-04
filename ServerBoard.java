import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/*
 * Run as java ServerBoard 4554 200 100 red white yellow green
 */
public class ServerBoard {

	public static int portNum;
	public static String[] ArrayOfColors;
	public static int boardWidth;
	public static int boardHeight;
	public static ArrayList<Note> notes = new ArrayList<Note>();

	public static void main(String[] args) throws Exception {
		// reads input from the command line arguments
		portNum = Integer.parseInt(args[0]);
		boardWidth = Integer.parseInt(args[1]);
		boardHeight = Integer.parseInt(args[2]);
		
		// array to store colors
		ArrayOfColors =  new String[args.length - 3];
		ArrayOfColors[0] = args[3];
		int j = 1;
		for (int i = 4; i < args.length; i++) {
			ArrayOfColors[j] = args[i];
			j++;
		}
		
		System.out.println("The message board server is running on port "+ portNum + ".");
		int clientNum = 0;
		ServerSocket listener = new ServerSocket(portNum);
		try {
			while (true) {
				// listener for POST, GET, PIN/UNPIN, CLEAR, DISCONNECT
				new ListenForRequests(listener.accept(), clientNum++).start();
			}
		} finally {
			listener.close();
		}
	}

	public static int getPortNum(int portNum) {
		return portNum;
	}

	public static boolean isNumeric(String str) {
		try {
			int d = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static boolean contains(String[] array, String value) {
		for (String str: array) {
			if (str == value || value != null && value.equals(str)) {
				return true;
			}
		}
		return false;
	}
	
	private static class ListenForRequests extends Thread {
		private Socket socket;
		private int clientNum;
		
		public ListenForRequests(Socket socket, int clientNum) {
			this.socket = socket;
			this.clientNum = clientNum;
			System.out.println("New connection with client #"+ clientNum + " on port "+ socket.getLocalPort());
		}
		
		public void run() {
			
			try {
				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
				ServerBoard server = new ServerBoard();

				// the following will print on the client side once they connect to the server
				output.println("Hello, you are client #" + clientNum + ".");
				output.println("The dimensions of this board are: "+ boardWidth + " x "+ boardHeight);
				output.println("Available colors: ");
				
				for (int i = 0; i < ArrayOfColors.length; i++) {
					output.print(ArrayOfColors[i] + " ");
				}
				output.print("\n");
				output.println("Now accepting requests to the server!");
				
				while (true) {
					// reads input from the client 
					String inputFromClient = input.readLine();
					if (inputFromClient == null) {
						break;
					} 

					// here parse the input based on request -- use string spliting					
					if (inputFromClient.contains(",")) {
						try {
							String[] inputArray = inputFromClient.split(",");
							int n = inputArray.length;
							if (inputArray[0].equals("POST") && n == 7) {
								// POST: create new note and store it in the arraylist
								int x = Integer.parseInt(inputArray[1]);
								int y = Integer.parseInt(inputArray[2]);
								if (contains(ServerBoard.ArrayOfColors, inputArray[5]) && x <= server.boardWidth && x >= 0 && y <= server.boardHeight && y >= 0) {
									Note note1 = new Note(Integer.parseInt(inputArray[1]), Integer.parseInt(inputArray[2]), Integer.parseInt(inputArray[3]), Integer.parseInt(inputArray[4]), inputArray[5], 0, inputArray[6]);
								
									ServerBoard.notes.add(note1);
									output.println("Message has been POSTED to the board! NOTE: Message status still UNPINNED");
								} else {
									output.println("ERROR: Please refer to the request formats for this protocol!");
								}
								
							} else if (inputArray[0].equals("PIN")) {
								if (inputArray[1].equals("ALL") && n == 2) {
									// PIN,ALL request: pin all message on the server
									for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
										Note note = i.next();
										note.status += 1;
									}
									output.println("All messages on server have been pinned.");
								} else if (isNumeric(inputArray[1]) && isNumeric(inputArray[2]) && n == 3) {
									// PIN,x,y : pin all messages that contain x,y coordinate (assuming this has nothing to do with width and height???)
									int x = Integer.parseInt(inputArray[1]);
									int y = Integer.parseInt(inputArray[2]);

									for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
										Note note = i.next();
										int NoteX = note.x;
										int NoteY = note.y; 

										if (NoteX <= x && NoteY <= y) {
											note.status += 1;
										}
									}
									output.println("All messages containing "+ x + " and " + y+ " coordinate have been pinned.");
								} else {
									// THIS ISNT WORKING IDK WHY
									output.println("ERROR: Please refer to the request formats for this protocol!");
								}

							} else if (inputArray[0].equals("UNPIN")) {
								if (inputArray[1].equals("ALL") && n == 2) {
									// UNPIN,ALL
									for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
										Note note = i.next();
										note.status += 1;
									}
									output.println("All messages on server have been unpinned.");
								} else if (isNumeric(inputArray[1]) && isNumeric(inputArray[2]) && n == 3) {
									// UNPIN,x,y : unpin all messages containing x,y coordinate
									int x = Integer.parseInt(inputArray[1]);
									int y = Integer.parseInt(inputArray[2]);

									for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
										Note note = i.next();
										int NoteX = note.x;
										int NoteY = note.y;
										
										if (NoteX <= x && NoteY <= y) {
											note.status -= 1;
										}
									}
									output.println("All messages containing "+ x + " and " + y+ " coordinate have been unpinned.");
								} else {
									output.println("ERROR: Please refer to the request formats for this protocol!");
								}
							
							} else if (inputArray[0].equals("GET")) {
								if (inputArray[1].equals("PINS") && n == 2) {
									// GET,PINS : return all pinned notes
									for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
										Note note = i.next();
										if (note.status > 0) {
											output.print(note.message + ", ");
										}
									}
									output.print("\n");
									output.flush();
								} else if (n == 4) {
									if (inputArray[1].contains("=") && inputArray[2].contains("=") && inputArray[2].contains("&") && inputArray[3].contains("=")) {
										// GET,color="somecolor",contains="x&y",refersTo="something"
										String[] colorArray = inputArray[1].split("=");
										String[] containsArray = inputArray[2].split("=");
										String[] XandYCoor = containsArray[1].split("&"); // stores x and y
										String[] refersArray = inputArray[3].split("=");
										
										if ((colorArray[0].equals("color") && contains(ServerBoard.ArrayOfColors, colorArray[1])) && 
											(containsArray[0].equals("contains") && isNumeric(XandYCoor[0]) && isNumeric(XandYCoor[1])) &&
											(refersArray[0].equals("refersTo"))) {
											int xC = Integer.parseInt(XandYCoor[0]);
											int yC = Integer.parseInt(XandYCoor[1]);
											for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
												Note note = i.next();
												String color = note.color;
												int x = note.x;
												int y = note.y;
												String message = note.message;

												if (color.equals(colorArray[1]) && x < xC && y < yC && message.contains(refersArray[1])) {
													output.print(note.message+", ");
												}
											}
											output.print("\n");
											output.flush();
										} else {
											output.println("ERROR: No such notes exist OR request format incorrect!");
										}

									} else {
										output.println("ERROR: Please refer to the request formats for this protocol!");
									}
								} else if (n == 3) {
									if (inputArray[1].contains("=") && inputArray[2].contains("=")) {
										String[] sepOne = inputArray[1].split("=");
										String[] sepTwo = inputArray[2].split("=");
										
										// GET,color="somecolor",contains="x&y
										if (sepOne[0].equals("color") && contains(ServerBoard.ArrayOfColors, sepOne[1]) && sepTwo[0].equals("contains") && inputArray[2].contains("&")) {
											String[] XandYCoor = sepTwo[1].split("&");
											String colorReq = sepOne[1];
											int xC = Integer.parseInt(XandYCoor[0]);
											int yC = Integer.parseInt(XandYCoor[1]);
											for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
												Note note = i.next();
												String color = note.color;
												int x = note.x;
												int y = note.y;
												if (color.equals(colorReq) && x < xC && y < yC) {
													output.print(note.message + ", ");
												}
											}
											output.print("\n");
											output.flush();
										} else if (sepOne[0].equals("color") && contains(ServerBoard.ArrayOfColors, sepOne[1]) && sepTwo[0].equals("refersTo")) {
											// GET,color="somecolor",refersTo="something"
											String colorReq = sepOne[1];
											for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
												Note note = i.next();
												String color = note.color;
												String message = note.message;
												if (color.equals(colorReq) && message.contains(sepTwo[1])) {
													output.print(note.message +", ");
												}
											}
											output.print("\n");
											output.flush();
										} else if (sepOne[0].equals("contains") && inputArray[1].contains("&") && sepTwo[0].equals("refersTo")) {
											// GET,contains="x&y",refersTo="something"
											String[] XandYCoor = sepOne[1].split("&");
											int xC = Integer.parseInt(XandYCoor[0]);
											int yC = Integer.parseInt(XandYCoor[1]);
											for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
												Note note = i.next();
												int x = note.x;
												int y = note.y;
												String message = note.message;
												if (x < xC && y < yC && message.contains(sepTwo[1])) {
													output.print(note.message+", ");
												}
											}
											output.print("\n");
											output.flush();
										} else {
											output.println("ERROR: No such notes exist OR request format incorrect!");
										}
									} else {
										output.println("ERROR: Please refer to the request formats for this protocol!");
									}
								} else if (n == 2) {
									if (inputArray[1].contains("=")) {
										String[] sep = inputArray[1].split("=");
										if (sep[0].equals("color") && contains(ServerBoard.ArrayOfColors, sep[1])) {
											// GET,color="somecolor"
											String colorReq = sep[1];
											for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
												Note note = i.next();
												String color = note.color;
												if (color.equals(colorReq)) {
													output.print(note.message+", ");
												}
												output.print("\n");
												output.flush();
											} 
										} else if (sep[0].equals("contains")) {
											// GET,contains="x&y"
											String[] XandYCoor = sep[1].split("&");
											if (sep[1].contains("&") && isNumeric(XandYCoor[0]) && isNumeric(XandYCoor[1])) {
												int xC = Integer.parseInt(XandYCoor[0]);
												int yC = Integer.parseInt(XandYCoor[1]);
												for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
													Note note = i.next();
													int x = note.x;
													int y = note.y;
													if (x < xC && y < yC) {
														output.print(note.message+", ");
													}
													output.print("\n");
													output.flush();
												}
											} else {
												output.println("ERROR: Please refer to the request formats for this protocol!");
											}
										} else if (sep[0].equals("refersTo")) {	
											// GET,refersTo="something"
											for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
												Note note = i.next();
												String message = note.message;
												if (message.contains(sep[1])) {
													output.print(note.message+", ");
												}
											}
											output.print("\n");
											output.flush();
										} else {
											output.println("ERROR: No such notes exist OR request format incorrect!");
										}

									} else {
										output.println("ERROR: Please refer to the request formats for this protocol!");
									}
								} else {
									output.println("ERROR: Please refer to the request formats for this protocol!");
								}
							} else {
								output.println("ERROR: Please refer to the request formats for this protocol!");
							}
						} catch (Exception e) {
							output.println("ERROR: Please refer to the request formats for this protocol!\n"+ e+"\n");
						}
					} else if (inputFromClient.equals("CLEAR")) {
						// CLEAR: remove all unpinned notes
						if (ServerBoard.notes.isEmpty()) {
							output.println("ERROR: No unpinned notes to clear!");
						} else {
							Iterator<Note> itr = ServerBoard.notes.iterator();
							int i = 0;
							while (itr.hasNext()) {
								Note note = itr.next();
								if (note.status == 0) {
									itr.remove();
								}
								i++;
							}
							output.println("All unpinned notes removed from server.");
							}
					} else {
						output.println("ERROR: Please refer to the request format for this protocol!\n");
					}
					
					// FOR TESTING PURPOSES: print all notes stored on server
					//System.out.println("\nFor testing:");
					//for (Iterator<Note> i = ServerBoard.notes.iterator(); i.hasNext();) {
					//	Note note = i.next();
					//	System.out.println("Message: " + note.message + "\n"+ "Status: "+ note.status+ "\n");
					//}
				}
				
			} catch (IOException e) {
				System.out.println("Error handling client #"+ clientNum + ": "+ e);
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("Couldn't close a socket");
				}
				System.out.println("Connection with client #"+ clientNum + " closed");
			}
			
		}
	
	}
}

/*
 * Create new note object by:
 * Note noteOne = new Note(x,y,width,height,color,status,message);
 */ 
class Note {
	// x and y coordinate, width and height, and color of post-it note for GUI
	int x;
	int y;
	int width;
	int height;
	String color;
	int status; // pinned or unpinned
	String message; // message of post-it note

	Note(int x, int y, int width, int height, String color, int status, String message) {
		// error handling here: make sure x and y (and width and height) cannot be negative (if they are set them to 0)
		if (x < 0) {
			this.x = 0;
		} else {
			this.x = x;
		}
		if (y < 0) {
			this.y = 0;
		} else {
			this.y = y;
		}
		this.width = width;
		this.height = height;
		this.color = color;
		this.status = 0; // default is 0 
		// NOTE: since a note can be pinned more than once, 0=UNPINNED and anything more than 0 is the # of times its been pinned
		this.message = message;
	}
	
}