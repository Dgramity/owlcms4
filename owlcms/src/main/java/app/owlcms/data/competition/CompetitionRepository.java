/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import java.util.List;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * CompetitionRepository.
 *
 */
public class CompetitionRepository {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionRepository.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Delete.
     *
     * @param Competition the competition
     */
    public static void delete(Competition Competition) {
        JPAService.runInTransaction(em -> {
            em.remove(getById(Competition.getId(), em));
            return null;
        });
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<Competition> findAll() {
        return JPAService.runInTransaction(em -> em.createQuery("select c from Competition c").getResultList());
    }

    /**
     * Gets Competition by id.
     *
     * @param id the id
     * @param em the em
     * @return the by id
     */
    @SuppressWarnings("unchecked")
    public static Competition getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from Competition u where u.id=:id");
        query.setParameter("id", id);

        return (Competition) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Save.
     *
     * @param competition the competition
     * @return the competition
     */
    public static Competition save(Competition competition) {
        JPAService.runInTransaction(em -> {
            Competition nc = em.merge(competition);
            TimeZone tz = nc.getTimeZone();
            if (tz != null) {
                TimeZone.setDefault(tz);
            }
            // needed because some classes get competition parameters from getCurrent()
            Competition.setCurrent(nc);
            return nc;
        });

        Competition current = Competition.getCurrent();
        return current;
    }

}
