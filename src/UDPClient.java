
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {
	
	private static boolean getReq = false;
	private static boolean postReq = false;
	private static boolean isTextOut = false;
	private static String outputFileName;

	private static String fileName;
	/*
	 * 	fix payload stuff
	 * 	implement window stuff (global window variables to track position -- STOP after a set number???)
	 * 	drop and delay
	 */
	
    private static void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
        try(DatagramChannel channel = DatagramChannel.open()){
            	
        	if(!handshake(channel, routerAddr, serverAddr)) {
        		System.out.println("Handshake failed! \nProgram terminating...");
        		return;
        	}
        	else {
        		System.out.println("Handshake success!");        		
        	}
        	
        	String userURL = "httpUDP get /";
            Packet p = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(1L)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(userURL.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddr);

            System.out.println("Sending \"" + userURL + "\" to router at " + routerAddr);

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            System.out.println("Waiting for the response");
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
                System.out.println("No response after timeout");
                return;
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            System.out.println("Packet: " + resp);
            System.out.println("Router: " + router);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            System.out.println("Payload: " + payload);

            keys.clear();
        }
    }

    private static boolean handshake(DatagramChannel channel, SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
    		/*	Packet Type: 0 - ACK
    		 * 				 1 - NAK
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
            if(keys.isEmpty()){
                System.out.println("No response after timeout");
                return false;
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
                        .setType(0)
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
    
	public static void parseURL(String urlLong) {
		
		if(urlLong.matches(".*httpUDP get?.*")) {
			getReq = true;
			
			if(urlLong.matches(".*-o.*")) {
				isTextOut = true;
				String temp = StringUtils.substringAfter(urlLong, "-o ");
				outputFileName = StringUtils.substringBefore(temp, " ");
			}
			if(urlLong.matches(".*-v.*")) {
				Get.setVerbose(true);
			}
			if(urlLong.matches(".*-h.*")) {
				Get.setHeaders(true);
				Get.addHeaders(urlLong);
			}
			return;
		}
//		else if(urlLong.matches(".*httpUDP post?.*")) {
//			postReq = true;
//			
//			if(urlLong.matches(".*-o.*")) {
//				isTextOut = true;
//				String temp = StringUtils.substringAfter(urlLong, "-o ");
//				outputFileName = StringUtils.substringBefore(temp, " ");
//			}
//			if(urlLong.matches(".*-v.*")) {
//				Post.setVerbose(true);
//			}
//			if(urlLong.matches(".*-h.*")) {
//				Post.setHeaders(true);
//				Post.addHeaders(urlLong);
//			}
//			if(urlLong.matches(".*-d.*")) {
//				Post.setInlineData(true);
//				Post.addData(urlLong);
//			}
//			if(urlLong.matches(".*-f.*")) {
//				Post.setIsFile(true);
//				Post.readFile(urlLong);
//			}
//		}
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

