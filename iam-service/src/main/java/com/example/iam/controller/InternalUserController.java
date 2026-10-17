package com.example.iam.controller.internal;

import com.example.iam.dto.ApiResponse;
import com.example.iam.dto.UserProfileDto;
import com.example.iam.entity.User;
import com.example.iam.repository.UserRepository;
import com.example.iam.dto.NotificationContactResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, UserProfileDto>>> getUsersBatch(@RequestBody List<String> userIds) {
        
        List<UUID> uuids = userIds.stream()
                .map(UUID::fromString)
                .toList();

        List<User> users = userRepository.findAllById(uuids);

        Map<String, UserProfileDto> profiles = users.stream()
                .collect(Collectors.toMap(
                        user -> user.getId().toString(),
                        user -> {
                            String rawEmail = user.getEmail();
                            String safeEmailPrefix = (rawEmail != null && rawEmail.contains("@")) 
                                    ? rawEmail.substring(0, rawEmail.indexOf('@')) 
                                    : "Anonymous";

                            String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
                            String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
                            String fullName = (firstName + " " + lastName).trim();

                            String avatarUrl = user.getAvatarUrl ();

                            return new UserProfileDto(
                                    user.getId().toString(),
                                    fullName, 
                                    avatarUrl
                            );
                        }
                ));

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User profiles retrieved successfully", profiles));
    }

    
    @GetMapping("/{userId}/notification-contact")
    public ResponseEntity<ApiResponse<NotificationContactResponse>> getNotificationContact(@PathVariable String userId) {
        UUID id;

        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException (HttpStatus.BAD_REQUEST, "Invalid user ID");
        }

        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        NotificationContactResponse contact = new NotificationContactResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                Boolean.TRUE.equals(user.getEmailVerified())
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Notification contact retrieved", contact));
    }
}
