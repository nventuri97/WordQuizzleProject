import java.io.*;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import Exception.*;

public class ClientConnection {
    //Porte di default per la connesione TCP, UDP e RMI
    private int TCPport;
    private int RMIport;
    private Socket TCPSock;
    private UDPThread udpThread;
    private int UDPport;
    private String msgAlert;
    private String additionalMsg;

    public ClientConnection(){
        //Genero una porta random così da non avere BindException al collegamento di più client
        this.TCPport=20546;
        this.RMIport=20000;
        this.msgAlert="";
        this.additionalMsg="";
        try {
            //Apro una socketchannel per instaurare la connessione TCP
            TCPSock = new Socket("localhost", TCPport);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        this.UDPport=TCPSock.getLocalPort();
    }

    /**
     * procedura di registrazione lato client
     * @return true se la registrazione è avvenuta correttamente, false altrimenti
     */
    public boolean my_registration(String nickname, String password){
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
            setMsgAlert("Error 800: nickname already used");
        }catch(NonValidPasswordException nne){
            setMsgAlert("Error 810: non valid password");
        }catch (Exception rme){
            rme.printStackTrace();
        }
        return result;
    }

    /**
     * Procedura di login lato client
     * @return true se il login è avvenuto correttamente false altrimenti
     */
    public boolean my_log(String nickname, String password){
        //Setto la richiesta per il login
        String request = "LOGIN " +nickname+ " "+password;
        sendRequest(request);

        String answer=receiveResponse();
        //Controllo che ci sia il codice di avvenuto login
        if (answer.contains("505")) {
            udpThread = new UDPThread(UDPport);
            udpThread.start();
            sendRequest("UDPport " + UDPport);
            return true;
        } else {
            //altrimenti stampo l'errore
            setMsgAlert(answer);
            return false;
        }
    }

    public boolean my_logout(){
        String request="LOGOUT";
        sendRequest(request);

        String answer=receiveResponse();
        //Se la risposta del server è diversa da OK allora vuol dire che qualcosa è andato storto
        if(!answer.contains("OK")){
            setMsgAlert("Something went wrong, try again");
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

    public void addFriends(String nickAmico){
        //Compongo la richiesta TCP
        String request="ADD "+nickAmico;

        sendRequest(request);
        //Mi metto in attesa della risposta del server
        String answer=receiveResponse();
        if(answer.contains("510")){
            setAdditionalMsg("Friend "+nickAmico+" is been correctly added");
        } else {
            setMsgAlert(answer);
        }
    }

    public String showFriends(){
        String request="SHOWfriends";
        sendRequest(request);

        //Mi metto in attesa della risposta dal server
        String answer=receiveResponse();
        //restituisco la stringa in formato JSON che verrà parsata nel GUIThread
        return answer;
    }

    public void score(){
        String request="SHOWscore";
        sendRequest(request);

        //Mi metto in attesa della risposta dal server
        String answer=receiveResponse();
        setAdditionalMsg(answer);
    }

    public void my_ranking(){
        String request="SHOWranking";
        sendRequest(request);

        //Mi metto in attesa della risposta dal server
        String answer=receiveResponse();
        //restituisco ad operation la stringa in formato JSON che verrà parsata nel GUIThread
        setAdditionalMsg(answer);
    }

    public void newGame(String friend){
        String request="NEW game against " + friend;
        //Invio la richiesta al server
        sendRequest(request);
        //Mi metto in attesa della risposta dal server
        String answer = receiveResponse();
        setAdditionalMsg(answer);
    }

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
     * Aggiunge un messaggio opzionale all'operazione (ad esempio password, nickname o amico)
     * @param msg
     */
    public synchronized void setAdditionalMsg(String msg){
        //pulisco dal messaggio precedente
        additionalMsg="";
        additionalMsg=msg;
    }

    /**
     * Restituisce il messaggio opzionale
     * @return additionalMsg messaggio opzionale
     */
    public String getAdditionalMsg(){
        return additionalMsg;
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
}