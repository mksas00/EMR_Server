package controller;

import entity.AuditLog;
import entity.Permission;
import entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuditLogService;
import service.PermissionService;
import service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserAuthorizationController {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Pobierz dane użytkownika i jego logi audytowe.
     */
    @GetMapping("/{userId}/details")
    public ResponseEntity<?> getUserDetails(@PathVariable UUID userId) {
        Optional<User> user = userService.getUserById(userId);

        List<AuditLog> auditLogs = auditLogService.getLogsByUserId(userId.toString().hashCode());

        return ResponseEntity.ok(new Object() {
            public Optional<User> userDetails = user;
            public List<AuditLog> logs = auditLogs;
        });
    }

    /**
     * Pobierz listę uprawnień dla danego użytkownika.
     */
    @GetMapping("/{userId}/permissions")
    public List<Permission> getUserPermissions(@PathVariable UUID userId) {
        return permissionService.getPermissionsByRole("USER_ROLE");
    }

    /**
     * Usuń użytkownika oraz wszystkie powiązane dane.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUserWithLogs(@PathVariable UUID userId) {
        List<AuditLog> logs = auditLogService.getLogsByUserId(userId.toString().hashCode());
        logs.forEach(log -> auditLogService.deleteLogById(log.getId()));

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}