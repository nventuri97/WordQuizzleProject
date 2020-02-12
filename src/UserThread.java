import java.io.*;
import java.net.*;
import java.util.Set;

import Exception.*;

public class UserThread extends Thread {
    private Socket clientSock;
    private BufferedReader reader;
    private BufferedWriter out;
    private Database db;
    private String nickname;
    private Boolean alive;
    private Parser parser;

    public UserThread(Socket client, Database database){
        this.clientSock=client;
        this.db=database;
        try {
            this.reader = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public void run(){
        try {
            String request;
            alive=true;

            while (alive) {
                request = reader.readLine();
                String[] substring=request.split("\\s+");
                String header=substring[0];
                System.out.println(header);
                switch (header){
                    case "LOGIN":
                        login(substring[1],substring[2]);
                        nickname=substring[1];
                        db.setSocket(nickname, clientSock);
                        saveUDP();
                        break;
                    case "LOGOUT":
                        db.logout(nickname);
                        sentResponse("OK");
                        alive=false;
                        break;
                    case "ADD":
                        newFriends(substring[1]);
                        break;
                    case "SHOWfriends":
                        sendFriends();
                        break;
                    case "SHOWscore":
                        String score=String.valueOf(db.mostra_punteggio(nickname));
                        sentResponse(score);
                        break;
                    case "SHOWranking":
                        sentResponse(db.mostra_classifica(nickname));
                        break;
                    case "NEW":
                        String friend=substring[3];
                        newGame(friend);
                        break;
                    default:
                        break;
                }
            }
            reader.close();
            out.close();
            clientSock.close();
        }catch(NullPointerException ne){
            //Se il client crasha per qualche motivo in questo modo garantisco
            //all'utente di potersi connettere nuovamente quando vorrà
            db.logout(nickname);
            System.out.println(clientSock+" crashed, logout executed");
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    public void sentResponse(String s){
        try {
            out=new BufferedWriter(new OutputStreamWriter(clientSock.getOutputStream()));
            // Invio della risposta al client
            out.write(s);
            out.newLine();
            out.flush();
            System.out.println("Reply sent to " + clientSock);
        }catch (IOException ioe2){
            ioe2.printStackTrace();
        }
    }

    public void saveUDP(){
        try {
            String request = reader.readLine();
            String subs[]=request.split("\\s+");
            if(subs[0].equals("UDPport")) {
                int port=Integer.parseInt(subs[1]);
                db.setUDPmap(nickname, port);
                System.out.println("UDPport of client "+clientSock+" saved");
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    public boolean login(String nickname, String password) {
        try {
            if (db.login(nickname, password)) {
                sentResponse("505 Successful Login");
                return true;
            }
        } catch(IsLoggedException ile){
            sentResponse("501 User already logged");
        } catch(NonValidPasswordException npe){
            sentResponse("502 Non valid password");
        } catch(InvalidNickException ine){
            sentResponse("503 Wrong nickname");
        }
        return false;
    }

    public void newFriends(String friend){
        try {
            db.aggiungi_amico(nickname, friend);
            sentResponse("510 Friend correctly added");
        }catch(InvalidNickException ine){
            sentResponse("511 This friend didn't join WordQuizzle");
        }catch(FriendshipException fe){
            sentResponse("512 Friendship already added");
        }
    }

    public void sendFriends(){
        Set<String> friends=db.lista_amici(nickname);
        Parser parser=new Parser();
        String response=parser.parseToJSON(friends);
        sentResponse(response);
    }

    public void newGame(String friend_nick){
        if(!db.userOnline(friend_nick))
            sentResponse("User is not online");
        else if(db.userBusy(friend_nick))
            sentResponse("User is busy in another game");
        else {
            int port = db.getUDPport(friend_nick);
            try {
                byte[] request;
                String s = "NEW game from " + nickname;
                InetAddress address = InetAddress.getByName("localhost");
                DatagramSocket reqSocket = new DatagramSocket();
                request = s.getBytes();
                DatagramPacket packet = new DatagramPacket(request, request.length, address, port);
                packet.setData(request);
                reqSocket.send(packet);
                System.out.println(reqSocket.getLocalPort());

                //aspetto la risposta dell'amico
                byte[] response = new byte[1024];
                DatagramPacket resp_packet = new DatagramPacket(response, 1024);
                //imposto il timeout e nel caso in cui scatti so che l'amico non ha accettato la partita
                try {
                    reqSocket.setSoTimeout(30000);
                    reqSocket.receive(resp_packet);
                    String answer=new String(resp_packet.getData());
                    System.out.println(answer);
                    if(answer.contains("yes")) {
                        sentResponse("Game accepted, it's starting");
                        GameThread gt=new GameThread(db, nickname, friend_nick);
                        gt.start();
                    } else
                        sentResponse("Game denied");
                } catch (SocketTimeoutException ste) {
                    sentResponse("Game denied");
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
