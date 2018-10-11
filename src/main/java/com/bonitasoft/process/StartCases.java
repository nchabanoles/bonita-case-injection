package com.bonitasoft.process;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.performance.Credential;
import org.bonitasoft.engine.performance.client.APIClient;
import org.bonitasoft.engine.performance.client.APIClientFactory;
import org.bonitasoft.engine.util.APITypeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartCases {

    private static List<String> startDurations = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        String serverURL = "http://localhost:" + "8080";
        Map<String, String> map = new HashMap<String, String>();
        map.put("server.url", serverURL);
        map.put("application.name", "bonita");
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);


        APIClient apiClient = APIClientFactory.newAPIClient();
        long processID = 4888507202623783240L;
        int i = 1;
        long startTime = System.currentTimeMillis();
        try {

            long lastTime = System.currentTimeMillis();
            do {
                apiClient.login(new Credential("Default", "walter.bates", "bpm"));
                apiClient.startProcess(processID, new HashMap<>());
                apiClient.logout();
                long now = System.currentTimeMillis();
                startDurations.add("Started case " + i + " after " + (now - lastTime));
                lastTime = now;
                i++;
            } while (i <= 1);
        } finally {
            long duration = (System.currentTimeMillis() - startTime);
            startDurations.stream().forEach(StartCases::log);

            System.err.println("Bench completed ("+(i-1)+" cases started in "+ duration+"ms ):");
        }
    }

    private static void log(String log) {
        System.err.println(log);
    }
}
