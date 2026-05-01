package tn.esprit.interfaces;

import javafx.collections.ObservableList;

public interface GlobalInterface<T> {
    ObservableList<T> getAll();

    T add(T entity);

    void update(T entity);

    void delete(T entity);
}
