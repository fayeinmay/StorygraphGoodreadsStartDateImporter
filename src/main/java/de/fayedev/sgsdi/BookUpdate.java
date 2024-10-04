package de.fayedev.sgsdi;

public record BookUpdate(String title, String author, String start_day, String start_month, String start_year,
                         String day, String month, String year, String read_instance_id, String book_id) {
}
