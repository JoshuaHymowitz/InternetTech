// App.java
package com.github.username;

import java.io.IOException;
import java.net.Inet4Address;

import java.util.Objects;
import java.util.ArrayList;
import java.net.Inet4Address;

import com.sun.jna.Platform;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.IcmpV4CommonPacket.IcmpV4CommonHeader;
import org.pcap4j.packet.IcmpV4CommonPacket;


public class App {

	static int check_number = 0;
    	static int UDP_number = 0;
	static float UDP_byte = 0;
	static int TCP_number = 0;
	static float total_byte = 0;
	static int ICMP_number = 0;
	static float ICMP_byte = 0;
	static int other_number = 0;
	static float other_byte = 0;

	static double first_pack_time = 0;
	static double last_pack_time = 0;
	static boolean first_packet_time= false;
	static boolean last_packet_time=false;
	

    public static void main(String[] args) throws PcapNativeException, NotOpenException {
        System.out.println("Let's start analysis ");
        // New code below here
                  
       	final PcapHandle handle;

	handle = Pcaps.openOffline(args[0] );
	
	System.out.println(handle);	
	ArrayList<TCP> TCPs = new ArrayList<TCP>();
	
	
        PacketListener listener = new PacketListener() {
                public void gotPacket(Packet packet) {

			//new wireshark2 code:
			double timeArrived = (double)handle.getTimestamp().getTime();

			if(packet.get(TcpPacket.class) != null){
				TcpPacket tcpPacket = packet.get(TcpPacket.class);
				int destPort = tcpPacket.getHeader().getDstPort().valueAsInt();
				int srcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();

				boolean syn_c = tcpPacket.getHeader().getSyn();
				boolean fin_c = tcpPacket.getHeader().getFin();
				
				IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);

				Inet4Address srcAddr = ipV4Packet.getHeader().getSrcAddr();
               			Inet4Address depAddr = ipV4Packet.getHeader().getDstAddr();

                		String destIP = depAddr.getHostAddress();
                		String srcIP = srcAddr.getHostAddress();

				
				
				
				if(TCPs.size() == 0){
					TCPs.add(new TCP(srcIP, srcPort, destIP, destPort, packet.length(), syn_c, timeArrived));
					if(!syn_c){//if packet arrived before first syn_c, assign content to incomplete
						TCPs.get(0).numBytesIncomplete += TCPs.get(0).pendingBytes;
						TCPs.get(0).numPacketsIncomplete += TCPs.get(0).pendingPackets;
						TCPs.get(0).pendingBytes = 0;
						TCPs.get(0).pendingPackets = 0;
					}
					
				}else{
				int count = 0;
				for(int i = 0; i < TCPs.size(); i++){
					
					if(Objects.equals(TCPs.get(i).srcIP, srcIP) && TCPs.get(i).srcPort == srcPort && Objects.equals(TCPs.get(i).destIP, destIP) && TCPs.get(i).destPort == destPort){
						
						//TCPs.get(i).pendingBytes += packet.length();
						//TCPs.get(i).pendingPackets++;
						//whenever a new packet arrives to a flow, there are 5 possible scenarios
						//first, either the most recent flag received was syn, or a fin (we treat the edge case where we have not received a flag yet as a fin being most recent because we are outside of syn fin pair)
						
						
						
						if(TCPs.get(i).syn_c){
							if(syn_c){//if the last flag we got was a syn, and we get another syn, any pending bytes and packets become categorized as incomplete
								TCPs.get(i).numBytesIncomplete += TCPs.get(i).pendingBytes;
								TCPs.get(i).numPacketsIncomplete += TCPs.get(i).pendingPackets;
								
								//TCPs.get(i).numBytesIncomplete += packet.length();
								//TCPs.get(i).numPacketsIncomplete++;
								//reset pending 
								TCPs.get(i).pendingBytes = packet.length();
								TCPs.get(i).pendingPackets = 1;
								TCPs.get(i).firstTime = timeArrived;
								

							}else if(fin_c){//packet is being completed
								TCPs.get(i).pendingBytes += packet.length();
								TCPs.get(i).pendingPackets++;
						
								TCPs.get(i).numBytesComplete += TCPs.get(i).pendingBytes;
								TCPs.get(i).numPacketsComplete += TCPs.get(i).pendingPackets;
								//switch syn and fin, reset pending
								TCPs.get(i).syn_c = false;
								TCPs.get(i).fin_c = true;
							
								TCPs.get(i).pendingBytes = 0;
								TCPs.get(i).pendingPackets = 0;

								TCPs.get(i).lastTime = timeArrived;
							}else{//no flag, just update pending
								TCPs.get(i).pendingBytes += packet.length();
								TCPs.get(i).pendingPackets++;
								//TCPs.get(i).lastTime = (double)handle.getTimestamp().getTime();
							}

						}else if(TCPs.get(i).fin_c){
							if(syn_c){//starting a new flow
								
							
								//flip syn and fin and update pending
								TCPs.get(i).syn_c = true;
								TCPs.get(i).fin_c = false;
							
								TCPs.get(i).pendingBytes += packet.length();
								TCPs.get(i).pendingPackets = 1;

								//TCPs.get(i).lastTime = (double)handle.getTimestamp().getTime();
								TCPs.get(i).firstTime = timeArrived;

								

							}else{//regardless of fin, if the last flag we got was fin, and this packet is not sin, automatically add content to incomplete
								TCPs.get(i).numBytesIncomplete += packet.length();
								TCPs.get(i).numPacketsIncomplete++;
								//TCPs.get(i).lastTime = (double)handle.getTimestamp().getTime();

							}

						}

						break;
					}else if(i == TCPs.size() - 1){
						
						TCPs.add(new TCP(srcIP, srcPort, destIP, destPort, packet.length(), syn_c, (double)handle.getTimestamp().getTime()));
						if(!syn_c){//if packet arrived before first syn_c, assign content to incomplete
							TCPs.get(i+1).numBytesIncomplete += TCPs.get(i+1).pendingBytes;
							TCPs.get(i+1).numPacketsIncomplete += TCPs.get(i+1).pendingPackets;
							TCPs.get(i+1).pendingBytes = 0;
							TCPs.get(i+1).pendingPackets = 0;

						}
						i++;
						//break;
						
					}		
		

				}
				}
			}else if(packet.get(UdpPacket.class)!=null){
				UDP_number = UDP_number + 1 ;
				UDP_byte += packet.length();
			}else if(packet.get(IcmpV4CommonPacket.class)!=null){
				ICMP_number++;
				ICMP_byte += packet.length();
			}else{
				other_number++;
				other_byte += packet.length();
			}
			


			
	                /**				
				if(first_packet_time==false)
				{
				first_pack_time = (double)handle.getTimestamp().getTime();
				first_packet_time=true;
				}
				last_pack_time = (double)handle.getTimestamp().getTime();
              					
				check_number = 1+ check_number;
		 		total_byte = total_byte + (float)packet.length();
			
				if(packet.get(TcpPacket.class)!=null){
				   TCP_number = TCP_number +1 ;
				}

				if(packet.get(UdpPacket.class)!=null){
				   UDP_number = UDP_number + 1 ;
				}
			**/

						

                }
        };

