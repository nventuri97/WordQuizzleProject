package Client;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import Exception.*;
import Server.*;

public class ClientConnection {
    //Porte di default per la connesione TCP, UDP e RMI
    private int TCPport;                                        //Porta TCP del server a cui il client deve collegarsi
    private int RMIport;                                        //Porta RMI
    private int UDPport;                                        //Porta UDP su cui il client deve ricevere la notifica di sfida
    private Socket TCPSock;                                     //Socket TCP del client
    private String nick;                                        //Nickname dell'utente da inviare con la prima parola della sfida
    private SocketChannel GameSock;                             //Socket stabilita per la partita
    private UDPThread udpThread;                                //Thread UDP per la gestione delle notifiche
    private String msgAlert;                                    //messaggio per settare l'allert
    private boolean firstWord;                                  //Flag che indica se quella da inviare è o meno la prima parola
    private ClientGui cl;                                       //Istanza della classe main da passare all'UDP thread

    public ClientConnection(ClientGui client){
        //Genero una porta random così da non avere BindException al collegamento di più client
        this.TCPport=20546;
        this.RMIport=20000;
        this.msgAlert="";
        try {
            //Apro una socketchannel per instaurare la connessione TCP
            TCPSock = new Socket("localhost", TCPport);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        this.UDPport=TCPSock.getLocalPort();
        this.cl=client;
    }

    /**
     * procedura di registrazione lato client
     * @param nickname nickname con cui l'utente vuole registrarsi
     * @param password password che l'utente vuole utilizzare per registrarsi
     * @return true se la registrazione è avvenuta correttamente, false altrimenti
     */
    public boolean my_registration(String nickname, String password){
        if(nickname==null || nickname==""){
            setMsgAlert("Error 801: non valid nickname");
            return false;
        }

        if(password==null || password==""){
            setMsgAlert("Error 803: non valid password");
            return false;
        }
        DatabaseInterface s_obj;
        Remote r_obj;
        boolean result=false;

        try{
            Registry reg= LocateRegistry.getRegistry(RMIport);
            r_obj=reg.lookup("DatabaseService");
            s_obj=(DatabaseInterface) r_obj;

            //Metodo di registrazione RMI
            result=s_obj.registra_utente(nickname,password);

        }catch (NickAlreadyExistException ne){
            setMsgAlert("Error 805: nickname already used");
        }catch(NonValidPasswordException nne){
            setMsgAlert("Error 803: non valid password");
        }catch (Exception rme){
            rme.printStackTrace();
        }
        return result;
    }

    /**
     * Procedura di login lato client
     * @param nickname nickname con cui l'utente si è registrato
     * @param password password con cui l'utente si è registrato
     * @return true se il login è avvenuto correttamente, false altrimenti
     */
    public boolean my_log(String nickname, String password){
        if(nickname==null || nickname==""){
            setMsgAlert("Error 801: non valid nickname");
            return false;
        }

        if(password==null || password==""){
            setMsgAlert("Error 803: non valid password");
            return false;
        }
        //Setto la richiesta per il login
        String request = "LOGIN " +nickname+ " "+password;
        sendRequest(request);
        nick=nickname;

        String answer=receiveResponse();
        //Controllo che ci sia il codice di avvenuto login
        if (answer.contains("806")) {
            //Avvio il thread UDP e comunico la porta di ascolto al server
            udpThread = new UDPThread(UDPport, cl);
            udpThread.start();
            sendRequest("UDPport " + UDPport);
            return true;
        } else {
            //altrimenti stampo l'errore
            setMsgAlert(answer);
            return false;
        }
    }

    /**
     * Esegue il logout dell'utente
     * @return true se il logout è avvenuto correttamente, false altrimenti
     */
    public boolean my_logout(){
        String request="LOGOUT";
        sendRequest(request);

        String answer=receiveResponse();
        //Se la risposta del server è diversa da OK allora vuol dire che qualcosa è andato storto
        if(!answer.contains("OK")){
            setMsgAlert("Error 815: Logout went wrong, try again");
            return false;
        }
        udpThread.setRunning();
        try {
            TCPSock.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return true;
    }

    /**
     * Aggiuge l'amico selezionato
     * @param nickAmico nome dell'utente da aggiungere alle amicizie
     * @return risposta del server se positiva, null altrimenti
     */
    public String addFriends(String nickAmico){
        if(nickAmico==null || nickAmico=="")
            setMsgAlert("Error 801: non valid nickname");
        else{
            //Compongo la richiesta TCP
            String request="ADD "+nickAmico;

            sendRequest(request);
            //Mi metto in attesa della risposta del server
            String answer=receiveResponse();
            if(answer.contains("810")){
                return ("Friend "+nickAmico+" is been correctly added");
            } else {
                setMsgAlert(answer);
            }
        }
        return null;
    }

    /**
     * Richiede al server la lista degli amici dell'utente
     * @return la lista di amici dell'utente
     */
    public String showFriends(){
        String request="SHOWfriends";
        sendRequest(request);

        //Mi metto in attesa della risposta dal server
        String answer=receiveResponse();
        //restituisco la stringa in formato JSON che verrà parsata nel GUIThread
        return answer;
    }

    /**
     * Richiede al server il punteggio dell'utente
     * @return il punteggio totalizzato dall'utente
     */
    public String score(){
        String request="SHOWscore";
        sendRequest(request);

        //Mi metto in attesa della risposta dal server
        String answer=receiveResponse();
        return answer;
    }

    /**
     * Richiede al server la classifica relativa all'utente
     * @return restituisce la classifica dell'utente
     */
    public String my_ranking(){
        String request="SHOWranking";
        sendRequest(request);

        //Mi metto in attesa della risposta dal server
        String answer=receiveResponse();
        //restituisco ad operation la stringa in formato JSON che verrà parsata nel GUIThread
        return answer;
    }

    public String newGame(String friend){
        if(friend==null || friend=="") {
            setMsgAlert("Error 801: non valid nickname");
            return null;
        }
        String request="NEW game against " + friend;
        //Invio la richiesta al server
        sendRequest(request);
        //Mi metto in attesa della risposta dal server
        String answer = receiveResponse();
        if(answer.contains("Error"))
            setMsgAlert(answer);
        answer="";
        return answer;
    }

    /**
     * Invia le richieste al server
     * @param request richiesta da inviare al server
     */
    public void sendRequest(String request){
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(TCPSock.getOutputStream()));
            writer.write(request);
            writer.newLine();
            writer.flush();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * riceve le risposte dal server
     * @return risposta ricevuta dal server
     */
    public String receiveResponse(){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(TCPSock.getInputStream()));
            return reader.readLine();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     * Imposta un messaggio per sollevare un nuovo Allert
     * @param msg messaggio da inserire come testo dell'Allert
     */
    public synchronized void setMsgAlert(String msg){
        //pulisco dal messaggio precedente
        msgAlert="";
        msgAlert=msg;
    }

    /**
     * Restituisce il messaggio da inserire nell'Allert
     * @return msgAlert testo dell'Allert
     */
    public String getMsgAlert(){
        return msgAlert;
    }

    /**
     * Setta la nuova connessione per la partita con GameSocket
     */
    public void newGameConnection(){
        //Stampa di debug
        System.out.println("Aspetto la porta");
        String answer=receiveResponse();
        String[] substring=answer.split("\\s+");

        int port=Integer.parseInt(substring[2]);
        //Stringa di debug
        System.out.println(port);
        try {
            GameSock = SocketChannel.open(new InetSocketAddress("localhost", port));
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        firstWord=true;
    }

    /**
     * riceve la nuova parola dal GameThread
     * @return stringa contenente la parola ricevuta
     */
    public String receiveNewWord(){
        ByteBuffer buffer=ByteBuffer.allocate(1024);
        buffer.clear();
        try {
            GameSock.read(buffer);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        buffer.flip();
        String word= StandardCharsets.UTF_8.decode(buffer).toString();
        return word;
    }

    /**
     * Invia la parola tradotta al GameThread
     * @param word parola tradotta
     */
    public void sendNewWord(String word){
        ByteBuffer buffer;
        String message="";
        if(firstWord){
            message=nick+" "+word;
            buffer=ByteBuffer.wrap(message.getBytes());
            firstWord=false;
        }else
            buffer=ByteBuffer.wrap(word.getBytes());
        int len=0;
        try {
            len=GameSock.write(buffer);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        //Stampa di debug
        System.out.println("Ho inviato "+len+" di "+message);
    }
}