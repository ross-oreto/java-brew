package io.oreto.brew.web.page;

import java.util.List;
import java.util.stream.Collectors;

public interface Notifiable {
    List<Notification> getNotifications();

    default List<Notification> notifications(String name) {
        return getNotifications().stream()
                .filter(it -> it.getName().equals(name))
                .collect(Collectors.toList());
    }

    default List<Notification> notifications(Notification.Type type) {
        return getNotifications().stream()
                .filter(it -> it.getType() == type)
                .collect(Collectors.toList());
    }

    default List<Notification> notifications(String name, Notification.Type type) {
        return getNotifications().stream()
                .filter(it -> it.name.equals(name) && it.getType() == type)
                .collect(Collectors.toList());
    }

    default void notify(String name, String message, Notification.Type type, String...args) {
        getNotifications().add(
                new Notification(name, message, type).withArgs(args)
        );
    }

    default void notify(String name, String message, String...args) {
        notify(name, message, Notification.Type.info, args);
    }

    default void notify(List<Notification> notifications) {
        getNotifications().addAll(notifications);
    }
}
