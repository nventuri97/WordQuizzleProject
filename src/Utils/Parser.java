package Utils;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.*;
import java.util.*;
import Server.*;

public class Parser {
    private static GsonBuilder builder;
    private static Gson gson;

    public Parser(){
        this.builder=new GsonBuilder();
        this.gson=builder.create();
    }

    /**
     * Crea un file JSON contenente la struttura database aggiornata all'ultima versione
     */
    public static void saveDB(Database database){
        String jsonString=gson.toJson(database);
        File file=new File("backup.json");
        try {
            FileWriter fileW = new FileWriter(file);
            fileW.write(jsonString);
            fileW.flush();
            fileW.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * Ripristina il database da una precedente versione salvata nel file JSON
     * @return database salvato precedentemente
     */
    public synchronized static Database restoreDB(){
        FileReader reader=null;
        try {
            reader= new FileReader("./backup.json");
        }catch (IOException fe){
            fe.printStackTrace();
        }
        return gson.fromJson(reader, Database.class);
    }

    /**
     * Restituisce una stringa in formato JSON
     * @param s oggetto da parsare
     * @return jsonString stringa JSON
     */
    public static String parseToJSON(Object s){
        String jsonString=gson.toJson(s);
        return jsonString;
    }

    /**
     * Restituisce un set di stringhe parsando una stringa JSON
     * @param s stringa JSON da parsare
     * @return result set di amici
     */
    public static Set<String> parseFriFromJSON(String s){
        Set<String> result=gson.fromJson(s, HashSet.class);
        return result;
    }

    public static List<Map.Entry<String, Integer>> parseRankFromJSON(String s){
        List<Map.Entry<String, Integer>> result=gson.fromJson(s,new TypeToken<List<AbstractMap.SimpleEntry<String, Integer>>>() {}.getType());
        return result;
    }

    public String readWordTranslate(String json) throws IOException {
        String word = "";
        JsonReader jsonReader = new JsonReader(new StringReader(json));

        try {
            while (jsonReader.hasNext()) {
                JsonToken nextToken = jsonReader.peek();
                //System.out.println(nextToken);

                if (JsonToken.BEGIN_OBJECT.equals(nextToken)) {
                    jsonReader.beginObject();
                } else if (JsonToken.NAME.equals(nextToken)) {
                    String name = jsonReader.nextName();
                } else if (JsonToken.STRING.equals(nextToken)) {
                    String value = jsonReader.nextString();
                    return value;
                } else if (JsonToken.NUMBER.equals(nextToken)) {
                    long value = jsonReader.nextLong();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return word;
    }
}
