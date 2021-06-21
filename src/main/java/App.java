import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;


import java.util.*;

public class App {
    private JsonObject json_response_relations;
    private final Map<String, String> issues_status = new HashMap<>();
    private final Map<String, String> issues_relations_id = new HashMap<>();
    private final Set<String> set_issues = new HashSet<>();

    private String rm_user_name; //= "yakorotyshov";
    private String rm_user_password; // = "VubxexJdTQ";

    public void setRm_user_name(String rm_user_name) {
        this.rm_user_name = rm_user_name;
    }

    public void setRm_user_password(String rm_user_password) {
        this.rm_user_password = rm_user_password;
    }

    public static void main(String[] args) {
        App app = new App();
        app.setRm_user_name(args[0]); // Первый параметр = Имя пользователя в RedMine
        app.setRm_user_password(args[1]); // Второй параметр = Пароль пользователя в RedMine
        String old_ticket = args[2]; // Третий параметр = тикет, с которого надо перенести данные
        String new_ticket = args[3]; // Четвертрый параметр = тикет, в который надо перенести данные

        // Получаем списко тикетов
        app.getAllRelationTickets(old_ticket);

        // Вынимаем данные: номер тикета и ID связи
        app.setIssuesCollection();

        // Вынимаем данные: номер тикета и его статус
        app.setIssuesStatusFromJsonObject();

        // Добавляем все тикеты в новую заявку, статус которых не равен Закрыт и Отклонен
        app.addRelationToTicket(new_ticket);

        // Удаляем все перенесенные тикеты из старой заявки
        app.removeOpenTicketsFromOldMainTicket();

    }

    // Метод получает json со всеми связанными тикетами в корневой заявкой
    public void getAllRelationTickets(String old_tiket){
        System.out.println("Start request tickets.");
        String url = String.format("http://redmine.mango.local/issues/%s.json?include=relations", old_tiket);
        Response response = RestAssured.given()
                .auth()
                .basic(rm_user_name,rm_user_password)
                .get(url);


        System.out.println("Request tickets done. Status cod: " + response.getStatusCode());
        json_response_relations = JsonParser.parseString(response.asString()).getAsJsonObject();
    }

    // Создаем множество с номерами тикетов и строим Map "Номер заявки" - relation_id
    public void setIssuesCollection(){
        JsonArray relations = json_response_relations.get("issue").getAsJsonObject().get("relations").getAsJsonArray();
        for(JsonElement e: relations){
            String issue_id = e.getAsJsonObject().get("issue_to_id").toString();
            set_issues.add(issue_id);
            issues_relations_id.put(issue_id,
                    e.getAsJsonObject().get("id").toString());
        }
        System.out.println("Set list Tickets and Relation_id done.");
    }

    // Создаем Map со списком "Номер_задачи - Статус"
    public void setIssuesStatusFromJsonObject(){
        System.out.println("Start getting status issues. List:");
        for(String issue_id : set_issues) {
            System.out.println(issue_id);
            String url = String.format("http://redmine.mango.local/issues/%s.json", issue_id);
            Response response = RestAssured.given()
                    .auth()
                    .basic(rm_user_name, rm_user_password)
                    .get(url);

            JsonObject response_as_json = JsonParser.parseString(response.asString()).getAsJsonObject();
            issues_status.put(issue_id,
                    response_as_json.get("issue").getAsJsonObject().get("status").getAsJsonObject().get("name").toString());
        }
    }

    // Добавляем открытые тикеты в новую задачу, и удаляем их из множества issues_relations_id
    public void addRelationToTicket(String new_ticket){
        System.out.println("Start adding open tickets on main ticket " + new_ticket);

        String url = String.format("http://redmine.mango.local/issues/%s/relations.json", new_ticket);

        for(Map.Entry<String, String> entry : issues_status.entrySet()) {

            String json_relation = String.format("{\"relation\": {\"issue_to_id\":%s, \"relation_type\": \"relates\" }}", entry.getKey());

            if(!entry.getValue().equals("\"Закрыт\"") && !entry.getValue().equals("\"Отклонен\"")){
                Response response = RestAssured.given()
                        .auth()
                        .basic( rm_user_name, rm_user_password)
                        .contentType(ContentType.JSON)
                        .body(json_relation)
                        .post(url);

                System.out.println("Adding " + entry.getKey() + ". Status " + response.getStatusCode());
                issues_relations_id.remove(entry.getKey());
            }
        }
    }

    // Удоляем из старого тикета все открыте и пересенные заявки
    public void removeOpenTicketsFromOldMainTicket(){
        System.out.println("Start deleting open ticket from old main ticket.");
        for(Map.Entry<String, String> entry : issues_status.entrySet()) {
            if(!issues_relations_id.containsKey(entry.getKey())){
                String relation_id = issues_relations_id.get(entry.getKey());
                String url = String.format("http://redmine.mango.local/relations/%s.json", relation_id);

                Response response = RestAssured.given()
                        .auth()
                        .basic( rm_user_name,rm_user_password)
                        .contentType(ContentType.JSON)
                        .delete(url);
                System.out.printf("Deleted relation with ID '%s' done. Status " + response.getStatusCode(), relation_id);
            }
        }
    }

}
