package com.example.emr_server.service;

import com.example.emr_server.entity.Permission;

import java.util.List;

public interface PermissionService {

    /**
     * Pobiera listę uprawnień na podstawie roli.
     *
     * @param role rola (np. "admin", "user")
     * @return lista uprawnień dla danej roli
     */
    List<Permission> getPermissionsByRole(String role);

    /**
     * Pobiera listę uprawnień na podstawie zasobu.
     *
     * @param resource nazwa zasobu (np. "patient", "prescription")
     * @return lista uprawnień dla danego zasobu
     */
    List<Permission> getPermissionsByResource(String resource);

    /**
     * Pobiera listę uprawnień na podstawie akcji.
     *
     * @param action akcja (np. "read", "write", "delete")
     * @return lista uprawnień dla danej akcji
     */
    List<Permission> getPermissionsByAction(String action);

    /**
     * Pobiera listę uprawnień na podstawie roli i zasobu.
     *
     * @param role     rola (np. "admin", "user")
     * @param resource zasób (np. "patient", "prescription")
     * @return lista uprawnień dla danej roli i zasobu
     */
    List<Permission> getPermissionsByRoleAndResource(String role, String resource);

    /**
     * Pobiera listę uprawnień na podstawie roli, zasobu i akcji.
     *
     * @param role     rola (np. "admin", "user")
     * @param resource zasób (np. "patient", "prescription")
     * @param action   akcja (np. "read", "write", "delete")
     * @return lista uprawnień dla danej roli, zasobu i akcji
     */
    List<Permission> getPermissionsByRoleResourceAndAction(String role, String resource, String action);

    /**
     * Zapisuje nowe uprawnienie lub aktualizuje istniejące.
     *
     * @param permission obiekt uprawnienia do zapisania
     * @return zapisany obiekt uprawnienia
     */
    Permission savePermission(Permission permission);

    /**
     * Usuwa uprawnienie na podstawie jego identyfikatora.
     *
     * @param permissionId identyfikator uprawnienia
     */
    void deletePermissionById(Integer permissionId);
}