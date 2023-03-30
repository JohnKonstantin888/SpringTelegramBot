package com.example.SpringTelegramBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity(name = "usersDataTable")
public class User {
    @Id
    private Long chatId;

    private Boolean embedJoke;

    private String phoneNumber;

    private Double latitude;

    private Double longitude;

    private String bio;

    private String description;

    private String pinnedMessage;

    private String firstName;

    private String lastName;

    private String userName;

    private Timestamp registeredAt;
}
