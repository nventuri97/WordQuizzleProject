import java.io.Serializable;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import Exception.*;

public class Database extends RemoteServer implements DatabaseInterface, Serializable {
    private HashMap<String, User> database;
    //private BCryptPasswordEncoder encoder;
    private transient Parser parser;
    private transient HashMap<String, Integer> UDPmap;

    public Database(){
        this.database=new HashMap<>();
        this.UDPmap=new HashMap<>();
        //this.encoder=new BCryptPasswordEncoder();
    }

    @Override
    public synchronized boolean registra_utente(String nickname, String password) throws RemoteException, NickAlreadyExistException, NonValidPasswordException {
        if(database.containsKey(nickname))
            throw new NickAlreadyExistException();

        if(password==" ")
            throw new NonValidPasswordException();

        //Creo l'istanza User, inserisco l'instanza dentro la HashMap
        User user=new User(nickname,password /*encoder.encode(password)*/);
        database.put(nickname, user);
        //Salvo le modifiche al database nel file JSON
        parser=new Parser();
        parser.saveDB(this);
        return true;
    }

    /**
     * Procedura di login al gioco WordQuizzle
     * @param nickname nickname utilizzato dall'utente che vuole effettuare l'operazione
     * @param password password in chiaro dell'utente che vuole effettuare l'operazione
     * @throws IsLoggedException, NonValidPasswordException, InvalidNickException
     * @return true se l'utente è loggato, false altrimenti
     */
    public synchronized boolean login(String nickname, String password) throws IsLoggedException, NonValidPasswordException, InvalidNickException {
        //Controllo che l'utente abbia inserito il nickname giusto
        if(!database.containsKey(nickname))
            throw new InvalidNickException();

        //Prendo l'istanza di nickname dal database per poterci lavorare sopra
        User us=database.get(nickname);
        //Contorllo che l'utente non sia già loggato
        if(us.isOnline())
            throw new IsLoggedException();

        //Verifico che la password inserita sia quella giusta
        if(!/*encoder.encode(password)*/password.equals(us.getPassword()))
            throw new NonValidPasswordException();

        //Setto il flag online così da poter controllare che l'utente sia veramente online
        us.setOnline();
        database.put(nickname, us);
        //Aggiorno il file del database
        parser=new Parser();
        parser.saveDB(this);
        return true;
    }

    /**
     * Procedura di logout
     * @param nickname nickname utilizzato dall'utente che vuole effettuare l'operazione
     */
    public synchronized void logout(String nickname){
        //Prendo l'istanza utente relativa al nickname
        User us=database.get(nickname);
        //modifico il valore del parametro online
        us.setOnline();
        //reinserisco nel database l'istanza con il valore aggiornato
        database.put(nickname,us);
        parser=new Parser();
        parser.saveDB(this);
    }


    /**
     * Aggiunge un amico alla cerchia di amici dell'utente
     * @param nickUtente nickname utilizzato dall'utente che vuole effettuare l'operazione
     * @param nickAmico nickname dell'amico che l'utente vuole aggiungere
     * @throws InvalidNickException, FriendshipException
     */
    public synchronized void aggiungi_amico(String nickUtente, String nickAmico) throws InvalidNickException, FriendshipException {
        //Controllo che nickAmico sia nel database e che nickAmico non sia uguale a nickUtente
        if(!database.containsKey(nickAmico) || nickUtente.equals(nickAmico))
            throw new InvalidNickException();

        //prelevo le due istanze utente dal database
        User us=database.get(nickUtente);
        User friend=database.get(nickAmico);
        //creo la lista di amici e controllo che nickAmico non sia già presente
        Set<String> friendship=us.getFriends();
        if(friendship.contains(nickAmico))
            throw new  FriendshipException();

        //Aggiungo prima a nickUtente
        friendship.add(nickAmico);
        us.setFriends(friendship);

        //poi aggiungo a nickAmico
        Set<String> friendsOfMine=friend.getFriends();
        friendsOfMine.add(nickUtente);
        friend.setFriends(friendsOfMine);
        parser=new Parser();
        parser.saveDB(this);
    }

    /**
     * Restituisce la lista di amici dell'utente
     * @param nickname nickname utilizzato dall'utente che vuole effettuare l'operazione
     * @return l'insieme degli amici dell'utente
     */
    public Set<String> lista_amici(String nickname){
        User us=database.get(nickname);
        return us.getFriends();
    }

    /**
     * Restituisce il punteggio totale dell'utente
     * @param nickname nickname utilizzato dall'utente che vuole effettuare l'operazione
     * @return il punteggio totale di nickname
     */
    public int mostra_punteggio(String nickname){
        User us=database.get(nickname);
        return  us.getPunteggio();
    }

    /**
     * Restituisce la classifica dell'utente
     * @param nickname nickname utilizzato dall'utente che vuole effettuare l'operazione
     * @return stringa in formato JSON contenente la classifica dell'utente
     */
    public String mostra_classifica(String nickname){
        //Prelevo l'utente dal database
        User us=database.get(nickname);
        //Creo una lista di entry per crearmi le coppie <nomeUtente, punteggio>
        List<Map.Entry<String, Integer>> classifica=new ArrayList<>();
        Set<String> amici=this.lista_amici(nickname);
        //Inserisco la prima entry che contiene il nome dell'utente
        Map.Entry<String, Integer> entry=new AbstractMap.SimpleEntry(nickname, us.getPunteggio());
        classifica.add(entry);
        //inserisco le altre entry contenenti gli amici dell'utente
        for(String s: amici){
            User user=database.get(s);
            int punt=user.getPunteggio();
            Map.Entry<String, Integer> ent=new AbstractMap.SimpleEntry(s, punt);
            classifica.add(ent);
        }
        classifica.sort(Map.Entry.comparingByValue());
        String jsonString=parser.parseToJSON(classifica);
        System.out.println(jsonString);
        return jsonString;
    }

    public synchronized User getUser(String nickname){
        return database.get(nickname);
    }

    public synchronized void setUDPmap(String nickname, int port){
        UDPmap.put(nickname, port);
    }

    public int getUDPport(String nickname){
        return UDPmap.get(nickname);
    }

    public boolean userOnline(String nickname){
        User us=database.get(nickname);
        return us.isOnline();
    }

    public boolean userBusy(String nickname){
        User us=database.get(nickname);
        return us.isBusy();
    }

    public void setSocket(String nickname, Socket socket){
        User us=database.get(nickname);
        us.setTCPSocket(socket);
    }

    public Socket getSocket(String nickname){
        User us=database.get(nickname);
        return us.getTCPSocket();
    }
}
