import com.google.gson.Gson;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GameThread extends Thread {
    private Database database;                                      //Istanza della classe database passata dal thread utente che invia la sfida
    private String gamer1;                                          //Nickname del primo giocatore
    private String gamer2;                                          //Nickname del secondo giocatore
    private int k, ind1, ind2;                                      //Numero di parole da inviare nella sfida, un intero tra 1 e 12, ind1 e ind2 indici della parola inviata
    private Gson gson;                                              //Struttura per il parsing del dizionario da JSON
    private FileReader reader;                                      //FileReader per leggere il file del dizionario
    private Socket sock1, sock2;                                    //Socket dei due giocatori
    private ArrayList<String> kparole;                              //ArrayList contenente le K parole scelte a caso dal dizionario
    private ArrayList<String> translation;                          //ArrayList contenente le traduzioni delle K parole scelte
    private ServerSocket gameSock;                                  //Server socket che gestisce la sfida
    private Selector selector;                                      //Selettore per la gestione dei due client
    private int[] punti;                                            //Array di interi per i punteggi

    public GameThread(Database db, String nick1, String nick2, ServerSocket ssocket){
        this.k=(int) (Math.random()*11)+1;
        this.database=db;
        this.gamer1=nick1;
        this.gamer2=nick2;
        this.translation=new ArrayList<>(k);
        this.gson=new Gson();
        try {
            this.reader = new FileReader("./dizionario.json");
            this.selector=Selector.open();
        }catch(IOException fe){
            fe.printStackTrace();
        }
        this.sock1=db.getSocket(gamer1);
        this.sock2=db.getSocket(gamer2);
        this.gameSock=ssocket;
        this.punti=new int[2];
    }

    @Override
    public void run(){

        //Creo un'ArrayList contenente il dizionario e poi seleziono le K parole
        ArrayList<String> dictionary=gson.fromJson(reader, ArrayList.class);
        kparole=getKWord(dictionary, k);
        getTranslation(translation, kparole, k);

        //Prelevo le due istanze della classe User dal database
        User us1 = database.getUser(gamer1);
        User us2 = database.getUser(gamer2);
        punti[0]=0;
        punti[1]=0;
        ind1=0;
        ind2=0;

        //Prendo la nuova porta su cui è aperta la gameSocket e la invio ai due client
        int newPort=gameSock.getLocalPort();
        sendMessage("Game port "+newPort, sock1);
        sendMessage("Game port "+newPort, sock2);


        Set<SelectionKey> readyKeys= selector.selectedKeys();
        Iterator<SelectionKey> iterator=readyKeys.iterator();
        while(iterator.hasNext()) {
            SelectionKey key=iterator.next();
            iterator.remove();

            try {
                if(key.isAcceptable()){
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        //Creo una active socket derivata dalla accept sulla passive socket su cui il server è in ascolto
                        SocketChannel client = server.accept();
                        //Setto a non-blocking
                        client.configureBlocking(false);
                        //Aggiungo la key del client
                        SelectionKey key2 = client.register(selector, SelectionKey.OP_READ |SelectionKey.OP_WRITE, null);
                } else if(key.isReadable()){
                    readWord(key);
                } else if(key.isWritable()){
                    writeWord(key);
                }
            }catch (IOException ioe){
                ioe.printStackTrace();
            }catch(NullPointerException e){
                SocketChannel socket=(SocketChannel) key.channel();
                String name=(String) key.attachment();
                if(name==gamer1){
                    us1.addPunteggio(punti[0]);
                }else{
                    us2.addPunteggio(punti[1]);
                }
                try {
                    socket.close();
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
            //Se la traduzione va a buon fine faccio cominciare la sfida
                if (punti[0] > punti[1]) {
                    sendMessage("You won " + punti[0] + " to " + punti[1] + ". Receive 3 bonus point", sock1);
                    sendMessage("You lose " + punti[0] + " to " + punti[1], sock2);
                    punti[0] += 3;
                } else if (punti[1] > punti[0]) {
                    sendMessage("You won " + punti[1] + " to " + punti[0] + ". Receive 3 bonus point", sock2);
                    sendMessage("You lose " + punti[1] + " to " + punti[0], sock1);
                    punti[1]+= 3;
                } else {
                    sendMessage("You drew " + punti[1] + " to " + punti[0], sock1);
                    sendMessage("You drew " + punti[1] + " to " + punti[0], sock2);
                }
                us1.addPunteggio(punti[0]);
                us2.addPunteggio(punti[1]);
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
    public boolean getTranslation(ArrayList<String> t, ArrayList<String> kparole, int k){
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
                    t.add(i,translation);
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
     * Invia al client selezionato la successiva parola
     * @param key la chiave da cui prendere il Channel
     * @throws IOException
     */
    public void writeWord(SelectionKey key) throws IOException{
        SocketChannel client=(SocketChannel) key.channel();
        String name=(String) key.attachment();

        ByteBuffer buffer=ByteBuffer.allocate(100);
        buffer.clear();
        String word;
        if(name==gamer1) {
            word = kparole.get(ind1);
            ind1++;
        }else{
            word=kparole.get(ind2);
            ind2++;
        }
        buffer=ByteBuffer.wrap(word.getBytes());
        client.write(buffer);

        client.register(selector, SelectionKey.OP_READ, name);
    }

    /**
     * Riceve dal client selezionato la parola tradotta
     * @param key la chiave da cui prendere il Channel
     * @throws IOException
     */
    public void readWord(SelectionKey key) throws IOException{
        SocketChannel client=(SocketChannel) key.channel();
        String name=(String) key.attachment();

        ByteBuffer buffer=ByteBuffer.allocate(1024);
        String answer="";
        String word="";
        if(name==null){
            int len=client.read(buffer);
            answer += StandardCharsets.UTF_8.decode(buffer).toString();
            if(len==0 | len==-1) {
                String[] substring = answer.split("\\s+");
                word = substring[0];
                name = substring[1];
                client.register(selector, SelectionKey.OP_READ, name);
            }
        } else{
            client.read(buffer);
            word=StandardCharsets.UTF_8.decode(buffer).toString();
            client.register(selector, SelectionKey.OP_READ, name);
        }

        if(name==gamer1){
            if(word.equals(translation.get(ind1)))
                punti[0]++;
            else
                punti[0]--;
        }else{
            if(word.equals(translation.get(ind2)))
                punti[1]++;
            else
                punti[1]--;
        }
    }
}
