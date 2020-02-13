import com.google.gson.Gson;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameThread extends Thread {
    private Database database;                                      //Istanza della classe database passata dal thread utente che invia la sfida
    private String gamer1;                                          //Nickname del primo giocatore
    private String gamer2;                                          //Nickname del secondo giocatore
    private int k;                                                  //Numero di parole da inviare nella sfida, un intero tra 1 e 12
    private Gson gson;                                              //Struttura per il parsing del dizionario da JSON
    private FileReader reader;                                      //FileReader per leggere il file del dizionario
    private Socket sock1, sock2;                                    //Socket dei due giocatori
    private HashMap<String, String> translation;                    //HashMap contenente le traduzioni delle K parole scelte

    public GameThread(Database db, String nick1, String nick2){
        this.k=(int) (Math.random()*11)+1;
        this.database=db;
        this.gamer1=nick1;
        this.gamer2=nick2;
        this.translation=new HashMap<>(k);
        this.gson=new Gson();
        try {
            this.reader = new FileReader("./dizionario.json");
        }catch(FileNotFoundException fe){
            fe.printStackTrace();
        }
        sock1=db.getSocket(gamer1);
        sock2=db.getSocket(gamer2);
    }

    @Override
    public void run(){
        //Creo un'ArrayList contenente il dizionario e poi seleziono le K parole
        ArrayList<String> dictionary=gson.fromJson(reader, ArrayList.class);
        ArrayList<String> kparole=getKWord(dictionary, k);
        //Se la traduzione va a buon fine faccio cominciare la sfida
        if(getTranslation(translation,kparole,k)){
            User us1=database.getUser(gamer1);
            User us2=database.getUser(gamer2);
            int pt1=game(us1, sock1);
            int pt2=game(us2, sock2);
            if(pt1>pt2){
                sendMessage("You won "+pt1+" to "+pt2+" Receive 3 bonus point",sock1);
                sendMessage("You lose "+pt1+" to "+pt2,sock2);
                pt1+=3;
            } else if(pt2>pt1){
                sendMessage("You won "+pt2+" to "+pt1+" Receive 3 bonus point",sock2);
                sendMessage("You lose "+pt2+" to "+pt1,sock1);
                pt2+=3;
            } else {
                sendMessage("You drew "+pt2+" to "+pt1,sock1);
                sendMessage("You drew "+pt2+" to "+pt1,sock2);
            }
            us1.addPunteggio(pt1);
            us2.addPunteggio(pt2);
        }
    }

    /**
     * Selezione K parole casuali e distinte dal dizionario e le inserisce nell'ArrayList
     * @param dic Dizionario da cui prendere le parole
     * @param k numero di parole da prelevare
     * @return ArrayList contenente le parole selezionate
     */
    public ArrayList<String> getKWord(ArrayList<String> dic, int k){
        ArrayList<String> s=new ArrayList<>(k);
        int i=0;
        int len=dic.size();
        while(i<k){
            int rand=(int) (Math.random()*(len-1));
            String word=dic.get(rand);
            if(!s.contains(word)) {
                s.add(word);
                i++;
            }
        }
        return s;
    }

    /**
     * Metodo che genera la traduzione delle parole selezionate tramite richieste HTTP GET
     * @param t HashMap in cui inserire la traduzione
     * @param kparole ArrayList contenente le k parole
     * @param k numero di parole
     * @return true se la traduzione è arrivata a buon fine, false altrimenti
     */
    public boolean getTranslation(HashMap<String, String> t, ArrayList<String> kparole, int k){
        for(int i=0;i<k;i++) {
            try {
                String word=kparole.get(i);
                URL site = new URL("https://api.mymemory.translated.net/get?q="+word+"&langpair=it|en");
                HttpURLConnection connection=(HttpURLConnection) site.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String translation=reader.readLine();
                if(connection.getResponseCode()!=200){
                    sendMessage("Something is gone wrong, we are sorry", sock1);
                    sendMessage("Something is gone wrong, we are sorry", sock2);
                    return false;
                }else
                    t.put(word, translation);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Metodo per inviare messaggi/parole
     * @param message messaggio da inviare al client
     * @param socket socket di comunicazione
     */
    public void sendMessage(String message, Socket socket){
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(message);
            writer.newLine();
            writer.flush();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * metodo per la ricezione di risposte dal client
     * @param socket socket di ricezione
     * @return messaggio ricevuto
     */
    public String receiveResponse(Socket socket){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return reader.readLine();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     * Invia al client le parole da tradurre e assegna il punteggio
     * @param user struttura dell'utente per l'aggiornamento del punteggio
     * @param sock socket di comunicazione
     * @return punteggio totalizzato dall'utente
     */
    public int game(User user, Socket sock){
        int punti=user.getPunteggio();
        String original,transl;
        for(Map.Entry<String,String> entry: translation.entrySet()){
            original=entry.getKey();
            sendMessage(original, sock);
            transl=receiveResponse(sock);
            if(transl.equals(entry.getValue()))
                punti++;
            else
                punti--;
        }
        return punti;
    }
}
