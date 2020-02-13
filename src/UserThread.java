import java.io.*;
import java.net.*;
import java.util.Set;

import Exception.*;

public class UserThread extends Thread {
    private Socket clientSock;                                      //socket di comunicazione con il client
    private BufferedReader reader;                                  //bufferedReader per la lettura sulla socket client
    private BufferedWriter out;                                     //BufferedReader per la scrittura sulla socket del client
    private Database db;                                            //Istanza della classe Database per interagire con il database
    private String nickname;                                        //nickname dell'utente
    private Boolean alive;                                          //flag booleano per verificare l'attività del thread

    public UserThread(Socket client, Database database){
        this.clientSock=client;
        this.db=database;
        try {
            this.reader = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
            this.out=new BufferedWriter(new OutputStreamWriter(clientSock.getOutputStream()));
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public void run(){
        try {
            //Stringa su cui vengono appoggiate le richieste del client
            String request;
            alive=true;

            while (alive) {
                request = reader.readLine();
                //Suddivido la richiesta in sotto stringhe per fare il parsing
                String[] substring=request.split("\\s+");
                //Header che indicherà l'operazione da fare
                String header=substring[0];
                System.out.println(header);
                switch (header){
                    case "LOGIN":
                        nickname=substring[1];
                        if(login(nickname,substring[2])) {
                            //Salvo la socket TCP e la porta per la connessione UDP per la richiesta di sfida
                            db.setSocket(nickname, clientSock);
                            saveUDP();
                        }
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
                        friends();
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
            //Quando esco dal ciclo dopo aver fatto logout chiudo tutti i canali di comunicazione
            reader.close();
            out.close();
            clientSock.close();
        }catch(NullPointerException ne){
            if(db.contains(nickname)) {
                //Se il client crasha per qualche motivo in questo modo garantisco
                //all'utente di potersi connettere nuovamente quando vorrà
                db.logout(nickname);
                System.out.println(clientSock + " crashed, logout executed");
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * Metodo per l'invio al client di un messaggio
     * @param s messaggio che voglio inviare al client
     */
    public void sentResponse(String s){
        try {
            // Invio della risposta al client
            out.write(s);
            out.newLine();
            out.flush();
            System.out.println("Reply sent to " + clientSock);
        }catch (IOException ioe2){
            ioe2.printStackTrace();
        }
    }

    /**
     * Salva la porta UDP inserendola in una HashMap all'interno del database
     */
    public void saveUDP(){
        try {
            //Leggo il messaggio che mi viene mandato dal client dopo che il login è stato effettuato
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

    /**
     * Metodo di login
     * @param nickname nickname inviato dal client
     * @param password password inviata dal client
     * @return true se il login ha successo, false altrimenti
     */
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

    /**
     * Metodo per l'aggiunta di un nuovo amico
     * @param friend nickname dell'amico da aggiungere
     */
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

    /**
     * Metodo per la richiesta di visualizzazione della lista di amici
     */
    public void friends(){
        //prelevo dal db la lista di amici di nickname
        Set<String> friends=db.lista_amici(nickname);
        //parso in formato JSON ed invio la lista
        Parser parser=new Parser();
        String response=parser.parseToJSON(friends);
        sentResponse(response);
    }

    /**
     * Metodo per la richiesta di sfida ad un amico
     * @param friend_nick nickname dell'amico a cui deve essere inviata la richiesta di sfida
     */
    public void newGame(String friend_nick){
        //Controllo che ci sia veramente una relazione di amicizia
        if(!(db.lista_amici(nickname)).contains(friend_nick))
            sentResponse(friend_nick+" is not your friend");
        //controllo che l'utente da sfidare sia online
        else if(!db.userOnline(friend_nick))
            sentResponse("User is not online");
        //e che non sia impegnato in un'altra sfida
        else if(db.userBusy(friend_nick))
            sentResponse("User is busy in another game");
        else {
            //Prelevo dal db la porta UDP dell'amico a cui devo collegarmi per inviare la richiesta
            int port = db.getUDPport(friend_nick);
            try {
                //Setto la richiesta di sfida
                String s = "NEW game from " + nickname;
                byte[] request= s.getBytes();
                //Ottengo l'InetAddress e creo la socket UDP per mandare la richiesta di sfida
                InetAddress address = InetAddress.getByName("localhost");
                DatagramSocket reqSocket = new DatagramSocket();
                //Creo il datagramma, setto i dati e poi lo invio sulla socket UDP
                DatagramPacket packet = new DatagramPacket(request, request.length, address, port);
                packet.setData(request);
                reqSocket.send(packet);
                //Stampa di debug
                System.out.println(reqSocket.getLocalPort());

                //aspetto la risposta dell'amico
                byte[] response = new byte[1024];
                DatagramPacket resp_packet = new DatagramPacket(response, 1024);
                //imposto il timeout e nel caso in cui scatti so che l'amico non ha accettato la partita
                try {
                    reqSocket.setSoTimeout(30000);
                    reqSocket.receive(resp_packet);
                    String answer=new String(resp_packet.getData());
                    //controllo la risposta
                    if(answer.contains("yes")) {
                        sentResponse("Game accepted, it's starting");
                        //Setto i due utenti a busy così che non possano ricevere altre richieste di sfida
                        db.setBusy(nickname);
                        db.setBusy(friend_nick);
                        //Creo e avvio il thread per la gestione della partita
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
