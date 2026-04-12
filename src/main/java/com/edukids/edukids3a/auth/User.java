package com.edukids.edukids3a.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Utilisateur applicatif (table {@code user}, projet Rami). */
public class User {

    private int id;
    private String email;
    private List<Role> roles;
    private String password;
    private String firstName;
    private String lastName;
    private boolean active;
    private String avatar;
    private boolean verified;

    public User() {
    }

    public User(int id, String email, List<Role> roles, String password,
                String firstName, String lastName, boolean active,
                String avatar, boolean verified) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
        this.avatar = avatar;
        this.verified = verified;
    }

    public String rolesToJson() {
        Gson gson = new Gson();
        List<String> roleStrings = roles.stream().map(Role::getDbValue).toList();
        return gson.toJson(roleStrings);
    }

    public static List<Role> rolesFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of(Role.ROLE_ELEVE);
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() { }.getType();
        List<String> roleStrings = gson.fromJson(json, listType);
        if (roleStrings == null || roleStrings.isEmpty()) {
            return List.of(Role.ROLE_ELEVE);
        }
        List<Role> out = new ArrayList<>();
        for (String s : roleStrings) {
            out.add(Role.fromDbValue(s));
        }
        return out;
    }

    public Role getPrimaryRole() {
        if (roles == null || roles.isEmpty()) {
            return Role.ROLE_ELEVE;
        }
        return roles.get(0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isActive() {
        return active;
    }

    public String getAvatar() {
        return avatar;
    }

    public boolean isVerified() {
        return verified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
