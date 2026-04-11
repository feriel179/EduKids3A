package tn.esprit.models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class Student {
    private final long id;
    private String name;
    private String email;
    private final ObservableList<Course> enrolledCourses;

    public Student(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
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
