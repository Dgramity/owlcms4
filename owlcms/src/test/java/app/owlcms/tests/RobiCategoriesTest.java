/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.RobiCategories;
import app.owlcms.init.OwlcmsSession;

public class RobiCategoriesTest {

    @Test
    public void testInside() {
        Athlete a = new Athlete();
        a.setBodyWeight(57.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M61", cat.getCode());
    }

    @Test
    public void testOutside() {
        Athlete a = new Athlete();
        a.setBodyWeight(50.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M55", cat.getCode());
    }

    @Test
    public void testYoung() {
        Athlete a = new Athlete();
        a.setBodyWeight(48.2D);
        a.setGender(Gender.M);
        a.setYearOfBirth(LocalDate.now().getYear() - 17);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M49", cat.getCode());
    }
    
    @Before
    public void setupTest() {
        OwlcmsSession.withFop(fop -> fop.beforeTest());
    }
}
