/* package CoronaStat;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Arrays;
import java.util.HashMap;

public class Controller {

    public String ID = "SVK";
    public TextField textField = new TextField(ID);

    @FXML private TableView<Result> tableView;

    @FXML protected void initialize() {
        downloadData();
    }

    static HashMap<String, Result> resultHashMap = new HashMap<>();

    void addResult(Result res) {
        ObservableList<Result> data = tableView.getItems();
        data.add(res);
    }

    void downloadData() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://covidapi.info/api/v1/country/" + ID)).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Controller::jsonParse)
                .join();

        for (String date : resultHashMap.keySet()) addResult(resultHashMap.get(date));

    }

    public void changeSource (){
        ID = textField.getText();
    }

    private static String jsonParse(String responseString) {


        JSONArray results = new JSONArray("[" + responseString + "]");
        JSONArray resultDates = results.getJSONObject(0).getJSONObject("result").names();

        String[] resultDatesSorted = new String[resultDates.length()];

        for (int i = 0; i < resultDates.length(); i++) { resultDatesSorted[i] = resultDates.get(i).toString(); }

        Arrays.sort(resultDatesSorted);

        for (String res : resultDatesSorted) {
            JSONObject result = results.getJSONObject(0).getJSONObject("result").getJSONObject(res);

            String date = res;
            String confirmed = String.valueOf(result.getInt("confirmed"));
            String deaths = String.valueOf(result.getInt("deaths"));
            String recovered = String.valueOf(result.getInt("recovered"));

            resultHashMap.put(res, new Result(res,confirmed,deaths,recovered));
        }

        return null;
    }
}

 */

package CoronaStat;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.TreeMap;


public class Controller {
    @FXML private TableView<Result> tableView;
    @FXML private DatePicker datePicker;
    @FXML private TextField textField = new TextField("Slovakia");

    private static TreeMap <String,String> country = new TreeMap<>();
    private static TreeMap <String,Result> data = new TreeMap<>();

    @FXML private String selCountry = "Slovakia";
    private String selDate = "";

    @FXML private Text text = new Text(selCountry);

    @FXML protected void initialize() {
        loadC();
        CountrySelect();
        DateSelect();
    }

    @FXML protected void switchCountry(){
        try {
            selCountry = textField.getText();
            otherCountrydata();
            showData();
        }catch (Exception e){
            System.out.println(e);
        }
    }

    private void CountrySelect(){
        selCountry = "Slovakia";
        otherCountrydata();
        showData();
    }

    void loadC(){
        country.clear();
        String string = null;
        try {
            string = Files.readString(Paths.get("src/countries.json"));
        } catch (Exception e){
            System.out.println(e);
        }
        JSONObject jsonObject = new JSONObject(string);
        for (String key: jsonObject.keySet()){
            country.put(key,jsonObject.getString(key));
        }
    }

    private void otherCountrydata() {
        String requestString = "https://covidapi.info/api/v1";

        if (selCountry.equals("Global")) requestString += "/global/count";
        else requestString += "/country/" + country.get(selCountry);

        downloadData(requestString);
    }

    private void DateSelect(){
        String [] date = data.get(data.lastKey()).getDate().split("-");
        int maxY = Integer.parseInt(date[0]);
        int maxM = Integer.parseInt(date[1]);
        int maxD = Integer.parseInt(date[2]);

        LocalDate maxDate = LocalDate.of(maxY,maxM,maxD);
        LocalDate minDate = LocalDate.of(2020,1,1);
        datePicker.setDayCellFactory(d -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(item.isAfter(maxDate) || item.isBefore(minDate));
            }});

        datePicker.getEditor().setOnKeyPressed(e->{
            switch (e.getCode()){
                case DELETE:
                    datePicker.setValue(null);
                    datePicker.getEditor().clear();
            }
        });

        datePicker.valueProperty().addListener(e -> {
            selDate = (datePicker.getValue() == null) ? "" : datePicker.getValue().toString();
            showData();
        });
    }

    void showData() {
        tableView.getItems().clear();

        if (selDate.equals("")){
            for (String key: data.keySet()){
                tableView.getItems().add(data.get(key));
            }
        }else {
            tableView.getItems().add(data.get(selDate));
        }
    }

    void downloadData(String restring) {
        data.clear();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(restring)).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Controller::jsonParse)
                .join();
    }

    private static String jsonParse(String string) {
        JSONObject results = new JSONObject(string).getJSONObject("result");

        for (String key: results.keySet()) {
            JSONObject oneResult = results.getJSONObject(key);
            String date = key;
            int confirmed = oneResult.getInt("confirmed");
            int deaths = oneResult.getInt("deaths");
            int recovered = oneResult.getInt("recovered");

            data.put(date, new Result(date, String.valueOf(confirmed), String.valueOf(deaths),
                    String.valueOf(recovered)));
        }

        return null;
    }
}