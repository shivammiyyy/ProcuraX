package com.procurax.realtime;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RealtimeEventStream {

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Subscriber>> subscribersByOrganization =
            new ConcurrentHashMap<>();
    private final AtomicInteger activeClients = new AtomicInteger();
    private final int maxClients;
    private final int maxClientsPerOrganization;
    private final int queueCapacity;
    private final int heartbeatSeconds;

    public RealtimeEventStream(
            @Value("${procurax.realtime.max-clients:5000}") int maxClients,
            @Value("${procurax.realtime.max-clients-per-organization:500}") int maxClientsPerOrganization,
            @Value("${procurax.realtime.queue-capacity-per-client:128}") int queueCapacity,
            @Value("${procurax.realtime.heartbeat-seconds:15}") int heartbeatSeconds) {
        if (maxClients < 1 || maxClientsPerOrganization < 1 || queueCapacity < 1 || heartbeatSeconds < 1) {
            throw new IllegalArgumentException("Realtime stream limits must be positive");
        }
        this.maxClients = maxClients;
        this.maxClientsPerOrganization = maxClientsPerOrganization;
        this.queueCapacity = queueCapacity;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    public synchronized SseEmitter open(UUID organizationId) {
        ConcurrentMap<UUID, Subscriber> organizationSubscribers =
                subscribersByOrganization.computeIfAbsent(organizationId, ignored -> new ConcurrentHashMap<>());
        if (activeClients.get() >= maxClients || organizationSubscribers.size() >= maxClientsPerOrganization) {
            if (organizationSubscribers.isEmpty()) {
                subscribersByOrganization.remove(organizationId, organizationSubscribers);
            }
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Realtime connection limit reached");
        }

        UUID subscriberId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        Subscriber subscriber = new Subscriber(subscriberId, organizationId, emitter, queueCapacity);
        organizationSubscribers.put(subscriberId, subscriber);
        activeClients.incrementAndGet();
        emitter.onCompletion(() -> remove(subscriber));
        emitter.onTimeout(() -> terminate(subscriber, null));
        emitter.onError(error -> remove(subscriber));
        Thread worker = Thread.ofVirtual().name("procurax-sse-" + subscriberId).unstarted(() -> drain(subscriber));
        subscriber.worker = worker;
        worker.start();
        return emitter;
    }

    public int publish(UUID organizationId, RealtimeEvent event) {
        ConcurrentMap<UUID, Subscriber> subscribers = subscribersByOrganization.get(organizationId);
        if (subscribers == null) {
            return 0;
        }
        int delivered = 0;
        for (Subscriber subscriber : subscribers.values()) {
            if (subscriber.queue.offer(event)) {
                delivered++;
            } else {
                terminate(subscriber, new IllegalStateException("Realtime client exceeded its event buffer"));
            }
        }
        return delivered;
    }

    int activeClients() {
        return activeClients.get();
    }

    @PreDestroy
    void closeAll() {
        subscribersByOrganization.values().forEach(organization ->
                organization.values().forEach(subscriber -> terminate(subscriber, null)));
    }

    private void drain(Subscriber subscriber) {
        try {
            subscriber.emitter.send(SseEmitter.event().name("connected").data("ready"));
            while (!subscriber.closed.get()) {
                RealtimeEvent event = subscriber.queue.poll(heartbeatSeconds, TimeUnit.SECONDS);
                if (event == null) {
                    subscriber.emitter.send(SseEmitter.event().comment("keepalive"));
                } else {
                    subscriber.emitter.send(SseEmitter.event()
                            .id(event.eventId().toString())
                            .name("procurement")
                            .data(event));
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | IllegalStateException exception) {
            remove(subscriber);
        } finally {
            remove(subscriber);
        }
    }

    private void terminate(Subscriber subscriber, Throwable error) {
        if (!subscriber.closed.compareAndSet(false, true)) {
            return;
        }
        removeFromRegistry(subscriber);
        Thread worker = subscriber.worker;
        if (worker != null) {
            worker.interrupt();
        }
        if (error == null) {
            subscriber.emitter.complete();
        } else {
            subscriber.emitter.completeWithError(error);
        }
    }

    private void remove(Subscriber subscriber) {
        if (subscriber.closed.compareAndSet(false, true)) {
            removeFromRegistry(subscriber);
            Thread worker = subscriber.worker;
            if (worker != null && worker != Thread.currentThread()) {
                worker.interrupt();
            }
        }
    }

    private synchronized void removeFromRegistry(Subscriber subscriber) {
        ConcurrentMap<UUID, Subscriber> organizationSubscribers =
                subscribersByOrganization.get(subscriber.organizationId);
        if (organizationSubscribers != null && organizationSubscribers.remove(subscriber.id, subscriber)) {
            activeClients.decrementAndGet();
            if (organizationSubscribers.isEmpty()) {
                subscribersByOrganization.remove(subscriber.organizationId, organizationSubscribers);
            }
        }
    }

    private static final class Subscriber {
        private final UUID id;
        private final UUID organizationId;
        private final SseEmitter emitter;
        private final ArrayBlockingQueue<RealtimeEvent> queue;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile Thread worker;

        private Subscriber(UUID id, UUID organizationId, SseEmitter emitter, int queueCapacity) {
            this.id = id;
            this.organizationId = organizationId;
            this.emitter = emitter;
            this.queue = new ArrayBlockingQueue<>(queueCapacity);
        }
    }
}
