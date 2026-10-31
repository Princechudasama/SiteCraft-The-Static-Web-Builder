package com.sitecraft.backend.repository;

import com.sitecraft.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<ChatMessage,Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId );
    List<ChatMessage> findByConversationIdOrderByCreatedAtDescIdDesc( UUID conversationId, Pageable pageable );

}
