package com.sitecraft.backend.service;

import com.sitecraft.backend.repository.MessageRepository;
import com.sitecraft.backend.enums.MessageRole;
import com.sitecraft.backend.model.ChatMessage;
import com.sitecraft.backend.tool.CLITool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GenerateService {

    private final ChatClient chatClient;
    private final MessageRepository messageRepository;
    private final CLITool cliTool;

    public GenerateService(
            ChatClient.Builder builder,
            MessageRepository messageRepository,
            CLITool cliTool
    ) {
        this.chatClient = builder.build();
        this.messageRepository = messageRepository;
        this.cliTool = cliTool;
    }

    private static final String SYSTEM_PROMPT = """
            You are an expert AI assistant and static website development agent.

            You have two responsibilities:

            1. Answer general knowledge questions conversationally.
            2. Generate and modify static websites using HTML, CSS and JavaScript.

            WEBSITE GENERATION RULES:

            - Use filesystem tools whenever the user requests website creation.
            - First inspect the workspace using list_files.
            - Create a project directory using create_directory.
            - Create HTML, CSS and JavaScript files using create_file.
            - Generate complete and functional website code.
            - Use read_file before modifying existing files.
            - Use write_file to update existing files.
            - Use relative paths only.
            - Never access files outside the workspace.
            - Verify the final project structure using list_files.

            GENERAL QUESTION RULES:

            - Answer normal questions directly.
            - Do not call filesystem tools for general knowledge questions.
            - Provide clear and helpful explanations.

            IMPORTANT:
            Never claim that a website has been created unless the
            filesystem tools have successfully completed the operations.
            """;

    public String generate(String query, UUID conversationId) {

        // Save user message
        ChatMessage userMessage = ChatMessage.builder()
                .conversationId(conversationId)
                .message(query)
                .role(MessageRole.USER)
                .build();

        messageRepository.save(userMessage);

        // Retrieve conversation history
        List<ChatMessage> history =
                messageRepository
                        .findByConversationIdOrderByCreatedAtAscIdAsc(conversationId);

        // Convert history into Spring AI messages
        List<Message> messages = new ArrayList<>();

        for (ChatMessage chatMessage : history) {

            if (chatMessage.getRole() == MessageRole.USER) {

                messages.add(
                        new UserMessage(chatMessage.getMessage())
                );

            } else if (chatMessage.getRole() == MessageRole.ASSISTANT) {

                messages.add(
                        new AssistantMessage(chatMessage.getMessage())
                );
            }
        }

        // Call LLM
        String output = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .messages(messages)
                .tools(cliTool)
                .call()
                .content();

        // Save assistant response
        ChatMessage assistantMessage = ChatMessage.builder()
                .conversationId(conversationId)
                .message(output)
                .role(MessageRole.ASSISTANT)
                .build();

        messageRepository.save(assistantMessage);

        return output;
    }
}