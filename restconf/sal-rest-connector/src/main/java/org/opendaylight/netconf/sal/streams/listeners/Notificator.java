/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.streams.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Notificator} is responsible to create, remove and find
 * {@link ListenerAdapter} listener.
 */
public class Notificator {

    private static Map<String, ListenerAdapter> listenersByStreamName = new ConcurrentHashMap<>();
    private static Map<String, List<NotificationListenerAdapter>> notificationListenersByStreamName = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(Notificator.class);
    private static final Lock lock = new ReentrantLock();

    private Notificator() {
    }

    /**
     * Returns list of all stream names
     */
    public static Set<String> getStreamNames() {
        return listenersByStreamName.keySet();
    }

    /**
     * Gets {@link ListenerAdapter} specified by stream name.
     *
     * @param streamName
     *            The name of the stream.
     * @return {@link ListenerAdapter} specified by stream name.
     */
    public static ListenerAdapter getListenerFor(final String streamName) {
        return listenersByStreamName.get(streamName);
    }

    /**
     * Checks if the listener specified by {@link YangInstanceIdentifier} path exist.
     *
     * @param streamName
     * @return True if the listener exist, false otherwise.
     */
    public static boolean existListenerFor(final String streamName) {
        return listenersByStreamName.containsKey(streamName);
    }

    /**
     * Creates new {@link ListenerAdapter} listener from {@link YangInstanceIdentifier} path and stream name.
     *
     * @param path
     *            Path to data in data repository.
     * @param streamName
     *            The name of the stream.
     * @return New {@link ListenerAdapter} listener from {@link YangInstanceIdentifier} path and stream name.
     */
    public static ListenerAdapter createListener(final YangInstanceIdentifier path, final String streamName) {
        final ListenerAdapter listener = new ListenerAdapter(path, streamName);
        try {
            lock.lock();
            listenersByStreamName.put(streamName, listener);
        } finally {
            lock.unlock();
        }
        return listener;
    }

    /**
     * Looks for listener determined by {@link YangInstanceIdentifier} path and removes it.
     * Creates String representation of stream name from URI. Removes slash from URI in start and end position.
     *
     * @param uri
     *            URI for creation stream name.
     * @return String representation of stream name.
     */
    public static String createStreamNameFromUri(final String uri) {
        if (uri == null) {
            return null;
        }
        String result = uri;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length()-1);
        }
        return result;
    }

    /**
     * Removes all listeners.
     */
    public static void removeAllListeners() {
        for (final ListenerAdapter listener : listenersByStreamName.values()) {
            try {
                listener.close();
            } catch (final Exception e) {
                LOG.error("Failed to close listener", e);
            }
        }
        try {
            lock.lock();
            listenersByStreamName = new ConcurrentHashMap<>();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if listener has at least one subscriber. In case it doesn't have any, delete listener.
     *
     * @param listener
     *            ListenerAdapter
     */
    public static void removeListenerIfNoSubscriberExists(final ListenerAdapter listener) {
        if (!listener.hasSubscribers()) {
            deleteListener(listener);
        }
    }

    /**
     * Delete {@link ListenerAdapter} listener specified in parameter.
     *
     * @param listener
     *            ListenerAdapter
     */
    private static void deleteListener(final ListenerAdapter listener) {
        if (listener != null) {
            try {
                listener.close();
            } catch (final Exception e) {
                LOG.error("Failed to close listener", e);
            }
            try {
                lock.lock();
                listenersByStreamName.remove(listener.getStreamName());
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Check if the listener specified by qnames of request exist.
     *
     * @param streamName
     *            - name of stream
     * @return True if the listener exist, false otherwise.
     */
    public static boolean existNotificationListenerFor(final String streamName) {
        return notificationListenersByStreamName.containsKey(streamName);
    }

    public static List<NotificationListenerAdapter> createNotificationListener(final List<SchemaPath> paths,
            final String streamName, final String outputType) {
        final List<NotificationListenerAdapter> listListeners = new ArrayList<>();
        for (final SchemaPath path : paths) {
            final NotificationListenerAdapter listener = new NotificationListenerAdapter(path, streamName, outputType);
            listListeners.add(listener);
        }
        try {
            lock.lock();
            notificationListenersByStreamName.put(streamName, listListeners);
        } finally {
            lock.unlock();
        }
        return listListeners;
    }

    public static void removeNotificationListenerIfNoSubscriberExists(final NotificationListenerAdapter listener) {
        if (!listener.hasSubscribers()) {
            deleteNotificationListener(listener);
        }
    }

    private static void deleteNotificationListener(final NotificationListenerAdapter listener) {
        if (listener != null) {
            try {
                listener.close();
            } catch (final Exception e) {
                LOG.error("Failed to close listener", e);
            }
            try {
                lock.lock();
                notificationListenersByStreamName.remove(listener.getStreamName());
            } finally {
                lock.unlock();
            }
        }
    }

    public static List<NotificationListenerAdapter> getNotificationListenerFor(final String streamName) {
        return notificationListenersByStreamName.get(streamName);
    }
}
