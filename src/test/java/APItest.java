import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import java.sql.SQLOutput;
import java.util.*;

public class APItest {

    private List<String> list_issues = new ArrayList<>();

    // Запрос спикска связанныз зяавок с текущей
    @Test
    public void test1(){
        String url = "http://redmine.mango.local/issues/252198.json?include=relations";
        Response response = RestAssured.given()
                .auth()
                .basic("yakorotyshov","VubxexJdTQ")
                .get(url);

        JsonObject result_as_json = JsonParser.parseString(response.asString()).getAsJsonObject();
        System.out.println(response.getStatusCode());
        JsonArray relations = result_as_json.get("issue").getAsJsonObject().get("relations").getAsJsonArray();
        for(JsonElement e: relations){
            list_issues.add(e.getAsJsonObject().get("issue_to_id").toString());
        }
        //list_issues.forEach(issue -> System.out.println(issue_name(issue)));
        list_issues.forEach(System.out::println);

        System.out.println(result_as_json.toString());
        System.out.println("Зависимости:");
        System.out.println(result_as_json.get("issue").getAsJsonObject().get("relations").toString());
    }

    // /issues.xml?issue_id=1


    // Просто получение заявки
    @Test
    public void test2(){
        String issue = "233958";
        String url = String.format("http://redmine.mango.local/issues/%s.json", issue);
        Response response = RestAssured.given()
                .auth()
                .basic("yakorotyshov","VubxexJdTQ")
                .get(url);

        JsonObject result_as_json = JsonParser.parseString(response.asString()).getAsJsonObject();
        System.out.println(response.getStatusCode());

        System.out.println(result_as_json.toString());
    }

    // Получение заявки и каких-то параметров из ответа
    @Test
    public void test5(){
        issue_name("220794");
    }
    private void issue_name(String issue_id){
        String url = String.format("http://redmine.mango.local/issues/%s.json", issue_id);
        Response response = RestAssured.given()
                .auth()
                .basic("yakorotyshov","VubxexJdTQ")
                .get(url);

        JsonObject result_as_json = JsonParser.parseString(response.asString()).getAsJsonObject();

        System.out.println(result_as_json);

        System.out.printf(
                "Status for %s: " + result_as_json.get("issue").getAsJsonObject().get("status").getAsJsonObject().get("name").toString(), issue_id);
        System.out.printf(
                "\nID for %s: " + result_as_json.get("issue").getAsJsonObject().get("status").getAsJsonObject().get("id").toString(), issue_id);
    }

    // Добавление связи
    @Test
    public void test3(){
        String issue_1 = "252198"; // главная
        String issue_2 = "252199";
        String issue_3 = "249901";

        String json_relation_1 = "{\"relation\": {\"issue_to_id\":"+ issue_3 +", \"relation_type\": \"relates\" }}";

        String url = String.format("http://redmine.mango.local/issues/%s/relations.json", issue_1);
        Response response = RestAssured.given()
                .auth()
                .basic("yakorotyshov","VubxexJdTQ")
                .contentType(ContentType.JSON)
                .body(json_relation_1)
                .post(url);
        System.out.println(response.getStatusCode());

    }


    // Удаление связи
    @Test
    public void test4(){
        String issue_1 = "252198"; // главная
        String issue_2 = "252199";
        String issue_3 = "249901";

        String json_relation_1 = "{\"relation\": {\"issue_to_id\":"+ issue_2 +", \"relation_type\": \"relates\" }}";

        // Удалять надо по ID связи, а не по issue_id

        String url = "http://redmine.mango.local/relations/128370.json";
        Response response = RestAssured.given()
                .auth()
                .basic("yakorotyshov","VubxexJdTQ")
                .contentType(ContentType.JSON)
                .delete(url);
        System.out.println(response.getStatusCode());

    }

    // 824 - ID коротышов ярослав
    // 1774- ID Шитикова Дарья

    // Замена Назначенного сотрудника в заявке
    @Test
    public void test6(){

        List<String> set = Arrays.asList(
                "249835","248560","248516","247978","247611","247620"
                ,"247383","247331","247201","247183","246701"
                ,"245061","244921","244435","242307","242165"
                ,"241253","240094","238956","238776");


        for(String issue : set){
            String url = String.format("http://redmine.mango.local/issues/%s.json", issue);
            Response response = RestAssured.given()
                    .auth()
                    .basic("yakorotyshov","VubxexJdTQ")
                    .contentType(ContentType.JSON)
                    .body("{\"issue\": {\"assigned_to_id\": 1774}}") // Шитикова Дарья
                    .put(url);

            System.out.println(response.getStatusCode());
        }

    }

}
