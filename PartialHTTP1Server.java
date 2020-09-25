
import java.net.Socket;
import java.net.ServerSocket;
import java.lang.Thread;
import java.io.IOException;
import static java.lang.System.out;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;


public class PartialHTTP1Server {

	public static void main(String[] args){
		
		int portNum = Integer.parseInt(args[0]);
		Thread threadPool[] = new Thread[5];
		int poolSize = 5;
		int threadsInUse = 0;
		try{
			ServerSocket serverSocket = new ServerSocket(portNum);


			while(true){
				try{
					Socket clientSocket = serverSocket.accept();
					threadPool[threadsInUse] = new Thread(new Connection(clientSocket));
					threadPool[threadsInUse].start();
					
					threadsInUse++;
					
					//threadPool[threadsInUse].isAlive() to check if thread is still running
					
					
				}catch(Exception e){
					System.out.println("hi");
				}
			
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
		out.println("hello there how are you?");
		try{
			s.close();
			System.out.println("closed");
		}catch(Exception e){
		}
	}


}



