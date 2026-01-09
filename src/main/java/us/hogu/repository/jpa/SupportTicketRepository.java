package us.hogu.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.SupportTicket;
import us.hogu.model.enums.SupportTicketStatus;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

}
