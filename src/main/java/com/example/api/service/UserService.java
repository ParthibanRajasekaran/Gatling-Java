package com.example.api.service;

import com.example.api.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final Map<Long, User> users = new ConcurrentHashMap<>();

    public UserService() {
        // Initialize with some sample data
        initializeSampleData();
    }

    private void initializeSampleData() {
        users.put(1L, new User(1L, "John Doe", "john.doe@example.com", 30, "active"));
        users.put(2L, new User(2L, "Jane Smith", "jane.smith@example.com", 25, "active"));
        users.put(3L, new User(3L, "Bob Johnson", "bob.johnson@example.com", 35, "inactive"));
        users.put(4L, new User(4L, "Alice Brown", "alice.brown@example.com", 28, "active"));
        users.put(5L, new User(5L, "Charlie Wilson", "charlie.wilson@example.com", 42, "active"));
        users.put(6L, new User(6L, "Diana Prince", "diana.prince@example.com", 29, "active"));
        users.put(7L, new User(7L, "Edward Norton", "edward.norton@example.com", 38, "inactive"));
        users.put(8L, new User(8L, "Fiona Davis", "fiona.davis@example.com", 33, "active"));
        users.put(9L, new User(9L, "George Miller", "george.miller@example.com", 45, "active"));
        users.put(10L, new User(10L, "Helen Carter", "helen.carter@example.com", 27, "active"));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> getUsersByStatus(String status) {
        return users.values().stream()
                .filter(user -> status.equalsIgnoreCase(user.getStatus()))
                .toList();
    }

    public User createUser(User user) {
        Long nextId = users.keySet().stream().max(Long::compareTo).orElse(0L) + 1;
        user.setId(nextId);
        users.put(nextId, user);
        return user;
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        if (users.containsKey(id)) {
            updatedUser.setId(id);
            users.put(id, updatedUser);
            return Optional.of(updatedUser);
        }
        return Optional.empty();
    }

    public boolean deleteUser(Long id) {
        return users.remove(id) != null;
    }

    public long getUserCount() {
        return users.size();
    }

    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("activeUsers", getUsersByStatus("active").size());
        stats.put("inactiveUsers", getUsersByStatus("inactive").size());
        stats.put("averageAge", users.values().stream()
                .mapToInt(User::getAge)
                .average()
                .orElse(0.0));
        return stats;
    }
}
