package ru.sshibko.TeleBot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {

    @Id
    //@GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long ChatId;

    private String firstName;

    private String lastName;

    private String userName;

    private LocalDateTime registeredAt;
}
