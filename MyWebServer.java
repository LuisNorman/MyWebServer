/*--------------------------------------------------------

1. Name: Luis Norman / Date: April 28th, 2020

2. Java version used: 1.8, if not the official version for the class:

e.g. build 1.8.0_242-8u242

3. Precise command-line compilation examples / instructions:

> javac MyWebServer.java

4. Precise examples / instructions to run this program:

In separate shell windows:

> java MyWebServer

5. List of files needed for running the program.

 a. Luis Norman's Joke Server Checklist.html
 b. MyWebServer.java

----------------------------------------------------------*/


import java.io.*;  // I/O API
import java.net.*; // Networking API

class ListenWorker extends Thread {    // Create thread class to execute upon socket connection
  Socket sock; 
  ListenWorker (Socket s) {sock = s;} // Thread constructor requires socket so it can know who to communicate with.

  public void run(){ 
    PrintStream out = null; // output stream to send to client
    BufferedReader in = null; // input stream to read from client
    try {
      out = new PrintStream(sock.getOutputStream()); // set up output stream to send data to client
      in = new BufferedReader(new InputStreamReader(sock.getInputStream())); // set up input stream to read from client
      String request = in.readLine (); // read client's request
      System.out.println("Incoming request: "+request); // Print to server console: Incoming request
      if (request != null) { // Check if request is null
        String[] requestArr = request.split(" "); // split request header into array
        if (requestArr[0].equals("GET") && !requestArr[1].contains("favicon")) { // Check if request is get request
          String requestedFile = requestArr[1].substring(1);
          if (requestedFile.startsWith("cgi")) { // check if request is to run a cgi script
            String html = runCGI(requestedFile); // run cgi script
            String contentLength = Integer.toString(html.length()); // Get Content-length
            String contentType = "text/html"; // Content-type: HTML
            out.println("HTTP/1.1 200 OK\r\nContent-Legnth: "+contentLength+"\r\nContent-Type: "+contentType+"\r\n"); // send OK 
            out.println(html); // Send html output to client
          }
          else {
            if (requestedFile.equals("") || requestedFile.endsWith("/")) { // If requested is a directory
              File[] directory = getDirectory(requestedFile); // Get all files and directories in directory
              String contentLength = getLength(directory); // Get the content length 
              String html = generateHTML(directory); // Generate html for to display the dynamic files
              out.println("HTTP/1.1 200 OK\r\nContent-Legnth: "+contentLength+"\r\nContent-Type: text/html\r\n");
              out.println(html); // Send newly generate html to client
            }
            else { // If requested file is a file and not a directory
              InputStream file = getFile(requestArr[1].substring(1)); // Get file 
              if (file != null) { // Check if the file that was requested by the user can be located
                String contentType = getMimeType(requestArr[1]); // if file is not null, get the file type
                InputStream file2 = getFile(requestArr[1].substring(1));;
                String contentLength = getLength(file2); // Get length of file
                out.println("HTTP/1.1 200 OK\r\nContent-Legnth: "+contentLength+"\r\nContent-Type: "+contentType+"\r\n"); // send OK 
                sendFile(file, out); // send file
              }
              else { // File cannot be found
                out.println("HTTP/1.1 404 Page Not Found\r\nContent-Legnth: 50\r\nContent-Type: text/html\r\n"); //send 404
                String html = notFound(); // Generate html to display "file not found"
                out.println(html); // Print file not found
              }
            }
          }
        }
        else {
          out.println("HTTP/1.1 Favicon Not Found\r\nContent-Legnth: 50\r\nContent-Type: text/html\r\n"); //No favicon
        }
      }
      sock.close(); // close socket
    } catch (IOException x) {
      System.out.println("Connetion reset. Listening again..."+x); 
    }
  }

  // Function to generate html to display "File Not Found"
  private static String notFound() {
    StringBuilder html = new StringBuilder();
    html.append("<html>");
    html.append("<body>");
    html.append("<center><h4>File Not Found</h4></center>");
    html.append("</body>");
    html.append("</html>");
    return html.toString();
  }

  // Outputs file to client
  private static void sendFile(InputStream file, OutputStream out) {
    try {
      byte[] buffer = new byte[1000]; // create buffer to store file
        while (file.available()>0) 
            out.write(buffer, 0, file.read(buffer)); // outputs buffer to client. file.read(buffer)=put file in buffer and return number of bytes
    } catch (IOException e) { System.err.println("Failed to send file to client: "+e); } // catch exceptions
  }

