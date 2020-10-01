
import java.net.Socket;
import java.net.ServerSocket;
import java.lang.Thread;
import java.io.IOException;
import static java.lang.System.out;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.File;  
import java.io.FileNotFoundException;  
import java.util.Scanner;
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors; 
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.text.DateFormat;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class PartialHTTP1Server {

	public static void main(String[] args){
		
		int portNum = Integer.parseInt(args[0]);
		//Thread threadPool[] = new Thread[5];
		//int poolSize = 5;
		int threadsInUse = 0;

		ExecutorService executor = Executors.newCachedThreadPool();
		ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;		
		pool.setCorePoolSize(5);
		pool.setMaximumPoolSize(50);
		
		
		try{
			ServerSocket serverSocket = new ServerSocket(portNum);
			
			

			while(true){
				//try{
					//System.out.println("Thread count: " + pool.getPoolSize());
					
					Socket clientSocket = serverSocket.accept();
					
					if(pool.getActiveCount() < pool.getMaximumPoolSize()){
						executor.submit(new Thread(new Connection(clientSocket)));
					}else{
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						out.println("503 Service Unavailable");
					}	
					//System.out.println("Active count: " + pool.getActiveCount());
			

					/**
					threadPool[threadsInUse] = new Thread(new Connection(clientSocket));
					threadPool[threadsInUse].start();
					
					threadsInUse++;
					**/
					
					//threadPool[threadsInUse].isAlive() to check if thread is still running
					
					
			/**	}catch(RejectedExecutionException e){
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					out.println("503 Service Unavailable");
			**/
				//}catch(Exception e){
				//	System.out.println("hi");
				//}
			
			}
			
			
		}catch(Exception e){
		
		}
		
		
		
			
			
	
	
		//new Thread(new Connection());
		
	}
}

public class Connection implements Runnable {
	private Socket s;
	private PrintWriter out;
	private BufferedReader in;

	public Connection(Socket s){
		this.s = s;
		try{
			this.out = new PrintWriter(this.s.getOutputStream(), true);
            		this.in = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
		}catch(Exception e){
		}
	}

	public void run(){
		
		try{
			
			String input = this.in.readLine();//READ COMMAND FROM CLIENT
			String command = "";
			File stringRequestURI;
			String version;
			double versionNum;				

			//check if first word is a valid command
			int i = 0;
			while(input.charAt(i) != ' '){
				i++;
				if(i >= input.length()){ //prevent a null pointer exception, and check for imporperly formatted string
					//reached end of input before a space, input not formatted correctly error 400
					sendError(400);
					return;
					
				}
			}
			command = input.substring(0, i);
			
			if(command.equals("GET") || command.equals("POST") || command.equals("HEAD")){ //valid, implemented command
				
			}else if(command.equals("DELETE") || command.equals("LINK") || command.equals("PUT") || command.equals("UNLINK")){
				//valid, but unimplemented command, error 501
				sendError(501);
			}else{//invalid command, error 400
				sendError(400);
				return;
			}

			
			

			
			i++;	//before this line, i would be the index of the space between the command and request (in a properly formatted command)
			int j = i; //now i and j are both the index of the first letter of the command, we will use these to get the request substring
			
			//double check that i and j are not out of bounds
			if(i >= input.length()){
				sendError(400);
				return;
			}	
			
			while(input.charAt(j) != ' '){
				j++;
				if(j >= input.length()){//prevent null pointer exception
					//reached end of input before a space, input not formatted correclty error 400
					sendError(400);
					return;
					
				}
			}
			
			stringRequestURI = new File(input.substring(i, j));
			
			//if file name is not valid it could be either that the file does not exist (a 404) or that the input is invalid (a 400)
			
			//if input was proper to this point and no errors encountered, j should be the index of the space between request URI and HTTP version
			j++;
			version = input.substring(j);
			if( version.length() < 4 || !(version.substring(0,5).equals("HTTP/"))){//length check prevents null pointer exception on substring call
				//if either part of the if statement is true, input is not proper, error 400
				sendError(400);
				return;
				
			}else{
				try{
					versionNum = Double.parseDouble(version.substring(5));
					if(versionNum > 1 || versionNum < 0){
						//unsupported version error 505
						sendError(505);
						return;
					}
				}catch(Exception e){
					//improper format of version
					sendError(400);
					return;
					
				}
			}
			//if we got to this point with no errors, the input format is valid, and the version number is supported
			//now we just check if the file name is valid, and if so get started processing the output!
			
			if(!(stringRequestURI.exists())){ //if file is not found
				sendError(404);
				return;
				
			}
			
			//System.out.println("REACHED END, NO ERRORS!");

			if(command.equals("GET") || command.equals("POST")){
				getOrPost(input, stringRequestURI);
			}else if(command.equals("HEAD")){
				head(input, stringRequestURI);
			}
			
			
			
			s.close();
			
		}catch(IOException e){
			System.out.println("Exception");
		}
	}
	
	public void sendError(int errNum){
		System.out.println("Error: " + errNum);
		try{
			this.out.flush();
			Thread.sleep(250);
			this.out.close();
			this.in.close();
			this.s.close();	
		}catch(Exception e){
			
		}
	}

	public void getOrPost(String input, File requestURI){
		//first, determine if the header request If-Modified-Since is included
		String headerRequestDate = "";
		Date ifModifiedSinceDate = new Date();

		String requestHeader = "";
		
		
			try{
				if(this.in.ready()){
					requestHeader = this.in.readLine();
					
				}
			}catch(IOException e){
				System.out.println("Exception");

			}
		
		try{
			if(requestHeader.substring(0, 17).equals("If-Modified-Since")){
				headerRequestDate = requestHeader.substring(19);//17 is the colon after If-Modified-Since, 18 is the space, 19 should be the first char of the date	
			}				
			
					
			
		}catch(IndexOutOfBoundsException e){

			//header is insufficient length, do a normal GET or POST
		}catch(SecurityException e){
			sendError(403); //forbidden error
			return;
		}catch(NullPointerException e){
			System.out.println("Null pointer Exception");
		}
		
		if(headerRequestDate.length() > 1){
			
			try{
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				ifModifiedSinceDate = sdf.parse(headerRequestDate);
			}catch(ParseException e){
				//System.out.println("seriously?");
				headerRequestDate = "";//if the parse method throws an exception, the date was invalid, do normal get or post function, change the headerRequestDate back to empty to signal this
			}catch(NullPointerException e){
				//System.out.println("a diff exception?");
			}catch(IllegalArgumentException e){
				//System.out.println("third one");
			}
		}

		/**
		if(headerRequestDate.length() == 0){
			System.out.println("no, or invalid header");
		}else{
			
			System.out.println(ifModifiedSinceDate.getTime());
		}
		*/
		//At this point if there is no or invalid header, headerRequestDate is empty, otherwise it has the date
		
		//if last modified date is less than or equal ifModifiedSinceDate, send expires date and error 304

		if(headerRequestDate.length() > 0){//we use the length of headerRequestDate to check if there was a valid header request
			if(ifModifiedSinceDate.getTime() <= requestURI.lastModified()){
				System.out.println("Expires: Sat, 21 Jul 2021 11:00:00 GMT\r\n");
				sendError(304);
				return;
			}
		}

		//If the If-modified-since header is not there, or the file has been modified since, execute get or post normally...
		

		
			
		
	}
	
	public void head(String input, File requestURI){
		

	}


}



