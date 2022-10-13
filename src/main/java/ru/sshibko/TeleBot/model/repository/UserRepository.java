package ru.sshibko.TeleBot.model.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.sshibko.TeleBot.model.entity.User;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
}