        try {
		
	        int maxPackets = -1;
		
	        handle.loop(maxPackets, listener);

		//once we are done iterating through the packets, and are almost ready to print, we must now just add any flows where the last flag is syn to incomplete (because there was no fin to finish it)
		for(int i = 0; i < TCPs.size(); i++){
				if(TCPs.get(i).syn_c){
					TCPs.get(i).numBytesIncomplete += TCPs.get(i).pendingBytes;
					TCPs.get(i).numPacketsIncomplete += TCPs.get(i).pendingPackets;
				}	
		}
		System.out.println("TCP Summary Table");
		for(int i = 0; i < TCPs.size(); i++){
			if(TCPs.get(i).numPacketsComplete == 0){
				System.out.println(TCPs.get(i).srcIP + ", " + TCPs.get(i).srcPort + ", " + TCPs.get(i).destIP + ", " + TCPs.get(i).destPort + ", " + TCPs.get(i).numPacketsComplete + ", " + TCPs.get(i).numPacketsIncomplete);
			}else{
				
				TCPs.get(i).bandwidth = ((double) TCPs.get(i).numBytesComplete / 125000) / ((double) ((TCPs.get(i).lastTime - TCPs.get(i).firstTime) / 1000000)); //calculate megabits per second by converting bytes to megabits and dividing by the quantity of microseconds converted to seconds
				System.out.println(TCPs.get(i).srcIP + ", " + TCPs.get(i).srcPort + ", " + TCPs.get(i).destIP + ", " + TCPs.get(i).destPort + ", " + TCPs.get(i).numPacketsComplete + ", " + TCPs.get(i).numPacketsIncomplete + ", " + (double) (TCPs.get(i).numBytesComplete + TCPs.get(i).numBytesIncomplete) + ", " + TCPs.get(i).bandwidth);

			}
		}
		System.out.println("\n");
		System.out.println("Additional Protocols Summary Table");
		System.out.println("UDP, " + UDP_number + ", " + UDP_byte);
		System.out.println("ICMP, " + ICMP_number + ", " + ICMP_byte);
		System.out.println("Other, " + other_number + ", " + other_byte);
	} catch (InterruptedException e) {
	        e.printStackTrace();
            }			



	/**

	double total_time = last_pack_time - first_pack_time;
	total_time = total_time/1000.0;
	
	System.out.println( "Total number of packets, "+  check_number);
	System.out.println( "Total number of UDP packets, " + UDP_number);
	System.out.println( "Total number of TCP packets, " + TCP_number);
	System.out.println( "Total bandwidth of the packet trace in Mbps, " + total_byte/total_time/125000.0);
	*/


        // Cleanup when complete
        handle.close();
    }
}


class TCP {


	int numBytesComplete;
	int numPacketsComplete;

	int numBytesIncomplete;
	int numPacketsIncomplete;
	
	//pending represents bytes and packets we have parsed, but do not know yet whether they belong to a completed flow or an incomplete flow,
	int pendingBytes;
	int pendingPackets;

	String srcIP;
	int srcPort;

	String destIP;
	int destPort;

	double firstTime;
	double lastTime;

	double bandwidth;
	
	//these booleans represent whether a syn or fin was more recently received, at any given time 1 is true and 1 is false
	boolean syn_c;
	boolean fin_c;
		
	public TCP(String srcIP, int srcPort, String destIP, int destPort, int length, boolean syn_c, double time){
		this.firstTime = time;
		this.srcIP = srcIP;
		this.srcPort = srcPort;
		this.destIP = destIP;
		this.destPort = destPort;
		this.pendingBytes = length;
		
		this.syn_c = syn_c;
		this.fin_c = !syn_c;
		this.pendingPackets = 1;
		
		numBytesComplete = 0;
		numPacketsComplete = 0;
		numBytesIncomplete = 0;
		numPacketsIncomplete = 0;

	}
	
	




}