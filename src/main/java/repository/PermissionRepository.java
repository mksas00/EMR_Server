package repository;

import entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    /**
     * Pobiera listę uprawnień na podstawie roli.
     *
     * @param role rola (np. "admin", "user")
     * @return lista uprawnień dla danej roli
     */
    List<Permission> findByRole(String role);

    /**
     * Pobiera listę uprawnień na podstawie zasobu.
     *
     * @param resource nazwa zasobu (np. "patient", "prescription")
     * @return lista uprawnień dla danego zasobu
     */
    List<Permission> findByResource(String resource);

    /**
     * Pobiera listę uprawnień na podstawie akcji.
     *
     * @param action akcja (np. "read", "write", "delete")
     * @return lista uprawnień dla danej akcji
     */
    List<Permission> findByAction(String action);

    /**
     * Pobiera listę uprawnień na podstawie roli i zasobu.
     *
     * @param role     rola (np. "admin", "user")
     * @param resource zasób (np. "patient", "prescription")
     * @return lista uprawnień dla danej roli i zasobu
     */
    List<Permission> findByRoleAndResource(String role, String resource);

    /**
     * Pobiera listę uprawnień na podstawie roli, zasobu i akcji.
     *
     * @param role     rola (np. "admin", "user")
     * @param resource zasób (np. "patient", "prescription")
     * @param action   akcja (np. "read", "write", "delete")
     * @return lista uprawnień dla danej roli, zasobu i akcji
     */
    List<Permission> findByRoleAndResourceAndAction(String role, String resource, String action);
}