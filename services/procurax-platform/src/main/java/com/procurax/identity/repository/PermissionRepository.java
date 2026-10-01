package com.procurax.identity.repository;

import com.procurax.identity.domain.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    @Query(value = """
            SELECT p.name FROM permissions p
            JOIN role_permissions rp ON rp.permission_id = p.id
            WHERE rp.role_id = :roleId
            """, nativeQuery = true)
    List<String> findPermissionNamesByRoleId(@Param("roleId") UUID roleId);
}
