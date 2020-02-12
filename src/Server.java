import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private static int TCPport=20546;
    private static int RMIport=20000;

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

        try {
            //Socket passiva su cui mi metto in ascolto
            ServerSocket server = new ServerSocket(TCPport);
            while(true){
                Socket client=server.accept();
                UserThread u=new UserThread(client, database);
                u.start();
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
