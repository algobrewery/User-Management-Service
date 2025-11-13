package com.userapi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.userapi.enums.ResourceType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for building role policies dynamically using ResourceType enum
 */
@Slf4j
public class PolicyBuilder {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Role configuration - can be moved to application.properties or database
    private static final Map<String, RoleConfig> ROLE_CONFIGS = Map.of(
        "ADMIN", new RoleConfig(
            Set.of(ResourceType.values()), // All resources
            Set.of(ResourceType.values()), // All resources
            Set.of(ResourceType.values()), // All resources
            Arrays.asList("*", "view_reports", "create_task", "assign_tasks", "manage_team", "view_analytics", "export_data", "manage_roles", "manage_users")
        ),
        "USER", new RoleConfig(
            Set.of(ResourceType.USERS, ResourceType.TASKS, ResourceType.CLIENTS, ResourceType.ORGANIZATIONS), // Limited read
            Set.of(ResourceType.TASKS), // Only tasks for write
            Set.of(), // No delete permissions
            Arrays.asList("create_task", "view_reports", "update_profile")
        ),
        "MODERATOR", new RoleConfig(
            Set.of(ResourceType.USERS, ResourceType.TASKS, ResourceType.CLIENTS, ResourceType.ORGANIZATIONS, ResourceType.ROLES), // More read access
            Set.of(ResourceType.TASKS, ResourceType.USERS), // Can manage tasks and users
            Set.of(ResourceType.TASKS), // Can delete tasks
            Arrays.asList("create_task", "view_reports", "assign_tasks", "manage_team")
        )
    );
    
    /**
     * Build policy for a specific role type
     */
    public static JsonNode buildPolicyForRole(String roleType) {
        log.debug("Building policy for role type: {}", roleType);
        
        RoleConfig config = ROLE_CONFIGS.get(roleType.toUpperCase());
        if (config == null) {
            log.warn("Unknown role type: {}, using default user policy", roleType);
            config = ROLE_CONFIGS.get("USER");
        }
        
        return buildPolicy(config);
    }
    
    /**
     * Build admin policy with full access to all resources
     */
    public static JsonNode buildAdminPolicy() {
        return buildPolicyForRole("ADMIN");
    }
    
    /**
     * Build user policy with limited access
     */
    public static JsonNode buildUserPolicy() {
        return buildPolicyForRole("USER");
    }
    
    /**
     * Build moderator policy with moderate access
     */
    public static JsonNode buildModeratorPolicy() {
        return buildPolicyForRole("MODERATOR");
    }
    
    /**
     * Build custom policy with specified resources and actions
     */
    public static JsonNode buildCustomPolicy(Set<ResourceType> readResources, 
                                           Set<ResourceType> writeResources, 
                                           Set<ResourceType> deleteResources,
                                           List<String> features) {
        log.debug("Building custom policy with read: {}, write: {}, delete: {}", 
                 readResources, writeResources, deleteResources);
        
        RoleConfig config = new RoleConfig(readResources, writeResources, deleteResources, features);
        return buildPolicy(config);
    }
    
    /**
     * Build policy from RoleConfig
     */
    private static JsonNode buildPolicy(RoleConfig config) {
        ObjectNode policy = objectMapper.createObjectNode();
        policy.put("version", "1.0");
        
        ObjectNode data = objectMapper.createObjectNode();
        ArrayNode readArray = objectMapper.createArrayNode();
        ArrayNode writeArray = objectMapper.createArrayNode();
        ArrayNode deleteArray = objectMapper.createArrayNode();

        config.readResources.stream().map(ResourceType::getResourceName).forEach(readArray::add);
        config.writeResources.stream().map(ResourceType::getResourceName).forEach(writeArray::add);
        config.deleteResources.stream().map(ResourceType::getResourceName).forEach(deleteArray::add);

        /*
        // Add wildcard if all resources are included
        if (config.readResources.contains(ResourceType.ALL) || 
            config.readResources.size() == ResourceType.values().length - 1) { // -1 to exclude ALL
            readArray.add("*");
        } else {
            for (ResourceType resourceType : config.readResources) {
                if (resourceType != ResourceType.ALL) {
                    readArray.add(resourceType.getResourceName());
                }
            }
        }
        
        if (config.writeResources.contains(ResourceType.ALL) || 
            config.writeResources.size() == ResourceType.values().length - 1) {
            writeArray.add("*");
        } else {
            for (ResourceType resourceType : config.writeResources) {
                if (resourceType != ResourceType.ALL) {
                    writeArray.add(resourceType.getResourceName());
                }
            }
        }
        
        if (config.deleteResources.contains(ResourceType.ALL) || 
            config.deleteResources.size() == ResourceType.values().length - 1) {
            deleteArray.add("*");
        } else {
            for (ResourceType resourceType : config.deleteResources) {
                if (resourceType != ResourceType.ALL) {
                    deleteArray.add(resourceType.getResourceName());
                }
            }
        }
        */
        
        data.set("read", readArray);
        data.set("write", writeArray);
        data.set("delete", deleteArray);
        
        // Add features
        ObjectNode featuresNode = objectMapper.createObjectNode();
        ArrayNode executeArray = objectMapper.createArrayNode();
        for (String feature : config.features) {
            executeArray.add(feature);
        }
        featuresNode.set("execute", executeArray);
        
        policy.set("data", data);
        policy.set("features", featuresNode);
        
        return policy;
    }
    
    /**
     * Get all available resource names as a list
     */
    public static List<String> getAllResourceNames() {
        return Arrays.stream(ResourceType.values())
                .filter(rt -> rt != ResourceType.ALL) // Exclude wildcard
                .map(ResourceType::getResourceName)
                .collect(Collectors.toList());
    }
    
    /**
     * Get available role types
     */
    public static Set<String> getAvailableRoleTypes() {
        return ROLE_CONFIGS.keySet();
    }
    
    /**
     * Configuration class for role permissions
     */
    private static class RoleConfig {
        final Set<ResourceType> readResources;
        final Set<ResourceType> writeResources;
        final Set<ResourceType> deleteResources;
        final List<String> features;
        
        RoleConfig(Set<ResourceType> readResources, Set<ResourceType> writeResources, 
                  Set<ResourceType> deleteResources, List<String> features) {
            this.readResources = readResources;
            this.writeResources = writeResources;
            this.deleteResources = deleteResources;
            this.features = features;
        }
    }
}
