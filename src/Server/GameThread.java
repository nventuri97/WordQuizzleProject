package Server;
import com.google.gson.Gson;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Utils.*;

public class GameThread extends Thread {
    private Database database;                                      //Istanza della classe database passata dal thread utente che invia la sfida
    private String gamer1;                                          //Nickname del primo giocatore
    private String gamer2;                                          //Nickname del secondo giocatore
    private int k;                                                  //Numero di parole da inviare nella sfida, un intero tra 1 e 12
    private Gson gson;                                              //Struttura per il parsing del dizionario da JSON
    private FileReader reader;                                      //FileReader per leggere il file del dizionario
    private Socket sock1, sock2;                                    //Socket dei due giocatori
    private ArrayList<String> kparole;                              //ArrayList contenente le K parole scelte a caso dal dizionario
    private ArrayList<String> translation;                          //ArrayList contenente le traduzioni delle K parole scelte
    private ServerSocketChannel gameSockChannel;                    //Server socket che gestisce la sfida
    private Selector selector;                                      //Selettore per la gestione dei due client
    private ScheduledExecutorService timer;                         //Timer per la sfida
    private Boolean endGaming;                                      //flag per il controllo del while
    private static GamerData gd1;                                   //dati di gioco relativi a gamer1
    private static GamerData gd2;                                   //dati di gioco relativi a gamer2
    private volatile AtomicInteger userClosed;                      //Indica il numero di giocatori che hanno terminato la partita, se ==2 allora la partita termina
    private static Iterator<SelectionKey> iterator;                 //Iteratore sulle chiavi del selettore
    private User us1, us2;

    public GameThread(Database db, String nick1, String nick2, ServerSocketChannel ssocket){
        this.k=(int) (Math.random()*11)+1;
        this.database=db;
        this.gamer1=nick1;
        this.gamer2=nick2;
        this.translation=new ArrayList<>(k);
        this.gson=new Gson();

        this.gameSockChannel=ssocket;
        try {
            this.reader = new FileReader("./dizionario.json");
            this.selector=Selector.open();
        }catch(IOException fe){
            fe.printStackTrace();
        }
        this.sock1=db.getSocket(gamer1);
        this.sock2=db.getSocket(gamer2);
        this.gameSockChannel=ssocket;
        this.endGaming=false;
        gd1=new GamerData();
        gd2=new GamerData();
        this.userClosed=new AtomicInteger();
        this.timer=Executors.newScheduledThreadPool(1);
    }

