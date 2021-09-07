/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;

public class AgeGroupDefinitionReader {

    /**
     * Create category templates that will be copied to instantiate the actual categories. The world records are read
     * and included in the template.
     *
     * @param workbook
     * @return
     */
    public static Map<String, Category> createCategoryTemplates(Workbook workbook) {
        Map<String, Category> categoryMap = new HashMap<>();
        DataFormatter dataFormatter = new DataFormatter();
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        int iRow = 0;
        while (rowIterator.hasNext()) {
            int iColumn = 0;
            Row row;
            if (iRow == 0) {
                // process header
                row = rowIterator.next();
            }
            row = rowIterator.next();

            Category c = new Category();

            Iterator<Cell> cellIterator = row.cellIterator();

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                switch (iColumn) {
                case 0: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    c.setCode(cellValue.trim());
                    categoryMap.put(cellValue, c);
                }
                    break;
                case 1: {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        c.setGender(cellValue.contentEquals("F") ? Gender.F : Gender.M);
                    }
                }
                    break;
                case 2: {
                    c.setMaximumWeight(cell.getNumericCellValue());
                }
                    break;
                case 3: {
                    c.setWrSr((int) Math.round(cell.getNumericCellValue()));
                }
                    break;
                case 4: {
                    c.setWrJr((int) Math.round(cell.getNumericCellValue()));
                }
                    break;
                case 5: {
                    c.setWrYth((int) Math.round(cell.getNumericCellValue()));
                }
                    break;
                }
                iColumn++;
            }
            iRow++;

        }
        return categoryMap;
    }

    static void createAgeGroups(Workbook workbook, Map<String, Category> templates,
            EnumSet<AgeDivision> ageDivisionOverride,
            String localizedName) {

        JPAService.runInTransaction(em -> {
            Sheet sheet = workbook.getSheetAt(1);
            Iterator<Row> rowIterator = sheet.rowIterator();
            int iRow = 0;
            while (rowIterator.hasNext()) {
                int iColumn = 0;
                Row row;
                if (iRow == 0) {
                    // process header
                    row = rowIterator.next();
                }
                row = rowIterator.next();

                AgeGroup ag = new AgeGroup();
                double curMin = 0.0D;

                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (iColumn) {
                    case 0: {
                        String cellValue = cell.getStringCellValue();
                        String trim = cellValue.trim();
                        ag.setCode(trim);
                    }
                        break;
                    case 1:
                        break;
                    case 2: {
                        String cellValue = cell.getStringCellValue();
                        ag.setAgeDivision(AgeDivision.getAgeDivisionFromCode(cellValue));
                    }
                        break;
                    case 3: {
                        String cellValue = cell.getStringCellValue();
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            ag.setGender(cellValue.contentEquals("F") ? Gender.F : Gender.M);
                        }
                    }
                        break;
                    case 4: {
                        long cellValue = Math.round(cell.getNumericCellValue());
                        ag.setMinAge(Math.toIntExact(cellValue));
                    }
                        break;
                    case 5: {
                        long cellValue = Math.round(cell.getNumericCellValue());
                        ag.setMaxAge(Math.toIntExact(cellValue));
                    }
                        break;
                    case 6: {
                        boolean explicitlyActive = cell.getBooleanCellValue();
                        // age division is active according to spreadsheet, unless we are given an explicit
                        // list of age divisions as override (e.g. to setup tests or demos)
                        boolean active = ageDivisionOverride == null ? explicitlyActive
                                : ageDivisionOverride.stream()
                                        .anyMatch((Predicate<AgeDivision>) (ad) -> ad.equals(ag.getAgeDivision()));
                        ag.setActive(active);
                    }
                        break;
                    default: {
                        String cellValue = cell.getStringCellValue();
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            Category cat = AgeGroupRepository.createCategoryFromTemplate(cellValue, ag, templates,
                                    curMin);
                            if (cat != null) {
                                em.persist(cat);
                                AgeGroupRepository.logger.trace(cat.longDump());
                                curMin = cat.getMaximumWeight();
                            }
                        }
                    }
                        break;
                    }
                    iColumn++;
                }
                em.persist(ag);
                iRow++;
            }
            Competition comp = Competition.getCurrent();
            Competition comp2 = em.contains(comp) ? comp : em.merge(comp);
            comp2.setAgeGroupsFileName(localizedName);

            return null;
        });
    }

    static void doInsertAgeGroup(EnumSet<AgeDivision> es, String localizedName) {
        //FIXME use getFileOrResource()
        //InputStream localizedResourceAsStream = AgeGroupRepository.class.getResourceAsStream(localizedName);
        InputStream localizedResourceAsStream = Config.getResourceAsStream(localizedName);
        try (Workbook workbook = WorkbookFactory
                .create(localizedResourceAsStream)) {
            AgeGroupRepository.logger.info("loading configuration file {}", localizedName);
            Map<String, Category> templates = createCategoryTemplates(workbook);
            createAgeGroups(workbook, templates, es, localizedName);
            workbook.close();
        } catch (Exception e) {
            AgeGroupRepository.logger.error("could not process ageGroup configuration\n{}", LoggerUtils.stackTrace(e));
        }
    }

}
