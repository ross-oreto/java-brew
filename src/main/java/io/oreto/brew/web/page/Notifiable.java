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

    default Object notify(String name, String message, Notification.Type type, String...args) {
        Notification notification = new Notification(name, message, type).withArgs(args);
        getNotifications().add(notification);
        return notification;
    }

    default Object notify(String name, String message, String...args) {
        return notify(name, message, Notification.Type.info, args);
    }

    default Object notify(List<Notification> notifications) {
        getNotifications().addAll(notifications);
        return getNotifications();
    }
}
