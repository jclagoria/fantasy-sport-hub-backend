package com.fantasysporthub.domain.model;

import org.springframework.data.domain.Persistable;

/**
 * Extension of {@link Persistable} for R2DBC entities with application-generated IDs.
 * <p>
 * R2DBC uses {@link Persistable#isNew()} to determine whether to INSERT or UPDATE.
 * Since {@code @Transient isNew} fields are not loaded from the database,
 * entities loaded via find*() methods retain their default {@code isNew = true},
 * causing save() to attempt INSERT instead of UPDATE.
 * <p>
 * All entities implementing this interface are automatically marked as existing
 * after being loaded from the database via {@link com.fantasysporthub.infrastructure.config.R2dbcConfig}.
 *
 * @param <ID> the type of the entity identifier
 */
public interface ManagedPersistable<ID> extends Persistable<ID> {

    /**
     * Mark this entity as existing (already persisted in the database).
     * Called automatically by the R2DBC AfterConvertCallback.
     */
    void markAsExisting();
}
