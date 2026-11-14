package com.example.Reporti.repository;

import com.example.Reporti.model.AgentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentNotificationRepository extends JpaRepository<AgentNotification, Long> {

    List<AgentNotification> findByAgentIdOrderByCreatedAtDesc(UUID agentId);

    Optional<AgentNotification> findByIdAndAgentId(Long id, UUID agentId);

    @Modifying
    @Query("update AgentNotification n set n.read = true where n.agentId = :agentId and (n.read = false or n.read is null)")
    int markAllAsRead(@Param("agentId") UUID agentId);
}
