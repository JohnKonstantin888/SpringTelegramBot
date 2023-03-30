package com.example.SpringTelegramBot.model.repository;

import com.example.SpringTelegramBot.model.Joke;
import org.springframework.data.repository.CrudRepository;

public interface JokeRepository extends CrudRepository<Joke, Integer> {
}
