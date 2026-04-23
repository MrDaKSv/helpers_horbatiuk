package edu.horbatiuk.telegrambot.repository;

import edu.horbatiuk.telegrambot.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

}