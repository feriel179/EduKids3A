package com.edukids.edukids3a.interfaces;

import javafx.collections.ObservableList;

/**
 * Contrat CRUD générique (aligné sur le projet EduKids cours / {@code tn.esprit.interfaces.GlobalInterface}).
 */
public interface GlobalInterface<T> {

    ObservableList<T> getAll();

    T add(T entity);

    void update(T entity);

    void delete(T entity);
}
