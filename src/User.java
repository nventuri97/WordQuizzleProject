import java.util.HashSet;
import java.util.Set;

public class User {

    private String nickname;
    private String password;
    private Set<String> friends;
    private int punteggio;
    private transient boolean online;
    private transient boolean busy;

    /**
     * Costruttore con parametri
     * @param nickname nickname inserito dall'utente
     * @param password password crittografata con l'algoritmo BCrypt
     */
    public User(String nickname, String password){
        this.nickname=nickname;
        this.password=password;
        this.friends=new HashSet<>();
        this.punteggio=0;
        this.online=false;
        this.busy=false;
    }

    /**
     * Restituisce il nickname dell'utente
     * @return il nickname
     */
    public String getNick(){
        return nickname;
    }

    /**
     * Restituisce la password dell'utente utile per il login
     * @return password utente
     */
    public String getPassword(){
        return password;
    }

    /**
     * Restituisce il punteggio dell'utente
     * @return punteggio utente
     */
    public int getPunteggio(){return punteggio;}

    /**
     * Aggiunge i punti vinti in una partita al punteggio totale dell'utente
     * @param punti
     */
    public void addPunteggio(int punti){punteggio+=punti;}

    /**
     * Restituisce true se l'utente è online, false altrimenti
     * @return il valore di online
     */
    public boolean isOnline(){
        return online;
    }

    /**
     * Cambia il valore del campo online, se true setta a false e viceversa
     */
    public void setOnline(){
        online=!online;
    }

    /**
     * Restituisce true se l'utente è impegnato in una partita con un altro giocatore
     * @return il valore di busy
     */
    public boolean isBusy(){
        return busy;
    }

    /**
     * Cambia il valore del campo busy, se true setta a false e viceversa
     */
    public void setBusy(){
        busy=!busy;
    }

    /**
     * Restituisce le amicizie dell'utente
     * @return Set di stringhe contenente le amicizie dell'utente
     */
    public Set<String> getFriends(){
        return friends;
    }

    /**
     * Sostituisce la lista di amici già presente con quella nuova
     * @param friends
     */
    public void setFriends(Set<String> friends){
        this.friends=friends;
    }
}
