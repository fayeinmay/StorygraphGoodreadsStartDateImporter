package de.fayedev.sgsdi;

import java.util.Objects;

public class Book {

    private String title;
    private String book_id;
    private String read_instance_id;

    public Book(String read_instance_id, String book_id, String title) {
        this.read_instance_id = read_instance_id;
        this.book_id = book_id;
        this.title = title;
    }

    public Book() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBook_id() {
        return book_id;
    }

    public void setBook_id(String book_id) {
        this.book_id = book_id;
    }

    public String getRead_instance_id() {
        return read_instance_id;
    }

    public void setRead_instance_id(String read_instance_id) {
        this.read_instance_id = read_instance_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(title, book.title) && Objects.equals(book_id, book.book_id) && Objects.equals(read_instance_id, book.read_instance_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, book_id, read_instance_id);
    }
}
