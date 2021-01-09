package io.oreto.brew.web.page;

import io.oreto.brew.data.validation.Validator;

import java.util.List;
import java.util.stream.Collectors;

public interface Notifiable {
    List<Notification> getNotifications();

    default List<Notification> notifications(String group) {
        return getNotifications().stream()
                .filter(it -> it.getGroup().equals(group))
                .collect(Collectors.toList());
    }

    default List<Notification> notifications(Notification.Type type) {
        return getNotifications().stream()
                .filter(it -> it.getType() == type)
                .collect(Collectors.toList());
    }

    default List<Notification> notifications(String group, Notification.Type type) {
        return getNotifications().stream()
                .filter(it -> it.group.equals(group) && it.getType() == type)
                .collect(Collectors.toList());
    }

    default Object notify(String message, Notification.Type type, String group, String...args) {
        Notification notification = Notification.of(message, type, group).withArgs(args);
        getNotifications().add(notification);
        return notification;
    }

    default Object notify(String message, Notification.Type type) {
        Notification notification = Notification.of(message, type);
        getNotifications().add(notification);
        return notification;
    }

    default Object notify(String message, String...args) {
        return getNotifications().add(Notification.of(message).withArgs(args));
    }

    default Object notify(Notification notification) {
        getNotifications().add(notification);
        return notification;
    }

    default Object notify(List<Notification> notifications) {
        getNotifications().addAll(notifications);
        return getNotifications();
    }

    default Object notify(Iterable<Validator.Invalid> validationErrors) {
        List<Notification> notifications = getNotifications();
        for (Validator.Invalid invalid: validationErrors) notifications.add(invalid);
        return notifications;
    }
}
