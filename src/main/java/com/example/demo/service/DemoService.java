package com.example.demo.service;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.auditing.Auditable;
import com.example.demo.domain.History;
import com.example.demo.domain.HistoryRepository;
import com.example.demo.provider.Form;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/* 
* Ideally I would want to further encapsulate the data logic in this class, such as the History entity,
* and make the service layer more agnostic to the particular source of the data it uses.
* I'm a big fan of making beans wired by interfaces and determining the implementation based on properties,
* which keeps from having to refactor this class if we start getting our "history" data from another service/API
* instead of a database directly.
*/

@Service
public class DemoService {

    private final Logger LOGGER = LogManager.getLogger(DemoService.class);

    private HistoryRepository historyRepository;

    public DemoService(final HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Auditable
    public String calculateSubsetsFromForm(final Form form) {

        History history = this.historyRepository.findFirstByTotalAndList(form.getTotal(), form.getList());

        if (history != null) {
            LOGGER.debug("Got result from history");
            return history.getResult();
        }

        // given the time, I would rather find a way for Spring to send these to the back end as a list already
        // and also account for the now unregulated length of this list
        Set<Integer> addendSet = Arrays.asList(
            form.getList()
            .split(",")
        ).stream()
            .map(String::trim)
            .map(Integer::valueOf)
            .collect(Collectors.toSet());

        int sum = Integer.valueOf(form.getTotal());

        RangeAddends addends = new RangeAddends(new LinkedList<>(addendSet), sum);

        Set<Set<Integer>> sumAddends = addends.calculateAddendsForSum();

        String result = this.serializeNestedSet(sumAddends);

        History newHistory = new History();
        newHistory.setTotal(form.getTotal());
        newHistory.setList(form.getList());
        newHistory.setResult(result);

        this.historyRepository.save(newHistory);

        return result;
    }

    private String serializeNestedSet(final Set<Set<Integer>> subsets) {
        StringBuilder builder = new StringBuilder();

        subsets.forEach(
            integers -> {
                builder.append("[");
                integers.forEach(
                    i -> builder.append(i + ",")
                );
                int index = builder.lastIndexOf(",");
                builder.replace(index, index + 1, "] ");
            }
        );

        return builder.toString().trim();
    }
}