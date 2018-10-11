package com.bonitasoft.process;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.util.APITypeManager;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SummaryReport {


    private static class DataStore {

        {
            String serverURL = "http://bonitalb-1662063562.eu-west-1.elb.amazonaws.com:" + "8080";
            Map<String, String> map = new HashMap<String, String>();
            map.put("server.url", serverURL);
            map.put("application.name", "bonita");
            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);
        }

        public Optional<DescriptiveStatistics> collect(long startTime) {

            APIClient apiClient = new APIClient();

            try {
                apiClient.login("install", "install");
                SearchOptions options = new SearchOptionsBuilder(0, Integer.MAX_VALUE).between(ArchivedProcessInstancesSearchDescriptor.START_DATE, startTime, System.currentTimeMillis())
                        .sort(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, Order.ASC).done();
                SearchResult<ArchivedProcessInstance> executedProcessInstances = apiClient.getProcessAPI().searchArchivedProcessInstances(options);

                DescriptiveStatistics stats = new DescriptiveStatistics();
                executedProcessInstances.getResult().stream().forEachOrdered(pi -> {
                    final LocalDateTime start = LocalDateTime.ofInstant(pi.getStartDate().toInstant(), ZoneId.systemDefault());
                    final LocalDateTime end = LocalDateTime.ofInstant(pi.getEndDate().toInstant(), ZoneId.systemDefault());
                    stats.addValue(ChronoUnit.MILLIS.between(start, end));
                });

                return Optional.ofNullable(stats);

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
            return Optional.empty();
        }

    }


    public static void main(String[] args) {
        long startTime = Long.parseLong(args[0]);
        System.err.println("Cases started after: " + new Date(startTime).toGMTString());
        DescriptiveStatistics statistics = new DataStore().collect(startTime).get();
        System.err.println(statistics);
    }
}
