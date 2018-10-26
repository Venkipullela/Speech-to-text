import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;

public class SpeechToText {

    public static void main(String... args) throws Exception {

        StringBuilder convertedText = new StringBuilder();

        try {
            String fileName = args[0];

            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);
            ByteString audioBytes = ByteString.copyFrom(data);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(AudioEncoding.OGG_OPUS)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("en-US")
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            SpeechClient speechClient = SpeechClient.create();
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            System.out.println(results.size());


            for (SpeechRecognitionResult result : results) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                System.out.printf("Transcription: %s%n", alternative.getTranscript());
                convertedText.append(alternative.getTranscript()).append(" ");
            }

            JsonObject jsonOutput = getJsonResponse(convertedText.toString());
            if(isSearchRestaurant(jsonOutput.getIntent())){
                executeQuery(buildSQLQuery(jsonOutput));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static JsonObject getJsonResponse(String input){
        String outputFilePath = "/Users/venkatesh/Downloads/speech_to_text/output.json";
        String query = "curl XPOST localhost:5000/parse -d '{\"query\":\""+input+"\"}' > " + outputFilePath;

        System.out.println("printing the executing command: " + query);
        try{
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c",query});
            System.out.println("printing the exit_value: " + p.waitFor());

            ObjectMapper objectMapper = new ObjectMapper();
            InputStream outputJson = new FileInputStream(outputFilePath);
            File file = new File(outputFilePath);
            if(file.exists()){
                file.delete();
            }
            JsonObject jsonObject = objectMapper.readValue(outputJson,JsonObject.class);
            System.out.println(jsonObject.toString());
            return jsonObject;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isSearchRestaurant(Intent intent){
        if(intent.getName().toLowerCase().contains("search") &&
                intent.getName().toLowerCase().contains("restaurant")){
            return true;
        }
        return false;
    }

    public static void executeQuery(String query){
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://127.0.0.1:5432/acko_test",
                            "venkatesh", "");
            Statement statement = c.createStatement();

            System.out.println("printing the query: " + query);
            System.out.println();
            System.out.println();


            ResultSet rs = statement.executeQuery(query);

            System.out.println("*****Results of your restaurant search*****");
            while (rs.next()){
                System.out.print("Restaurant Name: "+rs.getString("name").trim() + " , Location : " + rs.getString("location").trim() + " , Cuisine : "
                + rs.getString("cuisine").trim() + " , Direction In the City : " + rs.getString("direction").trim());
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String buildSQLQuery(JsonObject jsonObject){
        StringBuilder selectQuery = new StringBuilder().append("SELECT * FROM restaurants");
        if(jsonObject.getEntities().length != 0){
            Entity[] entities = jsonObject.getEntities();
            StringBuilder lb = new StringBuilder();
            lb.append(" location IN (");
            StringBuilder cb = new StringBuilder();
            cb.append(" cuisine IN (");
            StringBuilder db = new StringBuilder();
            db.append(" direction IN (");
            StringBuilder rb = new StringBuilder();
            rb.append(" name IN (");
            for (Entity e: entities) {
                if(e.getEntity().equalsIgnoreCase("location")){
                    lb.append("'");
                    lb.append(e.getValue()).append("',");
                }else if(e.getEntity().equalsIgnoreCase("cuisine")){
                    cb.append("'");
                    cb.append(e.getValue()).append("',");
                }else if(e.getEntity().equalsIgnoreCase("direction")){
                    db.append("'");
                    db.append(e.getValue()).append("',");
                }else if(e.getEntity().equalsIgnoreCase("restaurant_name")){
                    rb.append("'");
                    rb.append(e.getValue()).append("',");

                }
            }
            boolean flag = false;
            if(lb.length() !=0 || cb.length() != 0 || db.length() != 0 || rb.length() != 0){
                selectQuery.append(" WHERE ");
                if(lb.length() != 14){
                    lb.setLength(lb.length()-1);
                    lb.append(") ");
                    selectQuery.append(lb);
                    flag =true;
                }
                if(cb.length() != 13){
                    if(flag){
                        selectQuery.append(" AND ");
                    }
                    cb.setLength(cb.length()-1);
                    cb.append(") ");
                    selectQuery.append(cb);
                    flag =true;
                }
                if(db.length() != 15){
                    if(flag){
                        selectQuery.append(" AND ");
                    }
                    db.setLength(db.length()-1);
                    db.append(") ");
                    selectQuery.append(db);
                    flag =true;
                }
                if(rb.length() != 10){
                    if(flag){
                        selectQuery.append(" AND ");
                    }
                    rb.setLength(rb.length()-1);
                    rb.append(") ");
                    selectQuery.append(rb);
                }
            }
        }
        return selectQuery.toString();
    }
}
