/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.category;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class CategoryRepository.
 *
 */
public class CategoryRepository {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(CategoryRepository.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Count filtered.
     *
     * @param name        the name
     * @param ageDivision the age division
     * @param active      active category
     * @return the int
     */
    public static int countFiltered(String name, AgeDivision ageDivision, AgeGroup ageGroup, Gender gender, Integer age,
            Double bodyWeight, Boolean active) {
        return JPAService.runInTransaction(em -> {
            return doCountFiltered(name, gender, ageDivision, ageGroup, age, bodyWeight, active, em);
        });
    }

    /**
     * Delete.
     *
     * @param Category the category
     */
    public static void delete(Category Category) {
        JPAService.runInTransaction(em -> {
            em.remove(getById(Category.getId(), em));
            return null;
        });
    }

    public static Integer doCountFiltered(String name, Gender gender, AgeDivision ageDivision, AgeGroup ageGroup,
            Integer age, Double bodyWeight, Boolean active, EntityManager em) {
        String selection = filteringSelection(name, gender, ageDivision, ageGroup, age, bodyWeight, active);
        String qlString = "select count(c.id) from Category c " + selection;
        logger.trace("count = {}", qlString);
        Query query = em.createQuery(qlString);
        setFilteringParameters(name, gender, ageDivision, ageGroup, age, bodyWeight, true, query);
        int i = ((Long) query.getSingleResult()).intValue();
        return i;
    }

    @SuppressWarnings("unchecked")
    public static Category doFindByName(String string, EntityManager em) {
        Query query = em.createQuery("select c from Category c where lower(name) = lower(:string) order by c.name");
        query.setParameter("string", string);
        return (Category) query.getResultList().stream().findFirst().orElse(null);
    }

    public static List<Category> doFindFiltered(EntityManager em, String name, Gender gender, AgeDivision ageDivision,
            AgeGroup ageGroup, Integer age, Double bodyWeight, Boolean active, int offset, int limit) {
        String qlString = "select c from Category c"
                + filteringSelection(name, gender, ageDivision, ageGroup, age, bodyWeight, active)
                + " order by c.ageGroup.ageDivision, c.ageGroup.minAge, c.ageGroup.maxAge, c.maximumWeight";
        logger.trace("query = {}", qlString);
        
        Query query = em.createQuery(qlString);
        setFilteringParameters(name, gender, ageDivision, ageGroup, age, bodyWeight, active, query);
        if (offset >= 0) {
            query.setFirstResult(offset);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        @SuppressWarnings("unchecked")
        List<Category> resultList = query.getResultList();
        return resultList;
    }

    private static String filteringJoins(AgeGroup ag, Integer age) {
        List<String> fromList = new LinkedList<>();
        if (ag != null || age != null) {
            fromList.add("join c.ageGroup ag"); // group is via a relationship, join on id
        }
        if (fromList.size() == 0) {
            return "";
        } else {
            return String.join(" ", fromList);
        }
    }

    private static String filteringSelection(String name, Gender gender, AgeDivision ageDivision, AgeGroup ageGroup,
            Integer age, Double bodyWeight, Boolean active) {
        String joins = filteringJoins(ageGroup, age);
        String where = filteringWhere(name, ageDivision, ageGroup, age, bodyWeight, gender, active);
        String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
        return selection;
    }

    private static String filteringWhere(String name, AgeDivision ageDivision, AgeGroup ageGroup, Integer age,
            Double bodyWeight, Gender gender, Boolean active) {
        List<String> whereList = new LinkedList<>();
        if (ageDivision != null) {
            whereList.add("c.ageGroup.ageDivision = :division");
        }
        if (name != null && name.trim().length() > 0) {
            whereList.add("lower(c.name) like :name");
        }
        if (active != null && active) {
            whereList.add("c.active = true");
        }
        if (gender != null) {
            whereList.add("c.gender = :gender");
        }
        // because there is exactly one ageGroup following could be done with
        // c.ageGroup.id = :ageGroupId
        if (ageGroup != null) {
            whereList.add("ag.id = :ageGroupId"); // group is via a relationship, select the joined id.
        }
        // because there is exactly one ageGroup following could test on
        // c.ageGroup.minAge and maxAge
        if (age != null) {
            whereList.add("(ag.minAge <= :age) and (ag.maxAge >= :age)");
        }
        if (bodyWeight != null) {
            whereList.add("(c.minimumWeight < :bodyWeight) and (c.maximumWeight >= :bodyWeight)");
        }
        if (whereList.size() == 0) {
            return null;
        } else {
            return String.join(" and ", whereList);
        }
    }

    /**
     * @return active categories
     */
    public static List<Category> findActive() {
        return JPAService.runInTransaction(em -> {
            return doFindFiltered(em, null, null, null, null, null, null, true, -1, -1);
        });
    }

    /**
     * @return active categories for gender
     */
    public static List<Category> findActive(Gender gender) {
        return JPAService.runInTransaction(em -> {
            return doFindFiltered(em, null, gender, null, null, null, null, true, -1, -1);
        });
    }

    public static Collection<Category> findActive(Gender gender, Double bodyWeight) {
        List<Category> list = findActive(gender);
        if (bodyWeight == null) {
            return list;
        } else {
            return list.stream()
                    .filter(cat -> bodyWeight > cat.getMinimumWeight() && bodyWeight <= cat.getMaximumWeight())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<Category> findAll() {
        return JPAService
                .runInTransaction(em -> em.createQuery("select c from Category c order by c.name").getResultList());
    }

    /**
     * Find by name.
     *
     * @param string the string
     * @return the category
     */
    public static Category findByName(String string) {
        return JPAService.runInTransaction(em -> {
            return doFindByName(string, em);
        });
    }

    /**
     * Find filtered.
     *
     * @param name        the last name
     * @param ageDivision the age division
     * @param active      if category is active
     * @param offset      the offset
     * @param limit       the limit
     * @return the list
     */
    public static List<Category> findFiltered(String name, Gender gender, AgeDivision ageDivision, AgeGroup ageGroup,
            Integer age, Double bodyWeight, Boolean active, int offset, int limit) {
        return JPAService.runInTransaction(em -> {
            List<Category> doFindFiltered = doFindFiltered(em, name, gender, ageDivision, ageGroup, age, bodyWeight, active, offset, limit);
            logger.warn("found {} searching for {} {} {} {} {}", doFindFiltered.size(), gender, ageDivision, age, bodyWeight, active);
            return doFindFiltered;
        });
    }

    public static List<Category> findByGenderDivisionAgeBW(Gender gender, AgeDivision ageDivision, Integer age,
            Double bodyWeight) {
        
        List<Category> findFiltered = findFiltered((String)null, gender, ageDivision, (AgeGroup)null, age, bodyWeight, true, -1, -1);
//        gender = null;
//        ageDivision = null;
//        age = (Integer) null;
//        bodyWeight = (Double) null;
//        Boolean active = null;
//        List<Category> findFiltered = findFiltered((String)null, gender, ageDivision, (AgeGroup)null, age, bodyWeight, active, -1, -1);
        return findFiltered;
    }

    /**
     * Gets the by id.
     *
     * @param id the id
     * @param em the em
     * @return the by id
     */
    @SuppressWarnings("unchecked")
    public static Category getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from Category u where u.id=:id");
        query.setParameter("id", id);

        return (Category) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Insert kids categories.
     *
     * @param curAG  the cur AG
     * @param active the active
     */
    static void insertKidsCategories(EntityManager em, AgeGroup ag, Gender gender) {
        boolean active = true;
        if (gender == Gender.F) {
            em.persist(new Category(0.0, 35.0, Gender.F, active, 0, ag));
            em.persist(new Category(35.0, 40.0, Gender.F, active, 0, ag));
            em.persist(new Category(40.0, 45.0, Gender.F, active, 0, ag));
            em.persist(new Category(45.0, 49.0, Gender.F, active, 0, ag));
            em.persist(new Category(49.0, 55.0, Gender.F, active, 0, ag));
            em.persist(new Category(55.0, 59.0, Gender.F, active, 0, ag));
            em.persist(new Category(59.0, 64.0, Gender.F, active, 0, ag));
            em.persist(new Category(64.0, 71.0, Gender.F, active, 0, ag));
            em.persist(new Category(71.0, 76.0, Gender.F, active, 0, ag));
            em.persist(new Category(76.0, 999.0, Gender.F, active, 0, ag));
        } else {
            em.persist(new Category(0.0, 44.0, Gender.M, active, 0, ag));
            em.persist(new Category(44.0, 49.0, Gender.M, active, 0, ag));
            em.persist(new Category(49.0, 55.0, Gender.M, active, 0, ag));
            em.persist(new Category(55.0, 61.0, Gender.M, active, 0, ag));
            em.persist(new Category(61.0, 67.0, Gender.M, active, 0, ag));
            em.persist(new Category(67.0, 73.0, Gender.M, active, 0, ag));
            em.persist(new Category(73.0, 81.0, Gender.M, active, 0, ag));
            em.persist(new Category(81.0, 89.0, Gender.M, active, 0, ag));
            em.persist(new Category(89.0, 96.0, Gender.M, active, 0, ag));
            em.persist(new Category(96.0, 999.0, Gender.M, active, 0, ag));
        }
    }

    private static void insertNewCategories(EntityManager em, AgeGroup ag, Gender gender) {
        boolean active = true;
        if (gender == Gender.F) {
            em.persist(new Category(45.0, 49.0, Gender.F, active, 203, ag));
            em.persist(new Category(49.0, 55.0, Gender.F, active, 221, ag));
            em.persist(new Category(55.0, 59.0, Gender.F, active, 232, ag));
            em.persist(new Category(59.0, 64.0, Gender.F, active, 245, ag));
            em.persist(new Category(64.0, 71.0, Gender.F, active, 261, ag));
            em.persist(new Category(71.0, 76.0, Gender.F, active, 272, ag));
            em.persist(new Category(76.0, 81.0, Gender.F, active, 283, ag));
            em.persist(new Category(81.0, 87.0, Gender.F, active, 294, ag));
            em.persist(new Category(87.0, 999.0, Gender.F, active, 320, ag));
        } else {
            em.persist(new Category(0.0, 55.0, Gender.M, active, 293, ag));
            em.persist(new Category(55.0, 61.0, Gender.M, active, 312, ag));
            em.persist(new Category(61.0, 67.0, Gender.M, active, 331, ag));
            em.persist(new Category(67.0, 73.0, Gender.M, active, 348, ag));
            em.persist(new Category(73.0, 81.0, Gender.M, active, 368, ag));
            em.persist(new Category(81.0, 89.0, Gender.M, active, 387, ag));
            em.persist(new Category(89.0, 96.0, Gender.M, active, 401, ag));
            em.persist(new Category(96.0, 102.0, Gender.M, active, 412, ag));
            em.persist(new Category(102.0, 109.0, Gender.M, active, 424, ag));
            em.persist(new Category(109.0, 999.0, Gender.M, active, 453, ag));
        }
    }

    /**
     * Insert standard categories.
     *
     * @param em
     */
    public static void insertStandardCategories(EntityManager em) {
        if (findAll().size() == 0) {
            AgeGroup msr = new AgeGroup("SR", true, 15, 99, Gender.M, AgeDivision.DEFAULT);
            insertNewCategories(em, msr, Gender.M);
            em.persist(msr);

            AgeGroup mjr = new AgeGroup("JR", true, 15, 20, Gender.M, AgeDivision.DEFAULT);
            insertNewCategories(em, mjr, Gender.M);
            em.persist(mjr);

            AgeGroup myth = new AgeGroup("YTH", true, 13, 17, Gender.M, AgeDivision.DEFAULT);
            insertYouthCategories(em, myth, Gender.M);
            em.persist(myth);
        }
    }

    /**
     * Insert youth categories.
     *
     * @param em
     * @param curAG the cur AG
     */
    static void insertYouthCategories(EntityManager em, AgeGroup ag, Gender gender) {
        boolean active = false;
        if (gender == Gender.F) {
            em.persist(new Category(0.0, 40.0, Gender.F, active, 0, ag));
            em.persist(new Category(40.0, 45.0, Gender.F, active, 0, ag));
            em.persist(new Category(45.0, 49.0, Gender.F, active, 0, ag));
            em.persist(new Category(49.0, 55.0, Gender.F, active, 0, ag));
            em.persist(new Category(55.0, 59.0, Gender.F, active, 0, ag));
            em.persist(new Category(59.0, 64.0, Gender.F, active, 0, ag));
            em.persist(new Category(64.0, 71.0, Gender.F, active, 0, ag));
            em.persist(new Category(71.0, 76.0, Gender.F, active, 0, ag));
            em.persist(new Category(76.0, 81.0, Gender.F, active, 0, ag));
            em.persist(new Category(81.0, 999.0, Gender.F, active, 0, ag));
        } else {
            em.persist(new Category(0.0, 49.0, Gender.M, active, 0, ag));
            em.persist(new Category(49.0, 55.0, Gender.M, active, 0, ag));
            em.persist(new Category(55.0, 61.0, Gender.M, active, 0, ag));
            em.persist(new Category(61.0, 67.0, Gender.M, active, 0, ag));
            em.persist(new Category(67.0, 73.0, Gender.M, active, 0, ag));
            em.persist(new Category(73.0, 81.0, Gender.M, active, 0, ag));
            em.persist(new Category(81.0, 89.0, Gender.M, active, 0, ag));
            em.persist(new Category(89.0, 96.0, Gender.M, active, 0, ag));
            em.persist(new Category(96.0, 102.0, Gender.M, active, 0, ag));
            em.persist(new Category(102.0, 999.0, Gender.M, active, 0, ag));
        }
    }

    /**
     * Save.
     *
     * @param Category the category
     * @return the category
     */
    public static Category save(Category Category) {
        return JPAService.runInTransaction(em -> em.merge(Category));
    }

    private static void setFilteringParameters(String name, Gender gender, AgeDivision ageDivision, AgeGroup ageGroup,
            Integer age, Double bodyWeight, Boolean active, Query query) {
        // note -- active is not used, it is hard-coded in the where query.
        if (name != null && name.trim().length() > 0) {
            // starts with
            query.setParameter("name", "%" + name.toLowerCase() + "%");
        }
        if (ageGroup != null) {
            // group is via a relationship, we join and select on id
            query.setParameter("ageGroupId", ageGroup.getId());
        }
        if (age != null) {
            query.setParameter("age", age);
        }
        if (bodyWeight != null) {
            query.setParameter("bodyWeight", bodyWeight);
        }
        if (ageDivision != null) {
            query.setParameter("division", ageDivision); // ageDivision is a string
        }
        if (gender != null) {
            query.setParameter("gender", gender);
        }
    }

}
