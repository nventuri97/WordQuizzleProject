import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
    private static int TCPport=20546;
    private static int RMIport=20000;
    private static ThreadPoolExecutor executor;
    private static LinkedBlockingQueue<Runnable> queue;

    public static void main(String args[]){
        //Creo o recupero l'istanza dell'oggetto remoto così da permettere al client la registrazione
        File db=new File("./backup.json");
        Database database;
        Parser parser=new Parser();
        if(db.exists())
            database=parser.restoreDB();
        else
            database=new Database();
        try{
            //Esportazione dell'oggetto remoto
            DatabaseInterface stub=(DatabaseInterface) UnicastRemoteObject.exportObject(database, 0);
            LocateRegistry.createRegistry(RMIport);
            Registry r=LocateRegistry.getRegistry(RMIport);

            r.rebind(database.DatabaseName, stub);
        }catch(RemoteException ex){
            ex.printStackTrace();
        }
        queue=new LinkedBlockingQueue<>();
        executor=new ThreadPoolExecutor(50, 100, 320000, TimeUnit.MILLISECONDS, queue);
        System.out.println("Server online");

        try {
            //Socket passiva su cui mi metto in ascolto
            ServerSocket server = new ServerSocket(TCPport);
            while(true){
                //Accetto la richiesta di connessione e creo un thread utente dedicato
                Socket client=server.accept();
                UserThread u=new UserThread(client, database);
                executor.execute(u);
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
