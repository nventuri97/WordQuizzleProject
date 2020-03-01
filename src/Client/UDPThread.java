package Client;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.io.IOException;
import java.net.*;
import java.util.Optional;

public class UDPThread extends Thread {
    private static int UDPport;                                     //Porta UDP su cui il client è in ascolto
    private static DatagramSocket UDPSock;                          //Socket UDP
    private static boolean running;                                 //Flag booleano per verificare se il thread sta lavorando o meno
    private static DatagramPacket packet;                           //Datagramma UDP per la risposta alla notifica
    private static int frport;                                      //Porta a cui rispondere in UDP
    private static InetAddress friend;                              //InetAddress da cui ho ricevuto il datagramma di notifica
    private static boolean accepted;                                //flag per l'avvio dell'interfaccia
    private static ClientGui clientGui;                             //Istanza da passare alla classe di caricamente dell'interfaccia

    public UDPThread(int port, ClientGui client){
        this.UDPport=port;
        try{
            this.UDPSock=new DatagramSocket(UDPport);
        }catch (SocketException se){
            se.printStackTrace();
        }
        this.running=true;
        this.accepted=false;
        this.clientGui=client;
    }

    @Override
    public void run(){
        while(running){
            byte[] request=new byte[1024];
            packet=new DatagramPacket(request, 1024);
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
            //Prendo dal pacchetto le informazioni necessarie a rispondere
            frport=packet.getPort();
            friend = packet.getAddress();

            //Visualizzo in grafica la notifica
            Platform.runLater(new Runnable() {
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
                        setResponse("yes");
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("./GUI/Main.fxml"));
                            Parent root = loader.load();
                            MainController controller = loader.getController();
                            controller.waitTime(clientGui);
                        }catch (IOException ioe){
                            ioe.printStackTrace();
                        }
                    } else
                        setResponse("no");

                    Thread t=new Thread(()->{
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        if (notify.isShowing()) {
                            notify.close();
                            setResponse("no");
                        }
                    });
                    t.start();
                }
            });
        }
    }

    /**
     * Ferma l'UDPThread quando il client esegue il logout
     */
    public static void setRunning(){
        running=false;
    }

    /**
     * invia la risposta al thread che ha inviato la notifica di sfida
     * @param s risposta da inviare
     */
    public static void setResponse(String s){
        if(s!=null) {
            byte[] data = s.getBytes();
            DatagramPacket response = new DatagramPacket(data, data.length, friend, frport);
            response.setData(data);
            try {
                UDPSock.send(response);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
