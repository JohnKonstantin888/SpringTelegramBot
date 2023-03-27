package com.example.SpringTelegramBot.model.repository;

import com.example.SpringTelegramBot.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
