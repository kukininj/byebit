package com.example.byebit.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupedTransaction {
    public String date;
    public List<TransactionWithWallet> transactions;

    public GroupedTransaction(String date, List<TransactionWithWallet> transactions) {
        this.date = date;
        this.transactions = transactions;
    }

    public static List<GroupedTransaction> groupTransactionsByDate(List<TransactionWithWallet> allTransactions) {
        Map<String, List<TransactionWithWallet>> grouped = allTransactions.stream()
                .collect(Collectors.groupingBy(transactionWithWallet -> {
                    Instant instant = transactionWithWallet.transaction.getTimestamp();
                    return DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            .withZone(ZoneOffset.UTC)
                            .format(instant);
                }));

        return grouped.entrySet().stream()
                .map(entry -> new GroupedTransaction(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(gt -> Instant.parse(gt.date + "T00:00:00Z"), Comparator.reverseOrder())) // Sort by date descending
                .collect(Collectors.toList());
    }
}
