/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * Contains information regarding each competition category.
 *
 * A category is the combination of an age range (AgeGroup), a gender, and a bodyweight range.
 *
 * Category currently include record information for the computation of Robi points. Category links to its associated
 * records.
 *
 * Robi = * A x (total)^b where b = log(10)/log(2)
 *
 * A = 1000 / [ (WR)^b ] WR = World RecordEvent
 *
 * @author owlcms
 *
 */
@SuppressWarnings("serial")

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
public class Category implements Serializable, Comparable<Category>, Cloneable {

    @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(Category.class);

    public final static Double ROBI_B = 3.321928095;

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    /** The minimum weight. */
    Double minimumWeight; // inclusive

    /** The maximum weight. */
    Double maximumWeight; // exclusive

    /** minimum weight to be considered eligible */
    @Column(columnDefinition = "integer default 0")
    private int qualifyingTotal = 0;

    @ManyToOne(fetch = FetchType.LAZY) // ok in this case
    @JoinColumn(name = "agegroup_id")
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(columnDefinition = "boolean default false")
    private boolean active;

    private Integer wrSr;

    private Integer wrJr;

    private Integer wrYth;

    // combines age group and bw category (which includes gender).
    private String code;

    // we need EAGER due to the way the CRUD updater works.
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Participation> participations = new ArrayList<>();

    @SuppressWarnings("unused")
    private String name;

    /**
     * Instantiates a new category.
     */
    public Category() {
    }

    public Category(Category c) {
        this(c.id, c.minimumWeight, c.maximumWeight, c.gender, c.active, c.getWrYth(), c.getWrJr(), c.getWrSr(),
                c.ageGroup, c.qualifyingTotal);
    }

    public Category(Long id, Double minimumWeight, Double maximumWeight, Gender gender, boolean active, Integer wrYth,
            Integer wrJr, Integer wrSr, AgeGroup ageGroup, Integer qualifyingTotal) {
        this.setId(id);
        this.setMinimumWeight(minimumWeight);
        this.setMaximumWeight(maximumWeight);
        this.setGender(gender);
        this.setActive(active);
        this.setAgeGroup(ageGroup);
        this.setWrYth(wrYth);
        this.setWrJr(wrJr);
        this.setWrSr(wrSr);
        this.setQualifyingTotal(qualifyingTotal);
        setCategoryName(minimumWeight, maximumWeight, gender, ageGroup);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Category o) {
        if (o == null) {
            return -1; // we are smaller than null -- null goes to the end;
        }

        int compare = ObjectUtils.compare(this.gender, o.getGender());
        if (compare != 0) {
            return compare;
        }

        compare = ObjectUtils.compare(this.ageGroup, o.getAgeGroup(), true);
        if (compare != 0) {
            return compare;
        }

        // same division, same gender, rank according to maximumWeight.
        Double value1 = this.getMaximumWeight();
        Double value2 = o.getMaximumWeight();
        compare = ObjectUtils.compare(value1, value2);
        return compare;
    }

    public String dump() {
        return "Category [code=" + code + ", name=" + getName() + ", minimumWeight=" + minimumWeight
                + ", maximumWeight="
                + maximumWeight + ", ageGroup=" + ageGroup + ", gender=" + gender + ", active=" + active + ", wrSr="
                + getWrSr() + ", wrJr=" + getWrJr() + ", wrYth=" + getWrYth() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass()) {
            //logger.warn("equals quick fail {} {} {} from {}", o, getClass(), o != null ? o.getClass() : "");
            return false;
        }
        Category cat = (Category) o;

        String name2 = getName();
        String name3 = cat.getName();
        boolean equal1 = name2.contentEquals(name3);

        Long id1 = getId();
        Long id2 = cat.getId();
        boolean equal2 = id1 == id2;

//        List<Participation> p1 = getParticipations();
//        List<Participation> p2 = cat.getParticipations();
//        boolean equal3 = ObjectUtils.equals(p1, p2);

