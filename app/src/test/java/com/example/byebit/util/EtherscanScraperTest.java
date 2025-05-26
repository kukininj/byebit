package com.example.byebit.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.schedulers.Schedulers;

public class EtherscanScraperTest {

    @Test
    public void testEtherscanScraper() {
        String url = "https://sepolia.etherscan.io/address/0x69932a4868bc69f6970878d1bef71cf3e8449875"; // The URL to fetch from
        List<EtherscanScraper.TransactionDetail> allTransactions = new ArrayList<>();
        EtherscanScraper scraper = new EtherscanScraper();

        // The Observable chain now directly starts with the URL
        scraper.scrapeTransactions(url)
                // We don't need another subscribeOn for parsing if processDocument handles it
                .observeOn(Schedulers.computation()) // Process results on a computation thread
                .doOnNext(transaction -> allTransactions.add(transaction)) // Collect each transaction as it's emitted
                .doOnError(error -> System.err.println("Scraping process error: " + error.getMessage()))
                .doOnComplete(() -> System.out.println("Scraping completed. Total transactions collected: " + allTransactions.size()))
                .blockingSubscribe(); // Block the main thread until the Observable completes

        // Print all collected transactions
        System.out.println("\n--- All Collected Transactions ---");
        allTransactions.forEach(System.out::println);

        // Optional: Print as a simple table for better readability
        System.out.printf("\n%10s | %15s | %8s | %20s | %15s | %42s | %7s | %42s | %15s | %15s%n",
                "Hash", "Method", "Block", "DateTime (UTC)", "Age", "From", "Dir", "To", "Value", "Fee");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        allTransactions.forEach(tx -> System.out.printf("%10.10s | %15.15s | %8s | %20s | %15s | %42s | %7s | %42s | %15s | %15s%n",
                tx.getTxnHash() != null ? tx.getTxnHash().substring(0, Math.min(tx.getTxnHash().length(), 10)) : "N/A",
                tx.getMethod() != null ? tx.getMethod().substring(0, Math.min(tx.getMethod().length(), 15)) : "N/A",
                tx.getBlock(), tx.getDateTimeUTC(), tx.getAgeDisplay(),
                tx.getFromAddress() != null ? tx.getFromAddress().substring(0, Math.min(tx.getFromAddress().length(), 42)) : "N/A",
                tx.getDirection(),
                tx.getToAddress() != null ? tx.getToAddress().substring(0, Math.min(tx.getToAddress().length(), 42)) : (tx.getToType().equals("Contract Creation") ? "Contract Creation" : "N/A"), // Use .equals for String comparison
                tx.getValueETH(), tx.getTxnFeeETH()));
    }
}
