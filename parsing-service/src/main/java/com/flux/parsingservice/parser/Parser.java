package com.flux.parsingservice.parser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
public class Parser {

    // LOGISTIC_SERVICE API's
    public static final String LOGISTIC_SERVICE = "http://LOGISTIC-SERVICE/logistic-api";
    public static final String GET_DAILY_PARAMETERS_BY_WEEK_NOT_NULL = "/getDailyParametersByWeekNotNull";

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";

    public static final String ORIGIN_URL = "http://orar.usarb.md";

    private static final String GROUP_API = "http://orar.usarb.md/api/getGroups";
    private static final String TEACHERS_API = "http://orar.usarb.md/api/getteachers";
    private static final String AUDIENCE_API = "http://orar.usarb.md/api/getOffices";

    private static final String VALUE = "value";

    private static final List<String> CURRENT = Arrays.asList("day", "week", "semester");

    private final Gson gson;
    private final RestTemplate restTemplate;

    public Parser(Gson gson, RestTemplate restTemplate) {
        this.gson = gson;
        this.restTemplate = restTemplate;
    }

    public String getGroups() throws IOException {
        return getJsonContent(GROUP_API);
    }

    public String getTeachers() throws IOException {
        return getJsonContent(TEACHERS_API);
    }

    public String getAudiences() throws IOException {
        return getJsonContent(AUDIENCE_API);
    }

    public String getLessons(String groupJson, String dailyParameters) throws IOException {

        Connection.Response res = getResponseContent();
        Document timeTableDom = res.parse();
        String csrf = timeTableDom.select("meta[name=\"csrf-token\"]").first().attr("content");

        JsonObject groupObject = gson.fromJson(groupJson, JsonObject.class);
        JsonObject dailyParametersObject = gson.fromJson(dailyParameters, JsonObject.class);

        if (isNull(dailyParametersObject.get("week"))) {
            JsonObject dailyParams = gson.fromJson(restTemplate.getForObject(LOGISTIC_SERVICE + GET_DAILY_PARAMETERS_BY_WEEK_NOT_NULL, String.class), JsonObject.class);
            dailyParametersObject.add(
                    "week",
                    dailyParams.get("week")
            );
        }

        return Jsoup.connect(String.valueOf(LessonsBy.GROUP.getApi()))
                .method(Connection.Method.POST)
                .referrer("http://orar.usarb.md/")
                .userAgent(USER_AGENT)
                .referrer("http://orar.usarb.md/")
                .ignoreContentType(true)
                .cookies(res.cookies())
                .data("_csrf", csrf)
                .data("gr", groupObject.get("id").toString().replace("\"", ""))
                .data("sem", dailyParametersObject.get("semester").toString().replace("\"", ""))
                .data("day", dailyParametersObject.get("day").toString().replace("\"", ""))
                .data("week", dailyParametersObject.get("week").toString().replace("\"", ""))
                .data("grName", groupObject.get("name").toString().replace("\"", ""))
                .execute()
                .body();
    }

    private String formatJson(String json) {
        return json.replace("Denumire", "name").replace("Id", "id");
    }

    private Connection.Response getResponseContent() throws IOException {
        return Jsoup.connect(ORIGIN_URL).userAgent(USER_AGENT).method(Connection.Method.GET).execute();
    }

    private String getJsonContent(String apiUrl) throws IOException {
        return formatJson(Jsoup.connect(apiUrl)
                .userAgent(USER_AGENT)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute()
                .body());
    }

    @SneakyThrows
    public String getDailyParameters() {
        Connection.Response res = getResponseContent();
        Document timeTableDom = res.parse();
        Elements current = timeTableDom.select("option[selected=\"selected\"]");

        Map<String, String> weekData = new HashMap<>();
        if (current.size() < 3) {
            weekData.put(CURRENT.get(0), current.get(0).attr(VALUE));
            weekData.put(CURRENT.get(2), current.get(1).attr(VALUE));
        } else {
            weekData.put(CURRENT.get(0), current.get(0).attr(VALUE));
            weekData.put(CURRENT.get(1), current.get(1).attr(VALUE));
            weekData.put(CURRENT.get(2), current.get(2).attr(VALUE));
        }

        return gson.toJson(weekData);
    }
}
