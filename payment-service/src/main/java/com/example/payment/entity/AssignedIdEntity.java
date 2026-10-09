package com.example.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;
import java.util.UUID;

@MappedSuperclass
public abstract class AssignedIdEntity implements Persistable<UUID> {
    @Id @Getter @Setter @Column(columnDefinition = "BINARY(16)")
    private UUID id = UUID.randomUUID();                 // ASSUMPTION: app-generated UUIDs

    @Transient private boolean fresh = true;

    @Override public boolean isNew() { return fresh; }
    @PostLoad @PostPersist void markPersisted() { fresh = false; }
}