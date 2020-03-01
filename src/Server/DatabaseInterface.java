package Server;
import java.rmi.Remote;
import java.rmi.RemoteException;
import Exception.*;

public interface DatabaseInterface extends Remote{
    public static final String DatabaseName="DatabaseService";
    /**
     * Inserimento di un nuovo utente nel database
     * @param nickname nickname utilizzato dall'utente
     * @param password password in chiaro dell'utente
     * @throws RemoteException
     */
    public boolean registra_utente(String nickname, String password) throws RemoteException, NickAlreadyExistException, NonValidPasswordException;
}