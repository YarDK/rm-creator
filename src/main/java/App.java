import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;


import java.util.*;

public class App {
    private JsonObject json_response_relations;
    private final Map<String, Integer> issues_status = new HashMap<>();
    private Map<String, String> issues_relations_id = new HashMap<>();
    private final Set<String> set_issues = new HashSet<>();

    public String rm_user_name; //= "yakorotyshov";
    public String rm_user_password; // = "VubxexJdTQ";
    public String old_ticket;
    public String new_ticket;

    public static void main(String[] args) {
        App app = new App();
        try {
            app.rm_user_name = args[0]; // Первый параметр = Имя пользователя в RedMine
            app.rm_user_password = args[1]; // Второй параметр = Пароль пользователя в RedMine
            app.old_ticket = args[2]; // Третий параметр = тикет, с которого надо перенести данные
            app.new_ticket = args[3]; // Четвертрый параметр = тикет, в который надо перенести данные

            // Получаем списко тикетов
            app.getAllRelationTickets();

            // Вынимаем данные: номер тикета и ID связи
            app.setIssuesCollection();

            // Вынимаем данные: номер тикета и его статус
            app.setIssuesStatusFromJsonObject();

            // Добавляем все тикеты в новую заявку, статус которых не равен Закрыт и Отклонен
            app.addRelationToTicket();

            // Удаляем все перенесенные тикеты из старой заявки
            app.removeOpenTicketsFromOldMainTicket();

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(
                    "First parameter = Username in RedMine \n" +
                            "Second parameter = User password in RedMine \n" +
                            "Third parameter = ticket from which data should be transferred \n" +
                            "Fourth parameter = ticket to which data should be transferred"
            );
            e.printStackTrace();
        }

    }

    // Метод получает json со всеми связанными тикетами в корневой заявкой
    public void getAllRelationTickets() {
        System.out.println("Start request tickets from " + old_ticket);
        String url = String.format("http://redmine.mango.local/issues/%s.json?include=relations", old_ticket);
        Response response = RestAssured.given()
                .auth()
                .basic(rm_user_name, rm_user_password)
                .get(url);


        System.out.println("Request tickets done. Status cod: " + response.getStatusCode());
        json_response_relations = JsonParser.parseString(response.asString()).getAsJsonObject();
        //System.out.println(json_response_relations);
    }

    // Создаем множество с номерами тикетов и строим Map "Номер заявки" - relation_id
    public void setIssuesCollection() {
        JsonArray relations = json_response_relations.get("issue").getAsJsonObject().get("relations").getAsJsonArray();

        for (JsonElement e : relations) {
            String issue_id = e.getAsJsonObject().get("issue_id").toString();
            if(issue_id.equals(old_ticket)) issue_id = e.getAsJsonObject().get("issue_to_id").toString();

            set_issues.add(issue_id);
            issues_relations_id.put(issue_id,
                    e.getAsJsonObject().get("id").toString());
        }

        System.out.println("Set list Tickets and Relation_id done.");
    }

    // Создаем Map со списком "Номер_задачи - Статус"
    public void setIssuesStatusFromJsonObject() {
        System.out.println("Start getting status issues. List:");
        for (String issue_id : set_issues) {
            System.out.println(issue_id);
            String url = String.format("http://redmine.mango.local/issues/%s.json", issue_id);
            Response response = RestAssured.given()
                    .auth()
                    .basic(rm_user_name, rm_user_password)
                    .get(url);

            JsonObject response_as_json = JsonParser.parseString(response.asString()).getAsJsonObject();
            issues_status.put(issue_id,
                    response_as_json.get("issue").getAsJsonObject().get("status").getAsJsonObject().get("id").getAsInt());
        }
    }

    // Добавляем открытые тикеты в новую задачу, и удаляем их из множества issues_relations_id
    public void addRelationToTicket() {
        if(!issues_status.containsValue(5) && !issues_status.containsValue(14)) {
            System.out.println("Issues list not has open issues");
            this.issues_relations_id = null;
            return;
        }

        System.out.println("Start adding open tickets in main ticket " + new_ticket);

        String url = String.format("http://redmine.mango.local/issues/%s/relations.json", new_ticket);
        Map<String, String> issues_relations_id_update = new HashMap<>();

        for (Map.Entry<String, Integer> entry : issues_status.entrySet()) {

            String json_relation = String.format("{\"relation\": {\"issue_to_id\":%s, \"relation_type\": \"relates\" }}", entry.getKey());
            if (entry.getValue() != 5 && entry.getValue() != 14) {
                Response response = RestAssured.given()
                        .auth()
                        .basic(rm_user_name, rm_user_password)
                        .contentType(ContentType.JSON)
                        .body(json_relation)
                        .post(url);

                System.out.println("Adding " + entry.getKey() + ". Status " + response.getStatusCode());
                issues_relations_id_update.put(entry.getKey(), issues_relations_id.get(entry.getKey()));
            }
        }

        this.issues_relations_id = issues_relations_id_update;
    }

    // Удоляем из старого тикета все открыте и пересенные заявки
    public void removeOpenTicketsFromOldMainTicket() {
        if(issues_relations_id == null){
            System.out.println("Relations not found.");
            return;
        }

        System.out.println("Start deleting open ticket from old main ticket.");

        for (Map.Entry<String, String> entry : issues_relations_id.entrySet()) {
            String relation_id = entry.getValue();
            String url = String.format("http://redmine.mango.local/relations/%s.json", relation_id);

            Response response = RestAssured.given()
                    .auth()
                    .basic(rm_user_name, rm_user_password)
                    .contentType(ContentType.JSON)
                    .delete(url);
            System.out.printf("Deleted relation with ID '%s' done. Status " + response.getStatusCode() + "\n", relation_id);

        }
    }

}
