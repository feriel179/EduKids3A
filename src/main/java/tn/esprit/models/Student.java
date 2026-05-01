package tn.esprit.models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class Student {
    private final long id;
    private String name;
    private String email;
    private int age;
    private String preferredCategory;
    private final ObservableList<Course> enrolledCourses;

    public Student(long id, String name, String email) {
        this(id, name, email, 0, "General");
    }

    public Student(long id, String name, String email, int age, String preferredCategory) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = Math.max(0, age);
        this.preferredCategory = preferredCategory == null || preferredCategory.isBlank() ? "General" : preferredCategory.trim();
        this.enrolledCourses = FXCollections.observableArrayList();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }

    public String getPreferredCategory() {
        return preferredCategory;
    }

    public void setPreferredCategory(String preferredCategory) {
        this.preferredCategory = preferredCategory == null || preferredCategory.isBlank()
                ? "General"
                : preferredCategory.trim();
    }

    public String getAgeLabel() {
        return age <= 0 ? "Not set" : age + " years old";
    }

    public String getAgeGroupLabel() {
        if (age >= 8 && age <= 10) {
            return "Ages 8-10";
        }
        if (age >= 11 && age <= 13) {
            return "Ages 11-13";
        }
        if (age >= 14) {
            return "Ages 14+";
        }
        return age <= 0 ? "Profile pending" : "Age " + age;
    }

    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void replaceEnrolledCourses(List<Course> courses) {
        enrolledCourses.setAll(courses);
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
