package com.edukids.entities;

import com.edukids.enums.Role;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

public class User {

    private int id;
    private String email;
    private List<Role> roles;
    private String password;
    private String firstName;
    private String lastName;
    private boolean isActive;
    private String avatar;
    private boolean isVerified;

    public User() {
    }

    public User(String email, String password, String firstName, String lastName, List<Role> roles) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.isActive = true;
        this.isVerified = false;
    }

    public User(int id, String email, List<Role> roles, String password,
                String firstName, String lastName, boolean isActive,
                String avatar, boolean isVerified) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isActive = isActive;
        this.avatar = avatar;
        this.isVerified = isVerified;
    }

    // --- JSON conversion for roles column ---

    public String rolesToJson() {
        Gson gson = new Gson();
        List<String> roleStrings = roles.stream().map(Role::getDbValue).toList();
        return gson.toJson(roleStrings);
    }

    public static List<Role> rolesFromJson(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> roleStrings = gson.fromJson(json, listType);
        return roleStrings.stream().map(Role::fromDbValue).toList();
    }

    public Role getPrimaryRole() {
        if (roles == null || roles.isEmpty()) {
            return Role.ROLE_ELEVE;
        }
        return roles.get(0);
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    // --- Getters & Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<Role> getRoles() { return roles; }
    public void setRoles(List<Role> roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + getPrimaryRole().getDisplayName() +
                ", active=" + isActive +
                ", verified=" + isVerified +
                '}';
    }
}