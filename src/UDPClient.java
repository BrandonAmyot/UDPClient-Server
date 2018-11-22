
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

	private static double k = 3.0;
	private static double windowSize = Math.pow(2.0,k);
	private static long currentSequenceNumber = 0;
	private static long minSeqNum = 0;
	private static long maxSeqNum = -1;
	private static Packet[] packetArr = new Packet[(int) (0.5*windowSize)];
	
	/*
	 * 	fix payload stuff
	 * 	implement window stuff (global window variables to track position -- STOP after a set number???)
	 * 	drop and delay
	 */
	
    private static void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
        try(DatagramChannel channel = DatagramChannel.open()){
            
        	/*
        	 *  commented out for easy testing
        	 */
//        	if(!handshake(channel, routerAddr, serverAddr)) {
//        		System.out.println("Handshake failed! \nProgram terminating...");
//        		return;
//        	}
//        	else {
//        		System.out.println("Handshake success!");        		
//        	}
        	
        	while(currentSequenceNumber < 100) {
        		maxSeqNum += 4;
        		// fill array
        		for(int i = 0; i < packetArr.length; i++) {
        			if(packetArr[i] == null) {
        				String msg = "Hello from packet " + currentSequenceNumber;
        				Packet p = new Packet.Builder()
        						.setType(1)
        						.setSequenceNumber(currentSequenceNumber)
        						.setPortNumber(serverAddr.getPort())
        						.setPeerAddress(serverAddr.getAddress())
        						.setPayload(msg.getBytes())
        						.create();
        				packetArr[i] = p;
        				currentSequenceNumber++;
        			}
        		}
        		// send all packets
        		for (Packet packet : packetArr) {
					channel.send(packet.toBuffer(), routerAddr);
					System.out.println("Sending Packet \"" + packet.getSequenceNumber() + "\" to router at " + routerAddr);
				}
        		
        		
        		// Try to receive a packet within timeout.
        		channel.configureBlocking(false);
        		Selector selector = Selector.open();
        		channel.register(selector, OP_READ);
        		System.out.println("Waiting for the responses");
        		
        		Set<SelectionKey> keys = selector.selectedKeys();
        		selector.select(5000);
        		while(minSeqNum < currentSequenceNumber) {
        			if(keys.isEmpty()){
        				System.out.println("No response after timeout");       						
	        				for (int i = 0; i < packetArr.length; i++) {
	        					if(packetArr[i] == null) {
	        						
	        					}
	        					else {
	        						channel.send(packetArr[i].toBuffer(), routerAddr);
	        						System.out.println("Resending Packet \"" + packetArr[i].getSequenceNumber() + "\" to router at " + routerAddr);	        						
	        					}
	        				}
	        				selector.select(5000);
        			}
        			else {
        				// get all responses in the buffer
        				ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        				SocketAddress router;
        				while((router = channel.receive(buf)) != null) {
        					buf.flip();
        					Packet resp = Packet.fromBuffer(buf);
	                		
	                		if(resp.getType() == 1 && resp.getSequenceNumber() <= maxSeqNum && resp.getSequenceNumber() >= maxSeqNum-3 && packetArr[(int)(resp.getSequenceNumber() % 4)] != null) {
	                			System.out.println("Packet: " + resp);
	                			System.out.println("Router: " + router);
	                			String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
	                			System.out.println("Payload: " + payload);
	                			
//	                			if(resp.getSequenceNumber() == minSeqNum) {
//	                				minSeqNum++;	                				
//	                			}
	                			packetArr[(int)(resp.getSequenceNumber() % 4)] = null;
	                			minSeqNum++;
	                		}
	                		else if(resp.getType() == 0) {
	        					channel.send(packetArr[(int)(resp.getSequenceNumber() % 4)].toBuffer(), routerAddr);
	                		}
	                		keys.clear();
	                		buf.clear();
        				} 
        			}
        		}
        	}
        }
    }

    private static boolean handshake(DatagramChannel channel, SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
		/*	Packet Type: 0 - NAK
		 * 				 1 - ACK
		 * 				 2 - SYN
		 * 				 3 - SYN-ACK
		 */
    	
		String msg = "";
        Packet p = new Packet.Builder()
                .setType(2)
                .setSequenceNumber(0)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(msg.getBytes())
                .create();
        channel.send(p.toBuffer(), routerAddr);

        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        System.out.println("Waiting for the response");
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        while(true) {            	
        	if(keys.isEmpty()){
        		System.out.println("No response after timeout");
        		channel.send(p.toBuffer(), routerAddr);
        		selector.select(5000);
        	}
        	else {
        		break;
        	}
        }
        
        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        System.out.println("Packet: " + resp);
        System.out.println("Router: " + router);
        
        if(resp.getType() == 3 && resp.getSequenceNumber() == 0) { // is sequence number always 0 during handshake?
        	System.out.println("SYN-ACK received");
        	Packet packet = new Packet.Builder()
                    .setType(1)
                    .setSequenceNumber(0)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
            channel.send(packet.toBuffer(), routerAddr);
            
        	return true;
        }
        else {
        	return false;
        }
	}

	public static void main(String[] args) throws IOException {
        // Router address
        String routerHost = "localhost";
        int routerPort = 3000;

        // Server address
        String serverHost = "localhost";
        int serverPort = 8007;

        SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

        runClient(routerAddress, serverAddress);
    }
}

