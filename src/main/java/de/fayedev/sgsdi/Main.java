package de.fayedev.sgsdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    // TODO: 1. Read generate_files.txt
    // TODO: 2. Fill fields below from browser console example request to change book date. You can also use: https://curlconverter.com/java/
    private static final String AUTHENTICITY_TOKEN = "FILLME";
    private static final String CSRF_TOKEN = "FILLME";
    private static final String COOKIE = "FILLME";

    private static final String[] HEADERS = {"Book Id", "Title", "Author", "Author l-f", "Additional Authors", "ISBN", "ISBN13", "My Rating", "Average Rating", "Publisher", "Binding", "Number of Pages", "Year Published"
            , "Original Publication Year", "Date Read", "Date Added", "Bookshelves", "Bookshelves with positions", "Exclusive Shelf", "My Review", "Spoiler", "Private Notes", "Read Count", "Owned Copies", "read_dates", "genres", "n_ratings"};
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();

    public static void main(String[] args) throws IOException {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Connection");

        Main main = new Main();
        List<BookUpdate> bookUpdates = main.getBookUpdates();

        // TODO: MAKE SURE bookUpdates IS CORRECT BEFORE UNCOMMENTING!
        // TODO: TRY WITH bookUpdates.stream().limit(1).toList() BEFORE
        main.updateBooks(bookUpdates);
    }

    private List<BookUpdate> getBookUpdates() throws IOException {
        List<BookUpdate> bookUpdates = new ArrayList<>();

        // Prepare StoryGraph json data
        Reader inJson = new FileReader("src/main/resources/storygraph.json");
        List<Book> storyGraphBooks = Arrays.stream(objectMapper.readValue(inJson, Book[].class)).toList();

        // Prepare GoodReads CSV Data
        Reader in = new FileReader("src/main/resources/goodreads_library_export.csv");
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .setSkipHeaderRecord(true)
                .build();
        Iterable<CSVRecord> records = csvFormat.parse(in);

        // Parse CSV Data and match json data
        for (CSVRecord goodReadsRecord : records) {
            String title = goodReadsRecord.get("Title");
            String author = goodReadsRecord.get("Author");
            String dates = goodReadsRecord.get("read_dates");

            // Only read books
            if (dates.isBlank()) {
                continue;
            }

            // Prepare dates
            String[] datesSplit = dates.split(",", -1);
            datesSplit = Arrays.stream(datesSplit).toList().stream().filter(c -> !c.isBlank()).toArray(String[]::new);

            if (datesSplit.length == 0) {
                System.out.println("Date splitting failed." + "\n");
                return new ArrayList<>();
            }

            String dateStarted = null;
            String dateFinished;

            if (datesSplit.length == 1) {
                dateFinished = datesSplit[0];
            } else if (datesSplit.length == 2) {
                dateStarted = datesSplit[0];
                dateFinished = datesSplit[1];
            } else {
                System.out.println("Multiread found. Not updating. Goodreads Title: " + title + "\n");
                continue;
            }

            String[] dateFinishedSplit = dateFinished.split("-");

            String startDay = "";
            String startMonth = "";
            String startYear = "";

            if (dateStarted != null) {
                String[] dateStartedSplit = dateStarted.split("-");
                startDay = dateStartedSplit[2];
                startMonth = dateStartedSplit[1];
                startYear = dateStartedSplit[0];
            }

            String finishedDay = dateFinishedSplit[2];
            String finishedMonth = dateFinishedSplit[1];
            String finishedYear = dateFinishedSplit[0];

            // Manipulate titles to match. This still causes some problems with titles containing a : or something on StoryGraph...  but it's good enough
            storyGraphBooks.forEach(c -> c.setTitle(c.getTitle().split("\\(")[0].strip()));
            title = title.split("\\(")[0].strip();
            String finalTitle = title;
            Book book = storyGraphBooks.stream().filter(c -> finalTitle.contains(c.getTitle())).findFirst().orElse(null);

            if (book != null) {
                BookUpdate bookUpdate = new BookUpdate(finalTitle, author, startDay, startMonth, startYear, finishedDay, finishedMonth, finishedYear, book.getRead_instance_id(), book.getBook_id());
                bookUpdates.add(bookUpdate);
            } else {
                System.out.println("Book not found. Not updating. Goodreads Title: " + finalTitle + "\n");
                // continue
            }
        }

        return bookUpdates;
    }

    private void updateBooks(List<BookUpdate> bookUpdates) throws IOException {
        int counter = 0;
        for (BookUpdate bookUpdate : bookUpdates) {
            RequestBody formBody = new FormBody.Builder()
                    .add("_method", "patch")
                    .add("authenticity_token", AUTHENTICITY_TOKEN)
                    .add("read_instance[start_day]", bookUpdate.start_day())
                    .add("read_instance[start_month]", bookUpdate.start_month())
                    .add("read_instance[start_year]", bookUpdate.start_year())
                    .add("read_instance[day]", bookUpdate.day())
                    .add("read_instance[month]", bookUpdate.month())
                    .add("read_instance[year]", bookUpdate.year())
                    .add("book_id", bookUpdate.book_id())
                    .add("read_instance_id", bookUpdate.read_instance_id())
                    .add("commit", "Update")
                    .build();

            Request request = new Request.Builder()
                    .url("https://app.thestorygraph.com/read_instances/" + bookUpdate.read_instance_id())
                    .post(formBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0")
                    .header("Accept", "text/vnd.turbo-stream.html, text/html, application/xhtml+xml")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .header("Referer", "https://app.thestorygraph.com/books/" + bookUpdate.book_id())
                    .header("X-CSRF-Token", CSRF_TOKEN)
                    .header("Origin", "https://app.thestorygraph.com")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Connection", "keep-alive")
                    .header("Cookie", COOKIE)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code \n" + response + "\n on \n" + bookUpdate + "\n\n");
                } else {
                    counter++;
                    System.out.println(
                            "Updated book:" + "\n" +
                                    bookUpdate + "\n"
                    );
                }
            }
        }
        System.out.println("Updated " + counter + " books.");
    }
}