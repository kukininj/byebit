package com.example.byebit.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupedTransaction {

    private final String dateLabel;
    private final List<TransactionHandle> transactions;

    public GroupedTransaction(String dateLabel, List<TransactionHandle> transactions) {
        this.dateLabel = dateLabel;
        this.transactions = transactions;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public List<TransactionHandle> getTransactions() {
        return transactions;
    }

    public static List<GroupedTransaction> groupTransactionsByDate(List<TransactionHandle> allTransactions) {
        Map<LocalDate, List<TransactionHandle>> groupedMap = new LinkedHashMap<>();

        allTransactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

        for (TransactionHandle tx : allTransactions) {
            Instant timestamp = tx.getTimestamp();
            LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();

            groupedMap.computeIfAbsent(date, k -> new ArrayList<>()).add(tx);
        }

        List<GroupedTransaction> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        for (Map.Entry<LocalDate, List<TransactionHandle>> entry : groupedMap.entrySet()) {
            String formattedDate = entry.getKey().format(formatter);
            result.add(new GroupedTransaction(formattedDate, entry.getValue()));
        }

        return result;
    }
}