        //logger.trace("equals {} {} {} {} {}", this, cat, equal1, equal2, equal3);
        return equal1 && equal2 ;// && equal3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if ((obj == null) || (getClass() != obj.getClass())) {
//            return false;
//        }
//        Category other = (Category) obj;
//
//        // other is not null, and neither are we
//        // don't compare the categories inside age group, this gets circular.
//        boolean ageGroupEquals = AgeGroup.looseEquals(this.ageGroup, other.ageGroup);
//
//        return active == other.active && ageGroupEquals && Objects.equals(code, other.code)
//                && gender == other.gender && Objects.equals(id, other.id)
//                && Objects.equals(maximumWeight, other.maximumWeight)
//                && Objects.equals(minimumWeight, other.minimumWeight) && Objects.equals(name, other.name)
//                && Objects.equals(getWrJr(), other.getWrJr())
//                && Objects.equals(getWrSr(), other.getWrSr()) && Objects.equals(getWrYth(), other.getWrYth());
//    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public void setParticipations(List<Participation> participations) {
        this.participations = participations;
    }

    /**
     * Gets the active.
     *
     * @return the active
     */
    public Boolean getActive() {
        return active;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public String getCode() {
        return code;
    }

    /**
     * Gets the gender.
     *
     * @return the gender
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public String getLimitString() {
        if (maximumWeight > 110) {
            return Translator.translate("catAboveFormat", String.valueOf((int) (Math.round(minimumWeight))));
        } else {
            return String.valueOf((int) (Math.round(maximumWeight)));
        }
    }

    /**
     * Gets the maximum weight.
     *
     * @return the maximumWeight
     */
    public Double getMaximumWeight() {
        return maximumWeight;
    }

    /**
     * Gets the minimum weight.
     *
     * @return the minimumWeight
     */
    public Double getMinimumWeight() {
        return minimumWeight;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        if ((ageGroup == null) || (maximumWeight == null)) {
            return null;
        }
        String agName = ageGroup.getName();
        String catName = getLimitString();
        if (agName == null || agName.isEmpty()) {
            return catName;
        } else {
            return agName + " " + catName;
        }
    }

    /**
     * @return the qualifyingTotal
     */
    public int getQualifyingTotal() {
        return qualifyingTotal;
    }

    /**
     * Gets the wr.
     *
     * @return the wr
     */
    public Integer getWr() {
        if (ageGroup == null) {
            return 0;
        }
        int wr = 0;
        if (ageGroup.getAgeDivision() != AgeDivision.IWF) {
            wr = 0;
        } else if (ageGroup.getMaxAge() == 999) {
            wr = getWrSr();
        } else if (ageGroup.getMaxAge() == 20) {
            wr = getWrJr();
        } else if (ageGroup.getMaxAge() == 17) {
            wr = getWrYth();
        } else {
            wr = 0;
        }
        // logger./**/warn("wr({} {} {} {}) = {}",ageGroup, ageGroup.getAgeDivision(), ageGroup.getMaxAge(),
        // getCode(),wr);
        return wr;
    }

    /**
     * Gets the wr.
     *
     * @return the wr
     */
    public Integer getWr(int age) {
        int wr;
        // logger./**/warn("{} {} {} {} {}", this.getCode(), age, getWrYth(), getWrJr(), getWrSr());
        if (age <= 17) {
            wr = getWrYth();
        } else if (age <= 20) {
            wr = getWrJr();
        } else {
            wr = getWrSr();
        }
        return wr;
    }

    public Integer getWrJr() {
        return wrJr;
    }

    public Integer getWrSr() {
        return wrSr;
    }

    public Integer getWrYth() {
        return wrYth;
    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(active, ageGroup, code, gender, id, maximumWeight, minimumWeight, name, getWrJr(),
//                getWrSr(),
//                getWrYth());
//    }

    /**
     * Checks if is active.
     *
     * @return the boolean
     */
    public Boolean isActive() {
        return active;
    }

    public String longDump() {
        return "Category [name=" + getName() + ", active=" + active + ", id=" + id
                + ", minimumWeight=" + minimumWeight
                + ", maximumWeight=" + maximumWeight + ", ageGroup=" + ageGroup.getName()
                + ", gender="
                + gender + ", wr=" + getWrSr() + ", code=" + code + "]";
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Sets the active.
     *
     * @param active the new active
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setAgeGroup(AgeGroup ageGroup) {
        this.ageGroup = ageGroup;
    }

    public void setCategoryName(Double minimumWeight, Double maximumWeight, Gender enumGender, AgeGroup ageGroup) {
        // does nothing
    }

    public void setCode(String cellValue) {
        this.code = cellValue;
    }

    /**
     * Sets the gender.
     *
     * @param enumGender the new gender
     */
    public void setGender(Gender enumGender) {
        this.gender = enumGender;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Sets the maximum weight.
     *
     * @param maximumWeight the maximumWeight to set
     */
    public void setMaximumWeight(Double maximumWeight) {
        this.maximumWeight = maximumWeight;
    }

//    /**
//     * Sets the name.
//     *
//     * @param name the name to set
//     */
//    public void setName(String name) {
//        this.name = name;
//    }

    /**
     * Sets the minimum weight.
     *
     * @param minimumWeight the minimumWeight to set
     */
    public void setMinimumWeight(Double minimumWeight) {
        this.minimumWeight = minimumWeight;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param qualifyingTotal the qualifyingTotal to set
     */
    public void setQualifyingTotal(int qualifyingTotal) {
        this.qualifyingTotal = qualifyingTotal;
    }

    public void setWrJr(Integer wrJr) {
        this.wrJr = wrJr;
    }

    public void setWrSr(Integer wrSr) {
        // logger./**/warn("wrSr={}",wrSr);
        this.wrSr = wrSr;
    }

    public void setWrYth(Integer wrYth) {
        this.wrYth = wrYth;
    }

    /**
     * Short dump.
     *
     * @return the string
     */
    public String shortDump() {
        return getName() + "_" + active + "_" + gender + "_" + ageGroup;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

}
