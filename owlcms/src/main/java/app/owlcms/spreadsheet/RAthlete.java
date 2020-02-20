/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Used for registration. Converts from String to data types as required to simplify Excel/CSV imports
 *
 * @author Jean-François Lamy
 *
 */
public class RAthlete {

    static Pattern legacyPattern = Pattern.compile("([mMfF]?)(>?)(\\d+)");
    final Logger logger = (Logger) LoggerFactory.getLogger(RAthlete.class);

    {
        logger.setLevel(Level.INFO);
    }

    Athlete a = new Athlete();

    public Athlete getAthlete() {
        return a;
    }

    /**
     * @param bodyWeight
     */
    public void setBodyWeight(Double bodyWeight) {
        a.setBodyWeight(bodyWeight);
    }

    /**
     * @param category
     * @throws Exception
     * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
     */
    public void setCategory(String categoryName) throws Exception {
        if (categoryName == null) {
            return;
        }
        categoryName = categoryName.trim();
        if (categoryName.isEmpty()) {
            return;
        }

        Matcher legacyResult = legacyPattern.matcher(categoryName);
        double searchBodyWeight;
        if (!legacyResult.matches()) {
            // try by explicit name
            Category category = RCompetition.getActiveCategories().get(categoryName);
            if (category == null) {
                throw new Exception("Category " + categoryName + " is not defined.");
            }
            if (category.getGender() != a.getGender()) {
                throw new Exception(
                        "Gender for category " + categoryName + " does not match athlete gender " + a.getGender());
            }
            a.setCategory(category);
            return;
        } else {
            fixLegacyGender(legacyResult);
            if (!legacyResult.group(2).isEmpty()) {
                searchBodyWeight = 998.0D;
            } else {
                searchBodyWeight = Integer.parseInt(legacyResult.group(3)) - 0.1D;
            }
        }

        int age;
        // if no birth date, try with 0 and see if we get the default group.
        if (a.getFullBirthDate() == null) {
            age = 0;
        } else {
            age = LocalDate.now().getYear() - a.getYearOfBirth();
        }

        List<Category> found = CategoryRepository.findByGenderAgeBW(a.getGender(), age, searchBodyWeight);
        Category category = found.size() > 0 ? found.get(0) : null;
        if (category == null) {
            throw new Exception("Category cannot be found using age=" + age + ", gender=" + a.getGender() + ", weight="
                    + legacyResult.group(2) + legacyResult.group(3));
        }
        a.setCategory(category);
    }

    /**
     * @param cleanJerk1Declaration
     */
    public void setCleanJerk1Declaration(String cleanJerk1Declaration) {
        a.setCleanJerk1Declaration(cleanJerk1Declaration);
    }

    /**
     * @param firstName
     * @see app.owlcms.data.athlete.Athlete#setFirstName(java.lang.String)
     */
    public void setFirstName(String firstName) {
        a.setFirstName(firstName);
    }

//	/**
//	 * @param category
//	 * @throws Exception
//	 * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
//	 */
//	public void setFullBirthDate(Date fullBirthDate) throws Exception {
//		if (fullBirthDate == null) return;
//		LocalDate fbd = fullBirthDate.toInstant()
//			      .atZone(ZoneId.systemDefault())
//			      .toLocalDate();
//		a.setFullBirthDate(fbd);
//	}

    /**
     * Note the mapping file must process the birth date before the category, as it is a required input to determine the
     * category.
     *
     * @param category
     * @throws Exception
     * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
     */
    public void setFullBirthDate(String content) throws Exception {
        try {
            long l = Long.parseLong(content);
            if (l < 3000) {
                a.setYearOfBirth((int) l);
                logger.trace("short " + l);
            } else {
                LocalDate epoch = LocalDate.of(1900, 1, 1);
                LocalDate plusDays = epoch.plusDays(l - 2); // Excel quirks: 1 is 1900-01-01 and 1900-02-29 did not
                                                            // exist.
                logger.trace("long " + plusDays);
                a.setFullBirthDate(plusDays);
            }
            return;
        } catch (NumberFormatException e) {
            try {
                // try as a ISO Date
                LocalDate parse = LocalDate.parse(content, DateTimeFormatter.ISO_LOCAL_DATE);
                logger.trace("dateString {}", parse);

            } catch (DateTimeParseException e1) {
                throw new Exception(content + " is not a 4-digit year or an international yyyy-MM-dd format date like "
                        + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.of(1961, 02, 28)));
            }
        }
    }

    /**
     * @param lastName
     * @see app.owlcms.data.athlete.Athlete#setLastName(java.lang.String)
     */
    public void setGender(String gender) {
        logger.trace("setting gender {} for athlete {}", gender, a.getLastName());
        if (gender == null) {
            return;
        }
        a.setGender(Gender.valueOf(gender.toUpperCase()));
    }

    /**
     * @param group
     * @throws Exception
     * @see app.owlcms.data.athlete.Athlete#setGroup(app.owlcms.data.category.Group)
     */
    public void setGroup(String groupName) throws Exception {
        if (groupName == null) {
            return;
        }
        Group group = GroupRepository.findByName(groupName);
        if (group == null) {
            throw new Exception("Group " + groupName + " is not defined.");
        }
        a.setGroup(group);
    }

    /**
     * @param lastName
     * @see app.owlcms.data.athlete.Athlete#setLastName(java.lang.String)
     */
    public void setLastName(String lastName) {
        a.setLastName(lastName);
    }

    /**
     * @param lotNumber
     * @see app.owlcms.data.athlete.Athlete#setLotNumber(java.lang.Integer)
     */
    public void setLotNumber(String lotNumber) {
        if (lotNumber == null) {
            return;
        }
        a.setLotNumber(Integer.parseInt(lotNumber));
    }

    /**
     * @param membership
     * @see app.owlcms.data.athlete.Athlete#setMembership(java.lang.String)
     */
    public void setMembership(String membership) {
        a.setMembership(membership);
    }

    /**
     * @param qualifyingTotal
     * @see app.owlcms.data.athlete.Athlete#setQualifyingTotal(java.lang.Integer)
     */
    public void setQualifyingTotal(Integer qualifyingTotal) {
        a.setQualifyingTotal(qualifyingTotal);
    }

    /**
     * @param snatch1Declaration
     */
    public void setSnatch1Declaration(String snatch1Declaration) {
        a.setSnatch1Declaration(snatch1Declaration);
    }

    /**
     * @param club
     * @see app.owlcms.data.athlete.Athlete#setTeam(java.lang.String)
     */
    public void setTeam(String club) {
        a.setTeam(club);
    }

    private void fixLegacyGender(Matcher result) throws Exception {
        String genderLetter = result.group(1);
        if (a.getGender() == null) {
            if (genderLetter.equalsIgnoreCase("f")) {
                a.setGender(Gender.F);
            } else if (genderLetter.equalsIgnoreCase("m")) {
                a.setGender(Gender.M);
            }
        } else if (!genderLetter.isEmpty()) {
            // letter present, should match gender
            if ((genderLetter.equalsIgnoreCase("f") && a.getGender() != Gender.F)
                    || (genderLetter.equalsIgnoreCase("m") && a.getGender() != Gender.M)) {
                throw new Exception("Inconsistency between gender and category.");
            }
        } else {
            // nothing to do gender is known and consistent.
        }
    }

}