    @Override
    public void run(){
        try {
            this.gameSockChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (Exception e){
            e.printStackTrace();
        }

        //Prendo la nuova porta su cui è aperta la gameSocket e la invio ai due client
        int newPort=gameSockChannel.socket().getLocalPort();
        sendMessage("Game port "+newPort, sock1);
        sendMessage("Game port "+newPort, sock2);

        //Creo un'ArrayList contenente il dizionario e poi seleziono le K parole
        ArrayList<String> dictionary=gson.fromJson(reader, ArrayList.class);
        kparole=getKWord(dictionary, k);
        getTranslation(translation, kparole, k);

        //Prelevo le due istanze della classe User dal database
        us1 = database.getUser(gamer1);
        us2 = database.getUser(gamer2);

        timer.schedule(this::timeUP, 60, TimeUnit.SECONDS);

        while(!endGaming) {
            try{
                selector.select();
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        //Creo una active socket derivata dalla accept sulla passive socket su cui il server è in ascolto
                        SocketChannel client = server.accept();
                        System.out.println("Client " + client + " accepted");
                        //Setto a non-blocking
                        client.configureBlocking(false);
                        GamerData data=new GamerData();
                        //Aggiungo la key del client
                        client.register(selector, SelectionKey.OP_WRITE, data);
                    } else if (key.isWritable()) {
                        writeWord(key);
                    } else if (key.isReadable()) {
                        readWord(key);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (NullPointerException e) {
                    GamerData data=(GamerData) key.attachment();
                    String name=data.getUsername();
                    if (name == gamer1) {
                        us1.addPunteggio(data.getPunti());
                    } else {
                        us2.addPunteggio(data.getPunti());
                    }
                    try {
                        key.channel().close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }

        int pt1, pt2;
        pt1=gd1.getPunti();
        pt2=gd2.getPunti();
        //Se la traduzione va a buon fine faccio cominciare la sfida
        if (pt1 > pt2) {
            sendMessage("You won " + pt1 + " to " + pt2 + " You receive 3 bonus point", sock1);
            sendMessage("You lose " + pt1 + " to " + pt2, sock2);
            pt1 += 3;
        } else if (pt2 > pt1) {
            sendMessage("You won " + pt2 + " to " + pt1 + " You receive 3 bonus point", sock2);
            sendMessage("You lose " + pt2 + " to " + pt1, sock1);
            pt2+= 3;
        } else {
            sendMessage("You drew " + pt1 + " to " + pt2, sock1);
            sendMessage("You drew " +pt2+ " to " + pt1, sock2);
        }
        us1.addPunteggio(pt1);
        us2.addPunteggio(pt2);
        us1.setBusy();
        us2.setBusy();
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
            word.toLowerCase();
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
                String result=reader.readLine();
                if(connection.getResponseCode()!=200){
                    sendMessage("Something is gone wrong, we are sorry", sock1);
                    sendMessage("Something is gone wrong, we are sorry", sock2);
                    connection.disconnect();
                    return false;
                }else {
                    Parser parser=new Parser();
                    String translation=parser.readWordTranslate(result);
                    //Nel caso in cui la parola abbia come terminazione un carattere di punteggiatura lo elimino
                    if(translation.endsWith(","))
                        translation.replace(",", "");
                    else if(translation.endsWith("."))
                        translation.replace(".", "");
                    else if(translation.endsWith("!"))
                        translation.replace("!","");
                    //Trasformo tutte le lettere in minuscole
                    translation.toLowerCase();
                    System.out.println(translation);
                    t.add(i, translation);
                }
                connection.disconnect();
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
    public void writeWord(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        GamerData data = (GamerData) key.attachment();
        String name = data.getUsername();

        ByteBuffer buffer;
        if(!timer.isShutdown()) {
            if (!data.getHaveFinished()) {
                String word = "";
                //Inviando la prima parola quando ancora non conosco il nome devo essere sicuro di inviare sempre e solo la prima
                if (name == "") {
                    word = kparole.get(0);
                } else {
                    word = kparole.get(data.getIndWord());
                    data.setIndWord();
                }
                buffer = ByteBuffer.wrap(word.getBytes());
                client.write(buffer);

                //stampa di debug
                System.out.println(word);

                if (data.getIndWord() == k) {
                    data.setHaveFinished();
                }

                key.interestOps(SelectionKey.OP_READ);
                key.attach(data);
            } else {
                String message = "You have finished, wait to see game result";
                buffer = ByteBuffer.wrap(message.getBytes());
                client.write(buffer);
                buffer.clear();
                //Stampa di debug
                System.out.println(name + " gamer " + gamer1);
                //Stampa di debug
                System.out.println(name + " gamer " + gamer2);
                if (name.equals(gamer1)) {
                    gd1 = data;
                    //Stampa di debug
                    System.out.println(gd1.getPunti());
                } else if (name.equals(gamer2)) {
                    gd2 = data;
                    //Stampa di debug
                    System.out.println(gd2.getPunti());
                }
                //Se entrambi i giocatori hanno finito allora chiudo la partita
                if (userClosed.incrementAndGet() == 2)
                    endGaming = true;
                key.channel().close();
                key.cancel();
            }
        } else {
            try {
                String s="Time's up, game finished";
                buffer=ByteBuffer.wrap(s.getBytes());
                client.write(buffer);
                if(name.equals(gamer1))
                    gd1=data;
                else if(name.equals(gamer2))
                    gd2=data;
                key.channel().close();
                key.cancel();
                if(userClosed.incrementAndGet()==2)
                    endGaming=true;
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Riceve dal client selezionato la parola tradotta
     * @param key la chiave da cui prendere il Channel
     * @throws IOException
     */
    public void readWord(SelectionKey key) throws IOException{
        SocketChannel client=(SocketChannel) key.channel();
        GamerData data=(GamerData) key.attachment();
        String name=data.getUsername();
        String answer=data.getAnswer();
        String word="";

        ByteBuffer buffer=ByteBuffer.allocate(1024);
        buffer.clear();
        if(!timer.isShutdown()) {
            int len = client.read(buffer);
            buffer.flip();

            //Client crashato
            if (len == -1) {
                System.out.println("Client " + client + " is crashed");
                key.channel().close();
                key.cancel();
                //Ho letto tutto quello che c'era da leggere
            } else if (len < 1024) {
                answer += StandardCharsets.UTF_8.decode(buffer).toString();
                if (name == "") {
                    String[] substring = answer.split("\\s+");
                    word = substring[1];
                    data.setUsername(substring[0]);
                    data.setAnswer("");
                } else {
                    word = answer;
                    data.setAnswer("");
                }
                key.interestOps(SelectionKey.OP_WRITE);
                //Devo leggere ancora
            } else if (len == 1024) {
                answer += StandardCharsets.UTF_8.decode(buffer).toString();
                data.setAnswer(answer);

            }

            if (word.equals(translation.get(data.getIndWord() - 1)))
                data.incPunti();
            else
                data.decPunti();

            key.attach(data);
        }else{
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    /**
     * Funzione lanciata quando viene schedulato il timer
     */
    public void timeUP(){
        timer.shutdown();
        if(userClosed.get()<2){
            String s="Time's up, game finished";
            System.out.println(s);
            //Termino il ciclo principale
        }
    }


    /**
     * Classe di appoggio per la memorizzazione dei dati dell'utente
     */
    public class GamerData{
        public String username;
        public int indWord;
        public String answer;
        public int punti;
        public boolean haveFinished;

        public GamerData(){
            username="";
            indWord=1;
            punti=0;
            answer="";
            haveFinished=false;
        }

        public String getUsername(){
            return username;
        }

        public String getAnswer(){
            return answer;
        }

        public int getIndWord(){
            return indWord;
        }

        public int getPunti(){
            return punti;
        }

        public void setUsername(String s){
            username=s;
        }

        public void setAnswer(String s){
            answer=s;
        }

        public void setIndWord(){
            indWord++;
        }

        public void incPunti(){
            punti++;
        }

        public void decPunti(){
            punti--;
        }

        public Boolean getHaveFinished(){
            return haveFinished;
        }

        public void setHaveFinished(){
            haveFinished=!haveFinished;
        }
    }
}
