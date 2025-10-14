package com.group02.openevent.ai.service;

import com.group02.openevent.ai.util.ConfigLoader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 *
 * @author Admin
 */
@Service
public class WeatherService {
    private static final String API_KEY =ConfigLoader.get("api.weather");
    private static final String BASE_URL = "http://api.weatherapi.com/v1/forecast.json";

    public String getForecastNote(LocalDateTime date, String location) {
        try {
            String url = BASE_URL + "?key=" + API_KEY + "&q=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                    + "&dt=" + date + "&days=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) return null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String json = reader.lines().collect(Collectors.joining());

                JSONObject obj = new JSONObject(json);
                JSONObject day = obj.getJSONObject("forecast")
                        .getJSONArray("forecastday").getJSONObject(0)
                        .getJSONObject("day");

                String condition = day.getJSONObject("condition").getString("text");
                double rainChance = day.optDouble("daily_chance_of_rain", 0);

                if (condition.toLowerCase().contains("rain") || rainChance > 50) {
                    return "Dự báo ngày " + date + " tại " + location + ": " + condition +
                            " 🌧 (khả năng mưa: " + rainChance + "%)";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}