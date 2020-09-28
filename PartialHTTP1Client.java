import java.net.Socket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;


public class PartialHTTP1Client {


	public static void main(String[] args){

		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);

		try{
			Socket socket = new Socket(hostName, portNumber);

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Connection Established");
			out.println("GET test.txt HTTP/0.9");//THIS IS WHERE WE WOULD SEND THE COMMAND

			System.out.println(in.readLine());
			//socket.close();
			
		}catch(Exception e){

		}

	}



}
