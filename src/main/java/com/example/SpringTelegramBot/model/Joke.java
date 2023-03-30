package com.example.SpringTelegramBot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Joke {
    @Column(length = 255000)
    private String body;

    private String category;

    @Id
    private Integer id;

    private double rating;

    @Override
    public String toString() {
        return body +
                "\n\ncategory: " + category +
                "\nrating: " + rating;
    }
}
