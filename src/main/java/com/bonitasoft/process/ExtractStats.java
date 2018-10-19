package com.bonitasoft.process;

import com.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.util.APITypeManager;

import java.util.HashMap;
import java.util.Map;

public class ExtractStats {

    public static void main(String[] args) throws SearchException {

        String serverURL = "http://bonitalb-1662063562.eu-west-1.elb.amazonaws.com:" + "8080";
        Map<String, String> map = new HashMap<String, String>();
        map.put("server.url", serverURL);
        map.put("application.name", "bonita");
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

        APIClient apiClient = new APIClient();

        try {
        apiClient.login("install", "install");
        long earlierStartDate = Long.parseLong(args[0]);
        HashMap<Object, Object> result = new CreatedVsCompleted().fetchData(apiClient, earlierStartDate, System.currentTimeMillis());

        System.out.println(result);
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (SearchException e) {
            e.printStackTrace();
        } finally {
            try {
                apiClient.logout();
            } catch (LogoutException e) {
                e.printStackTrace();
            }
        }
    }
}