  // Returns a stream of the file contents
  private static InputStream getFile(String filename) {
    filename = filename.replaceAll("%20", " ");
    try {
      return new FileInputStream(filename); // return file stream
    }
    catch(IOException ex) {
      System.out.println("File not found: "+filename); // print to server console: file was not found
      return null;
    }
  
  }

  // Returns the files in the directory
  private static File[] getDirectory(String directoryname) {
    directoryname = directoryname.replaceAll("%20", " "); // Added the ability to handle file names w spaces in them
    File fl = new File("./"+directoryname); // Get current file directory
    File[] strFilesDirs = fl.listFiles(); // Get all the files and directories in the current directory
    return strFilesDirs;
  }

  // checks the last characters to find the mime type
  private static String getMimeType(String filename) {
    if (filename.endsWith(".html")) return "text/html"; 
    else return "text/plain";
  }

  // Get the length of a file
  private static String getLength(InputStream file) {
    if (file == null) return "0";
    BufferedReader br = new BufferedReader(new InputStreamReader(file)); // Put file in buffer to access
    String line;
    String holder = "";
    try {
      while ((line = br.readLine()) != null) {
        holder = holder+line;
      }
    }
    catch(IOException ex) {
      System.out.println(ex);
    }
    String length = Integer.toString(holder.length());
    System.out.println(length);
    return length;
  }

  // Get the length of directory
  private static String getLength(File[] directory) {
    if (directory == null) return "0";
    int length = 0;
    for (int i=0; i<directory.length; i++) {
      length += directory[i].getName().length();
    }
    return Integer.toString(length);
  }

  // Generate html to display conetents in a directory
  private static String generateHTML(File[] directory) {
    StringBuilder html = new StringBuilder();
    html.append("<html>");
    html.append("<body>");
    html.append("<a href=\"../\">Parent Directory/</a><br>"); // if file is directory, add "/"
    if (directory != null) {
      for (int i=0; i<directory.length; i++) {
        if (directory[i].isDirectory()) // Check if file is directory
          html.append("<a href=\"/"+directory[i]+"/\">"+directory[i]+"/</a><br>"); // if file is directory, add "/"
        else
          html.append("<a href=\"/"+directory[i]+"\">"+directory[i]+"</a><br>"); 
      }
    }
    html.append("</body>");
    html.append("</html>");
    return html.toString();

  }

  // Run the requested cgi script
  private static String runCGI(String requestedFile) {
    // Bunch of parsing to get the user's inputs
    int usernameStartIndex = requestedFile.indexOf('=', requestedFile.indexOf("person")) + 1; // Get the start index of the name in the url
    int usernameEndIndex = requestedFile.indexOf('&', usernameStartIndex);
    String name = requestedFile.substring(usernameStartIndex, usernameEndIndex);
    int num1StartIndex = requestedFile.indexOf('=', usernameEndIndex)+1;
    int num1EndIndex = requestedFile.indexOf('&', num1StartIndex);
    String num1 = requestedFile.substring(num1StartIndex, num1EndIndex);
    int num2StartIndex = requestedFile.indexOf('=', num1EndIndex)+1;
    String num2 = requestedFile.substring(num2StartIndex); 
    int result = Integer.valueOf(num1) + Integer.valueOf(num2); // add nums together to get result
    StringBuilder html = new StringBuilder(); // create stringbuilder to build html output
    html.append("<html>");
    html.append("<body>");
    html.append("<h4>");
    html.append("Dear " + name + ", the sum of " + num1 + " and " + num2 + " is " + result); // Create message to send to client
    html.append("</h4>");
    html.append("</body>");
    html.append("</html>");
    return html.toString();
    
  }

}

public class MyWebServer {

  public static boolean controlSwitch = true;

  public static void main(String a[]) throws IOException {
    int q_len = 6;
    int port = 2540;
    Socket sock;

    ServerSocket servsock = new ServerSocket(port, q_len); // Create server socket to listen on port 2540

    System.out.println("Luis Norman's Port listener running at 2540.\n");
    while (controlSwitch) { // Listen for the next's client's connection
      sock = servsock.accept(); // Accept the client's connection
      new ListenWorker (sock).start(); // Spawn of worker thread to execute task
      // Uncomment to see shutdown bug:
      // try{Thread.sleep(10000);} catch(InterruptedException ex) {}
    }
  }
}