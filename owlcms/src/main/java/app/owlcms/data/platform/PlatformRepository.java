/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.platform;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/**
 * PlatformRepository.
 *
 */
public class PlatformRepository {

    /**
     * Delete.
     *
     * @param Platform the platform
     */
    /**
     * @param Platform
     */
    public static void delete(Platform platform) {
        JPAService.runInTransaction(em -> {
            // this is the only case where platform needs to know its groups, so we do a
            // query instead of adding a relationship.
            Long pId = platform.getId();
            // group is illegal as a table name; query uses the configured table name for entity.
            Query gQ = em.createQuery("select g from CompetitionGroup g join g.platform p where p.id = :platformId");
            gQ.setParameter("platformId", pId);
            @SuppressWarnings("unchecked")
            List<Group> gL = gQ.getResultList();
            for (Group g : gL) {
                g.setPlatform(null);
            }
            em.remove(em.contains(platform) ? platform : em.merge(platform));
            return null;
        });
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<Platform> findAll() {
        return JPAService.runInTransaction(em -> em.createQuery("select c from Platform c").getResultList());
    }

    /**
     * Find by name.
     *
     * @param string the string
     * @return the platform
     */
    @SuppressWarnings("unchecked")
    public static Platform findByName(String string) {
        return JPAService.runInTransaction(em -> {
            Query query = em.createQuery("select c from Platform c where lower(name) = lower(:string)");
            query.setParameter("string", string);
            List<Platform> resultList = query.getResultList();
            return resultList.get(0);
        });
    }

    /**
     * Gets the by id.
     *
     * @param id the id
     * @param em the em
     * @return the by id
     */
    @SuppressWarnings("unchecked")
    public static Platform getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from Platform u where u.id=:id");
        query.setParameter("id", id);

        return (Platform) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Save. The 1:1 relationship with FOP is managed manually since FOP is not persisted.
     *
     * @param platform the platform
     * @return the platform
     */
    public static Platform save(Platform platform) {
        Platform nPlatform = JPAService.runInTransaction(em -> em.merge(platform));
        String name = nPlatform.getName();
        if (name != null) {
            FieldOfPlay fop = OwlcmsFactory.getFOPByName(name);
            if (fop != null) {
                fop.setPlatform(nPlatform);
            }
        }
        return nPlatform;
    }
}
