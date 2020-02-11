import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class UDPThread extends Thread {
    private static int UDPport;
    private static DatagramSocket UDPSock;
    private static boolean running;
    private static boolean working;
    private static String answer;

    public UDPThread(int port){
        this.UDPport=port;
        try{
            this.UDPSock=new DatagramSocket(UDPport);
        }catch (SocketException se){
            se.printStackTrace();
        }
        this.working=false;
        this.running=true;
    }

    @Override
    public void run(){
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
        while(running){
            byte[] request=new byte[1024];
            DatagramPacket packet=new DatagramPacket(request, 1024);
            try {
                UDPSock.setSoTimeout(2000);
                UDPSock.receive(packet);
            }catch(SocketTimeoutException ioe){
                if(!running)
                    break;
                else
                    continue;
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
            //Ricostruisco la stringa inviata dal thread dell'utente che chiede la partita
            String source=new String(packet.getData());
            String[] substring=source.split("\\s+");
            Timer timer=new Timer();
            Runnable notifier=new Runnable(){
                @Override
                public void run() {
                    Alert notify=new Alert(Alert.AlertType.INFORMATION);
                    notify.setTitle("Challenge request");
                    notify.setHeaderText(substring[3]+" ask you to join a new match");
                    notify.setContentText("Answer accept/deny: ");
                    ButtonType accept = new ButtonType("Accept");
                    ButtonType deny = new ButtonType("Deny");

                    notify.getButtonTypes().setAll(accept,deny);
                    Optional<ButtonType> result = notify.showAndWait();

                    if(result.get()==accept){
                        answer="yes";
                    } else
                        answer="no";
                }
            };
            timer.schedule((TimerTask) notifier, 30000);
            System.out.println(answer);
            Platform.runLater(notifier);
            //Costruisco il messaggio di risposta da inviare via datagrampacket
            InetAddress friend=packet.getAddress();
            int frport=packet.getPort();
            byte[] data=answer.getBytes();
            DatagramPacket response=new DatagramPacket(data, data.length, friend, frport);
            response.setData(data);
            try {
                UDPSock.send(response);
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    public static void setRunning(){
        running=false;
    }

    public static synchronized void setFlag(){working=true;}

    public static synchronized boolean isWorking(){
        return working;
    }
}
