package com.bonitasoft.process;

import com.bonitasoft.engine.api.APIClient;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

import java.time.temporal.ChronoUnit;
import java.util.*;

public class CreatedVsCompleted {



    public HashMap<Object, Object> fetchData(APIClient apiClient, long startTime, long endTime) throws SearchException {

        SearchResult<ArchivedProcessInstance> executedProcessInstances = getArchivedProcessInstancesCompletedAfter(apiClient, startTime, endTime);

        long origin = executedProcessInstances.getResult().get(0).getStartDate().getTime();


        List<Double> timeSpan = createTimeSpan(executedProcessInstances.getResult(), origin);

        LinkedHashMap<Double, Long> startedRecords = buildEmptyRecordsMap(timeSpan);
        LinkedHashMap<Double, Long> completedRecords = buildEmptyRecordsMap(timeSpan);

        DescriptiveStatistics stats = new DescriptiveStatistics();

        executedProcessInstances.getResult().stream().forEachOrdered(pi -> {
            countProcessInstances(origin, startedRecords, pi.getStartDate());
            countProcessInstances(origin, completedRecords, pi.getEndDate());
            computeDurationStats(stats, pi.getStartDate(), pi.getEndDate());
        });


        final List<Long> cumulatedStarted = cumulatedCountsBySecond(startedRecords);
        final List<Long> cumulatedCompleted = cumulatedCountsBySecond(completedRecords);


        HashMap<Object, Object> result = new HashMap<>();
        result.put("data", Arrays.asList(cumulatedStarted, cumulatedCompleted));
        result.put("labels", timeSpan);
        if(!Double.isNaN(stats.getSkewness()) && !Double.isNaN(stats.getKurtosis())) {
            result.put("stats", stats);
        }

        return result;

    }

    private List<Long> cumulatedCountsBySecond(LinkedHashMap<Double, Long> records) {
        final List<Long> cumulatedCount = new ArrayList();
        cumulatedCount.add(0L);
        records.forEach((second, count) -> {
            cumulatedCount.add(cumulatedCount.get(cumulatedCount.size() - 1) + count);
        });
        return cumulatedCount;
    }

    private void computeDurationStats(DescriptiveStatistics stats, Date start, Date end) {
        stats.addValue(ChronoUnit.MILLIS.between(start.toInstant(), end.toInstant()));
    }

    private void countProcessInstances(long origin, LinkedHashMap<Double, Long> records, Date date) {
        Double secondFromOrigin = Math.ceil((date.getTime() - origin) / 1000);
        records.put(secondFromOrigin, records.get(secondFromOrigin) + 1);
    }

    private SearchResult<ArchivedProcessInstance> getArchivedProcessInstancesCompletedAfter(APIClient apiClient, long startTime, long endTime) throws SearchException {
        SearchOptions options = new SearchOptionsBuilder(0, Integer.MAX_VALUE).between(ArchivedProcessInstancesSearchDescriptor.START_DATE, startTime, endTime)
                .sort(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, Order.ASC).done();
        return apiClient.getProcessAPI().searchArchivedProcessInstances(options);
    }

    private List<Double> createTimeSpan(List<ArchivedProcessInstance> executedProcessInstances, long origin) {
        List<Double> timeSpanLabels = new ArrayList<>();
        double maxEndDate = getMaxSecond(executedProcessInstances, origin);
        for (Double i = 0d; i <= maxEndDate; i++) {
            timeSpanLabels.add(i);
        }
        return timeSpanLabels;
    }


    private LinkedHashMap<Double, Long> buildEmptyRecordsMap(List<Double> timeSpan) {
        LinkedHashMap<Double, Long> result = new LinkedHashMap(timeSpan.size());
        for (Double i = 0d; i <= timeSpan.size(); i++) {
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
