package us.hogu.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import us.hogu.model.ServiceLocale;

@Repository
public interface ServiceLocaleJpa extends JpaRepository<ServiceLocale, Long> {
}

