package com.example.emr_server.service;

import com.example.emr_server.entity.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.emr_server.repository.PermissionRepository;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public List<Permission> getPermissionsByRole(String role) {
        return permissionRepository.findByRole(role);
    }

    @Override
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    @Override
    public List<Permission> getPermissionsByAction(String action) {
        return permissionRepository.findByAction(action);
    }

    @Override
    public List<Permission> getPermissionsByRoleAndResource(String role, String resource) {
        return permissionRepository.findByRoleAndResource(role, resource);
    }

    @Override
    public List<Permission> getPermissionsByRoleResourceAndAction(String role, String resource, String action) {
        return permissionRepository.findByRoleAndResourceAndAction(role, resource, action);
    }

    @Override
    public Permission savePermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Override
    public void deletePermissionById(Integer permissionId) {
        permissionRepository.deleteById(permissionId);
    }
}