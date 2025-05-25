package com.example.byebit.util;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherscanScraper {

    // POJO to hold transaction details (remains unchanged)
    static class TransactionDetail {
        private String txnHash;
        private String method;
        private String block;
        private String dateTimeUTC;
        private String ageDisplay;
        private String fromAddress;
        private String fromDisplay;
        private String direction;
        private String toAddress;
        private String toDisplay;
        private String toType;
        private String valueETH;
        private String txnFeeETH;

        // Constructor
        public TransactionDetail(String txnHash, String method, String block, String dateTimeUTC, String ageDisplay, String fromAddress, String fromDisplay, String direction, String toAddress, String toDisplay, String toType, String valueETH, String txnFeeETH) {
            this.txnHash = txnHash;
            this.method = method;
            this.block = block;
            this.dateTimeUTC = dateTimeUTC;
            this.ageDisplay = ageDisplay;
            this.fromAddress = fromAddress;
            this.fromDisplay = fromDisplay;
            this.direction = direction;
            this.toAddress = toAddress;
            this.toDisplay = toDisplay;
            this.toType = toType;
            this.valueETH = valueETH;
            this.txnFeeETH = txnFeeETH;
        }

        // Getters
        public String getTxnHash() { return txnHash; }
        public String getMethod() { return method; }
        public String getBlock() { return block; }
        public String getDateTimeUTC() { return dateTimeUTC; }
        public String getAgeDisplay() { return ageDisplay; }
        public String getFromAddress() { return fromAddress; }
        public String getFromDisplay() { return fromDisplay; }
        public String getDirection() { return direction; }
        public String getToAddress() { return toAddress; }
        public String getToDisplay() { return toDisplay; }
        public String getToType() { return toType; }
        public String getValueETH() { return valueETH; }
        public String getTxnFeeETH() { return txnFeeETH; }

        @Override
        public String toString() {
            return "TransactionDetail{" +
                    "txnHash='" + txnHash + '\'' +
                    ", method='" + method + '\'' +
                    ", block='" + block + '\'' +
                    ", dateTimeUTC='" + dateTimeUTC + '\'' +
                    ", ageDisplay='" + ageDisplay + '\'' +
                    ", fromAddress='" + fromAddress + '\'' +
                    ", fromDisplay='" + fromDisplay + '\'' +
                    ", direction='" + direction + '\'' +
                    ", toAddress='" + toAddress + '\'' +
                    ", toDisplay='" + toDisplay + '\'' +
                    ", toType='" + toType + '\'' +
                    ", valueETH='" + valueETH + '\'' +
                    ", txnFeeETH='" + txnFeeETH + '\'' +
                    '}';
        }
    }

    /**
     * Helper method to process a Jsoup Document and emit TransactionDetail objects.
     * This separates the parsing logic from the fetching logic.
     *
     * @param doc The Jsoup Document to parse.
     * @return An Observable emitting TransactionDetail objects.
     */
    private Observable<TransactionDetail> processDocument(Document doc) {
        return Observable.create(emitter -> {
            Element transactionsDiv = doc.getElementById("transactions");

            if (transactionsDiv == null) {
                emitter.onError(new RuntimeException("Transactions div with id='transactions' not found."));
                return;
            }

            Element tableResponsiveDiv = transactionsDiv.selectFirst("div.table-responsive");
            if (tableResponsiveDiv == null) {
                emitter.onError(new RuntimeException("Table-responsive div for transactions not found."));
                return;
            }

            Element table = tableResponsiveDiv.selectFirst("table");
            if (table == null) {
                emitter.onError(new RuntimeException("Transactions table not found."));
                return;
            }

            Element tbody = table.selectFirst("tbody");
            if (tbody == null) {
                emitter.onError(new RuntimeException("Table body (tbody) not found in the transactions table."));
                return;
            }

            Elements rows = tbody.select("tr");

            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");

                if (cols.size() < 10) { // Expecting at least 10 columns for a transaction row
                    System.err.println("Skipping row " + (i + 1) + " with less than 10 columns.");
                    continue;
                }

                // Extract data, similar to the Python script
                String txnHash = Optional.ofNullable(cols.get(1).selectFirst("a.myFnExpandBox_searchVal"))
                        .map(Element::text)
                        .orElse(null);

                String method = Optional.ofNullable(cols.get(2).selectFirst("span.badge"))
                        .map(e -> {
                            if (e.hasAttr("data-bs-title")) return e.attr("data-bs-title");
                            if (e.hasAttr("title")) return e.attr("title");
                            return e.text().trim();
                        })
                        .orElse(null);

                String block = Optional.ofNullable(cols.get(3).selectFirst("a"))
                        .map(Element::text)
                        .orElse(null);

                String ageDateTimeUTC = null;
                String ageDisplay = null;

                Element dateTimeSpan = cols.get(4).selectFirst("span.showDate");
                if (dateTimeSpan != null && !dateTimeSpan.attr("style").contains("display:none")) {
                    ageDateTimeUTC = dateTimeSpan.text().trim();
                    ageDisplay = dateTimeSpan.hasAttr("data-bs-title") ? dateTimeSpan.attr("data-bs-title") : ageDateTimeUTC;
                } else {
                    Element ageTooltipSpan = cols.get(4).selectFirst("span[rel=tooltip]");
                    if (ageTooltipSpan != null && ageTooltipSpan.hasAttr("data-bs-title")) {
                        ageDateTimeUTC = ageTooltipSpan.attr("data-bs-title"); // Full UTC date
                        ageDisplay = ageTooltipSpan.text().trim(); // "X days ago"
                    } else {
                        Element ageSpanShowAge = cols.get(4).selectFirst("span.showAge");
                        if (ageSpanShowAge != null) {
                            ageDisplay = ageSpanShowAge.text().trim();
                            ageDateTimeUTC = ageSpanShowAge.hasAttr("data-bs-title") ? ageSpanShowAge.attr("data-bs-title") : ageDisplay;
                        }
                    }
                }

                String fromAddress = null;
                String fromDisplay = null;
                Element fromDiv = cols.get(5).selectFirst("div.d-flex");
                if (fromDiv != null) {
                    Element fromTarget = fromDiv.selectFirst("span[data-highlight-target], a[data-highlight-target]");
                    if (fromTarget != null) {
                        fromAddress = fromTarget.attr("data-highlight-target");
                        fromDisplay = fromTarget.text().trim();
                    }
                }

                String direction = Optional.ofNullable(cols.get(6).selectFirst("span.badge"))
                        .map(Element::text)
                        .orElse(null);

                String toAddress = null;
                String toDisplay = null;
                String toType = "Address";
                Element toDiv = cols.get(7).selectFirst("div.d-flex");
                if (toDiv != null) {
                    if (toDiv.selectFirst("i.far.fa-newspaper") != null) { // Check for the contract creation icon
                        toType = "Contract Creation";
                        toAddress = Optional.ofNullable(toDiv.selectFirst("a[data-highlight-target]"))
                                .map(e -> e.attr("data-highlight-target"))
                                .orElse(null);
                        toDisplay = "Contract Creation"; // The link text is "Contract Creation"
                    } else {
                        Element toTarget = toDiv.selectFirst("span[data-highlight-target], a[data-highlight-target]");
                        if (toTarget != null) {
                            toAddress = toTarget.attr("data-highlight-target");
                            toDisplay = toTarget.text().trim();
                        }
                    }
                }

                String valueETH = Optional.ofNullable(cols.get(8).selectFirst("span.td_showAmount"))
                        .map(Element::text)
                        .orElse(null);

                String txnFeeETH = cols.get(9).text().trim();

                // Emit the extracted transaction detail
                emitter.onNext(new TransactionDetail(
                        txnHash, method, block, ageDateTimeUTC, ageDisplay,
                        fromAddress, fromDisplay, direction,
                        toAddress, toDisplay, toType,
                        valueETH, txnFeeETH
                ));
            }
            emitter.onComplete(); // Signal that all items have been emitted
        });
    }

    /**
     * Fetches HTML content from the given URL and then scrapes transaction details.
     *
     * @param url The URL to fetch HTML from.
     * @return An Observable emitting TransactionDetail objects.
     */
    public Observable<TransactionDetail> scrapeTransactions(String url) {
        return Observable.fromCallable(() -> {
                    System.out.println("Attempting to fetch HTML from: " + url);
                    // Jsoup.connect handles the HTTP request and returns a Document
                    Document doc = Jsoup.connect(url)
                            .timeout(10 * 1000) // 10 seconds timeout for the connection
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36") // Good practice to set User-Agent
                            .get();
                    System.out.println("HTML fetched successfully from: " + url);
                    return doc; // Return the Jsoup Document directly
                })
                .subscribeOn(Schedulers.io()) // Perform the network request on an I/O thread
                .flatMap(this::processDocument); // FlatMap to process the Document and emit TransactionDetail objects
    }

    public static void main(String[] args) {
        String url = "http://localhost:8080/response"; // The URL to fetch from
        List<TransactionDetail> allTransactions = new ArrayList<>();
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
