import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Chat
{
    static HashMap<String, InetAddress> pessoas = new HashMap<>();
    static String meuApelido = "";

    public static void main(String[] args)
    {
        try(MulticastSocket mSocket = new MulticastSocket(6789))
        {
            DatagramSocket dSocket = new DatagramSocket(6799);
            Scanner scanner = new Scanner(System.in);
            InetAddress group = InetAddress.getByName("225.1.2.3");

            ReceiveMulticast receiveMulticast = new ReceiveMulticast(mSocket, dSocket);
            ReceiveUnicast receiveUnicast = new ReceiveUnicast(dSocket);
            receiveMulticast.start();
            receiveUnicast.start();

            System.out.println("Digite JOIN [apelido] para entrar no chat.");

            while(true) {
                String[] msg = scanner.nextLine().split("\\[|\\]");
                String buffer = "";
                switch(msg[0]) {
                    case "JOIN ": {
                        mSocket.joinGroup(group);
                        meuApelido = msg[1];
                        buffer = msg[0]+"["+msg[1]+"]";
                        DatagramPacket d = new DatagramPacket(buffer.getBytes(), buffer.length(),
                                                                group, 6789);
                        mSocket.send(d);
                        break;
                    }
                    case "MSG ": {
                        buffer = msg[0]+"["+msg[1]+"]"+msg[2];
                        DatagramPacket d = new DatagramPacket(buffer.getBytes(), buffer.length(),
                                                                group, 6789);
                        mSocket.send(d);
                        break;
                    }
                    case "MSGIDV TO ": {
                        buffer = "MSG FROM [" + Chat.meuApelido + "] " + msg[2]; 
                        DatagramPacket d = new DatagramPacket(buffer.getBytes(), buffer.length(),
                                                               Chat.pessoas.get(msg[1]) , 6799);
                        dSocket.send(d);
                        break;
                    }
                    case "LEAVE ": {
                        pessoas.remove(msg[1]);
                        System.out.println("Você saiu do grupo.");
                        buffer = msg[0]+"["+msg[1]+"]";
                        DatagramPacket d = new DatagramPacket(buffer.getBytes(), buffer.length(),
                                                                group, 6789);
                        dSocket.send(d);
                        mSocket.close();
                        break;
                    }
                    case "membros": {
                        for (String membro : pessoas.keySet()) {
                            System.out.println(membro);
                        }
                    }
                }

            }
        } catch(IOException ioe) {
            System.out.println("Erro: " + ioe.getMessage());
            System.exit(0);
        }
    }
}

class ReceiveMulticast extends Thread
{
    MulticastSocket m;
    DatagramSocket d;

    ReceiveMulticast(MulticastSocket m, DatagramSocket d) {
        this.m = m;
        this.d = d;
    }

    @Override
    public void run() { 
        byte[] buf = new byte[1024];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        while(true) {
            try {
                m.receive(p);
                String[] msg = new String(p.getData(), 0, p.getLength()).split("\\[|\\]");
                switch(msg[0]) {
                    case "JOIN ": {
                        Chat.pessoas.put(msg[1], p.getAddress());
                        System.out.println(msg[1] + " entrou no chat.");
                        String buffer = "JOINACK " + "[" + Chat.meuApelido +"]";
                        DatagramPacket p2 = new DatagramPacket(buffer.getBytes(), buffer.length(),
                                                                p.getAddress(), 6799);
                        d.send(p2);
                        break;
                    }
                    case "MSG ": {
                        System.out.println("[" + msg[1] + "]: " + msg[2]);
                        break;
                    }
                }
            } catch(IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        }
    }
}

class ReceiveUnicast extends Thread {

    DatagramSocket s;
    ReceiveUnicast(DatagramSocket s) {
        this.s = s;
    }

    @Override
    public void run() {
        byte[] buf = new byte[1024];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        try {
            while(true) {
                s.receive(p);
                String[] msg = new String(p.getData(), 0, p.getLength()).split("\\[|\\]");
                switch(msg[0]) {
                    case "JOINACK ": {
                        Chat.pessoas.put(msg[1], p.getAddress());
                        break;
                    }
                    case "MSG FROM ": {
                        System.out.println(msg[0] + "[" + msg[1] + "]" + msg[2]);
                        break;
                    }
                }
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
}