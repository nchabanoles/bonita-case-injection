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

import java.util.*;

public class CreatedVsCompleted {


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

                long origin = executedProcessInstances.getResult().get(0).getStartDate().getTime();
                LinkedHashMap<Double, Long> startedRecords = createTimeSpan(executedProcessInstances.getResult(), origin);
                LinkedHashMap<Double, Long> completedRecords = createTimeSpan(executedProcessInstances.getResult(), origin);

                executedProcessInstances.getResult().stream().forEachOrdered(pi -> {
                    final long start = pi.getStartDate().getTime();
                    Double startSecond = Math.ceil((start - origin) / 1000);
                    if(!startedRecords.containsKey(startSecond)) {
                        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! If you can read me, it means there is a bug :-( !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    } else {
                        startedRecords.put(startSecond, startedRecords.get(startSecond)+1);
                    }


                    final long end = pi.getEndDate().getTime();
                    Double endSecond = Math.ceil((end - origin) / 1000);
                    if(!completedRecords.containsKey(endSecond)) {
                        completedRecords.put(endSecond, 1L);
                    } else {
                        completedRecords.put(endSecond, completedRecords.get(endSecond)+1);
                    }
                });


                final List<Long> cumulatedStarted = new ArrayList();
                cumulatedStarted.add(0L);
                startedRecords.forEach((second, count) -> {
                    cumulatedStarted.add(cumulatedStarted.get(cumulatedStarted.size()-1) + count);
                });

                final List<Long> cumulatedCompleted = new ArrayList();
                cumulatedCompleted.add(0L);
                completedRecords.forEach((second, count) -> {
                    cumulatedCompleted.add(cumulatedCompleted.get(cumulatedCompleted.size()-1) + count);
                });

                System.err.println("Started: ");
                System.err.println(cumulatedStarted.size());

                System.err.println("Completed: ");
                System.err.println(cumulatedCompleted.size());

                return Optional.empty();

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

        private LinkedHashMap<Double, Long> createTimeSpan(List<ArchivedProcessInstance> executedProcessInstances, long origin) {

            double maxEndDate = getMaxSecond(executedProcessInstances, origin);
            LinkedHashMap<Double, Long> result = new LinkedHashMap();
            for (Double i = 0d; i <= maxEndDate; i++) {
                result.put(i, 0L);
            }
            return result;
        }

        private double getMaxSecond(List<ArchivedProcessInstance> executedProcessInstances, long origin) {

            Optional<ArchivedProcessInstance> lastToComplete = executedProcessInstances.stream().max(Comparator.comparing(ArchivedProcessInstance::getEndDate));

            double nbSeconds = Math.ceil((lastToComplete.get().getEndDate().getTime() - origin) / 1000);
            System.err.println("Bench lasted: " + nbSeconds + " seconds");
            return nbSeconds;
        }

    }


    public static void main(String[] args) {
        long startTime = Long.parseLong(args[0]);
        System.err.println("Cases started after: " + new Date(startTime).toGMTString());
        DescriptiveStatistics statistics = new DataStore().collect(startTime).get();
        System.err.println(statistics);
    }
}
